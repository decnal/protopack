package com.risenperspectives.pgparsers;

//
// by: D. Lance Robinson
// Risen Perspectives, LLC
// February, 2025
//

import java.util.Hashtable;

import com.risenperspectives.pgparsers.PGPNode.PGPDataType;

public class XmlPGParser implements PGParsersInterface {

	public  PGPOptionFlags flags_ = new PGPOptionFlags();
	private int    debug_;

	private String data_;
	public  int    onLine_;
	public  int    at_;
	private boolean bPastSpaces_;
	public  int    onCharOfLine_;
	public  Hashtable<String,PGPNode> aliases_;

	public String protocolName() { return "XML"; }

	public XmlPGParser() {
	}

	public void setFlags( PGPOptionFlags flags ) {
		flags_ = flags;
	}
	public PGPOptionFlags getFlags() {
		return flags_;
	}

	public void setDebugLevel( int dbgLevel ) {
		debug_ = dbgLevel;
	}

	public String getLastErrorText() {
		return null;
	}

	// get next character()
	// keep track of the line number and offset,
	// and the indent.
	//
	protected char getNext() {
		if ( at_ < data_.length()) {
			char c = data_.charAt(at_++);
			onCharOfLine_++;
			if ( c == '\n' ) {
				onLine_++;
				onCharOfLine_ = 0;
				bPastSpaces_  = false;
			}
			else if ( ! bPastSpaces_  ) {
				if ( c != ' ' )
					bPastSpaces_ = true;
			}
			return c;
		}
		return '\0';	// at end, return 0
	}//getNext();


	protected char lookAhead() {
		return ( at_ < data_.length()) ? data_.charAt(at_) : '\0';
	}


	// pushBack() -- rolls back to the previous character pointer.
	// This should only need to be done once after a hard lookAhead.
	protected void pushBack() {
		--at_;
		--onCharOfLine_;
		if ( --onCharOfLine_ < 0 ) {
			bPastSpaces_ = true;
			--onLine_;
		}
	}

	// bump() -- moves the pointer ahead by n characters
	protected char bump( int n ) {
		char c = '\0';
		while ( n-- > 0 ) {
			c = getNext();
		}
		return c;
	}


	// hexValue()
	// returns decimal value of a hexadecimal character
	//         -1 is returned if not a hexadecimal character
	//
	static public int hexValue( char c ) {
		if ( c >= '0' && c <= '9' )
			return c - '0';
		else if ( c >= 'a' && c <= 'f' )
			return c - 'a' + 10;
		else if ( c >= 'A' && c <= 'F' )
			return c - 'A' + 10;
		else
			return -1;
	}// hexValue


	// skipWhitespaces()
	// Skips over whitespaces, and comments (Everything after a '#' to the eol)
	// returns the first char after spaces
	//         a '\0' is returned if the end of the document has been reached.
	// at_ will at the char returned (not like getNext() which is the next).
	//
	protected char skipWhitespaces() {
		char c = getNext();
		do {
			if ( c != ' ' && c != '\n' && c != '\t' )
			{
				pushBack();
				return c;
			}

			c = getNext();

		} while (true);

	}//skipWhitespaes()


	// getNextNonSpaceChar()
	// Skips over whitespace, and comments (Everything after a '#' to the eol)
	// returns the first char after spaces.
	//         a '\0' is returned if the end of the document has been reached.
	// at_ will be after the character returned.
	//
	protected char getNextNonSpaceChar() {
		while ( at_ < data_.length() ) {
			char c = data_.charAt(at_++);
			onCharOfLine_++;

			// skip past comments.
			if ( c == '#' ) {
				// We have reached the start of a comment.
				// skip to the eol
				c = 0;
				while ( at_ < data_.length() ) {
					c = data_.charAt(at_++);
					onCharOfLine_++;
					if ( c == '\n' )
						break; // we have reached the end of the comment.
					c=0; // in case we have hit the end of the document.
				}
			}// if a comment.

			if ( c != ' ' && c != '\n'  &&  c != '\t' )
				return c;

		}//while
		return '\0';
	}//getNextNonSpaceChar()

	public String decodeXmlValue( String data, int start, int end ) {
		StringBuffer sb = new StringBuffer();
		for (int i=start; i<end; ++i) {
			char c = data.charAt(i);
			if ( c == '&' && i+3 < end ) {
				if ( data.startsWith("quot;",i+1) ) {
					sb.append('"');
					i += 5;
				} else if ( data.startsWith("apos;",i+1) ) {
					sb.append('\'');
					i += 5;
				} else if ( data.startsWith("amp;",i+1) ) {
					sb.append('&');
					i += 4;
				} else if ( data.startsWith("lt;",i+1) ) {
					sb.append('<');
					i += 3;
				} else if ( data.startsWith("gt;",i+1) ) {
					sb.append('>');
					i += 3;
				} else {
					sb.append(c);
				}
			} else if ( c == '%' && i+2 < end )
			{
				int hex1 = hexValue(data.charAt(i+1));
				int hex2 = hexValue(data.charAt(i+2));
				if ( hex1 >= 0 && hex2 >= 0 ) {
					sb.append( (char)(hex1*16 + hex2) );
					i += 2;
				} else {
					sb.append(c);
				}
			} else
			{
				sb.append(c);
			}
		}

		return sb.toString();

	}//decodeXmlValue()


	// parse() includes any optional flags.
	public PGPNode parse( String data, PGPOptionFlags flags ) throws PGPException
	{
		flags_ = flags;
		return parse( data );
	}


	// scanInAttrValue -- at_ should be at char after '='
	private void scanInAttrValue( PGPNode attrNode ) throws PGPException
	{
		int  valueAnchor = at_;
		char c = getNext();
		if ( c == ' ' ) {
			attrNode.setType( PGPDataType.NULL );
			return;
		}
		else if ( c == '\'' || c == '"' ) {
			// A quoted string value
			attrNode.setType( PGPDataType.STRING );
			char quoteChar = c;
			c = getNext();
			while ( c != quoteChar && c != 0 ) {
				c = getNext();
			}
			if ( c == 0 ) {
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "End hit before closing Attribute quote at offset " + valueAnchor );
			}
			if ( at_-1 > valueAnchor+1 )
				attrNode.setValue( decodeXmlValue(data_,valueAnchor+1, at_-1) );
		} else {
			// Non-quoted attribute value. Not typical, but possible.
			attrNode.setType( PGPDataType.UNQUOTED_STRING );
			// scanning in until space or / or >
			while ( c != '/' && c != '>' && c != 0 ) {
				c = getNext();
			}
			pushBack();
			if ( at_ > valueAnchor )
				attrNode.setValue( decodeXmlValue(data_,valueAnchor,at_ ) );
		}

	} //scanInAttrValue()


	private void scanInAttributes( PGPNode node ) throws PGPException
	{
		char c;
		while (true)
		{
			c = getNextNonSpaceChar();
			if ( c == '/' || c == '>' ) {
				pushBack();
				break;
			}
			int nameAnchor = at_-1;
			// search for end of attribute name (look for '=')
			while ( Character.isLetterOrDigit(c) || c=='_' || c==':' || c=='-' || c=='.' ) {
				c = getNext();
			}
			if ( c==0 )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected End of file looking for attribute name at offset " + nameAnchor );
			int nameLen = at_ - nameAnchor;
			if ( nameLen <= 0 )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Invalid Attribute name at offset " + nameAnchor );
			if ( c != '=' )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Equal (=) expected after Attribute name at offset " + nameAnchor );
			PGPNode attrNode = node.addAttr();
			attrNode.setName( data_.substring(nameAnchor,at_-1) );
			scanInAttrValue( attrNode );
		}
	}// scanInAttributes()

	// Sees if the given string is what is next in the data_ stream
	// returns  true only if it matches.
	// If it did match, the next getNext will be the char after the string.
	// If it did not match, the stream is rewound to the starting point
	private boolean isStringNext( String expectedStr ) {
		int len = expectedStr.length();
		if ( at_ + len > data_.length() )
			return false;
		for (int i=0; i<len; ++i) {
			if ( data_.charAt(at_+i) != expectedStr.charAt(i) ) {
				return false;
			}
		}
		bump(len);
		return true;
	}//isStringNext()


	// scanInSpecial() -- handles '<![CDATA[" and "<!--" and "<?" Sequences
	// It should already be known to start with '<!' or '<?'
	private void scanInSpecial( PGPNode node ) throws PGPException
	{
		char c = getNext();	// Should be '<'
		c = getNext();		// Should be '!' or '?
		int valueAnchor = at_;
		if ( c == '!' )
		{
			if ( isStringNext("--") )
			{
				// This is a comment
				valueAnchor = at_;
				while ( (c = getNext()) != 0 ) {
					; //continue looking for the end of the comment
					if ( c == '-' && isStringNext("->") )
						break;
				}
				if ( c == 0 )
					throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected End of file looking for comment end at offset " + valueAnchor );
				// We can ignore the comment
				return;
			} else if ( isStringNext("[CDATA[") )
			{
				// This is a <![CDATA[...]]> section
				valueAnchor = at_;
				while ( (c = getNext()) != 0 ) {
					if ( c == ']' && isStringNext( "]>"))
						break;
					//continue looking for the end of the CDATA section
				}
				if ( c == 0 )
					throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected End of file looking for CDATA ending ']]' at offset " + valueAnchor );
				int length = at_ - valueAnchor - 3;
				String value = null;
				if ( length > 0 )
					value = data_.substring(valueAnchor,valueAnchor+length);
				if ( null == node.getChild() && null == node.getName() ) {
					// We can put the CDATA in the value of this node.
					node.setValue(value,PGPDataType.UNESCAPED_STRING);
				}
				else {
					// Add new node to hold the CDATA value in.
					node.addChild(null,value,PGPDataType.UNESCAPED_STRING);
				}
				return;
			}
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected char after '<!' at offset " + valueAnchor );
	
		} else if ( c == '?' ) {
			// This is a processing instruction
			if ( !isStringNext("<?") )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "'<?' expected at position " + at_ );
			valueAnchor = at_;
			while ( (c = getNext()) != '?' && c != 0 ) {
				if ( c == '?' && lookAhead() == '>' )
					break;
				//continue looking for the end of the processing instruction
			}
			if ( c == 0 )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected End of file looking for processing instruction end at offset " + valueAnchor );
			bump(1);
			int length = at_ - valueAnchor - 2;
			if ( length > 0 ) {
				String instruction = data_.substring(valueAnchor,at_-1);
				//TODO: Do something with the processing instruction
			}
		} else {
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected char after '<' at offset " + valueAnchor );
		}
	}//scanInSpecial()


	// scanInElementValue -- Could be as simple as a value, or a value with sub-elements
	private void scanInElementValue( PGPNode node ) throws PGPException
	{
		char c = getNextNonSpaceChar();
		int count = 0;

		// This is a real value
		while ( c != 0 && (c != '<' || lookAhead() != '/') ) {

			++count;

			if ( c == '<' ) {
				c = lookAhead();
				pushBack();
				if ( c == '!' || c == '[' || c == '?' ) {
					scanInSpecial(node);
				}
				else {
					scanInElement(node);
				}
				c = getNextNonSpaceChar();
				continue;
			}

			// This is a real value
			// Take in chars up to the next '<' char
			int valueAnchor = at_ - 1;
			while ( (c = getNext()) != '<' && c != 0 ) {
				; //continue looking for a sub-element or ending
			}
			int valueLen = at_ - valueAnchor - 1;
			if ( valueLen > 0 ) {
				String value = decodeXmlValue(data_,valueAnchor, at_-1 );
				if ( count == 1 ) {
					// This is the first value, it can go inside the node.
					node.setValue(value,PGPDataType.UNQUOTED_STRING);
				} else {
					// Text is after a child element. Make a node with no name, but with a value.
					node.addChild(null,value,PGPDataType.UNQUOTED_STRING);
				}
			}

		}//while

		pushBack();

	}//scanInElementValue()


	private void scanInElement( PGPNode parentNode ) throws PGPException
	{
		char c = getNextNonSpaceChar();

		if ( c != '<' )
		{
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "'<' expected at position " + at_ );
		}
		int nameAnchor = at_;

		if ( (c = getNext()) == '/' )
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "'<' Unexpected element close at position" + at_ );

		// Read in Element Name
		while ( Character.isLetterOrDigit(c) || c=='_' || c==':' || c=='-' || c=='.' ) {
			c = getNext();//continue search for the end of the name
		}
		if ( c==0 )
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected End of file looking for element name at offset " + nameAnchor );

		int nameLen = at_ - nameAnchor - 1;
		if ( nameLen <= 0 ) {
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Invalid Element name at offset " + nameAnchor );
		}
		String name = data_.substring(nameAnchor,at_-1);

		PGPNode node = parentNode.addChild(name);
		parentNode.setType(PGPDataType.OBJECT);	// Helps XML know when to use {}

		if ( c != '/' && c != '>' )
		{
			// Looks like there are attributes load them
			scanInAttributes(node);
			c = getNext();
		}
		if ( c == '/' ) {
			// No Data for this item
			if ( getNext() != '>' )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "'>' expected to close the element at offset " + at_ );
			node.setType( PGPDataType.NULL );
			return;
		}
		if ( c != '>' )
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "'>' expected to close the element name at offset " + at_ );

		// We have the element name, now gather the element value
		// Or, it could be another set of sub-elements. Get them recursively.
		scanInElementValue( node );

		//Expect closing element for this name

		c = getNextNonSpaceChar();
		int closeAnchor = at_-1;
		if ( c != '<' || (c=getNext()) != '/' || !isStringNext( name) || (c=getNext()) != '>' ) {
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "'</' expected to close the element name at offset " + closeAnchor );
		}

	}//scanInElement

	// parser() -- Scan a XML message and create a PGPNode structure.
	// returns topNode
	// If there was an error, the lastError_ value will have a non-zero value.
	// If
	public PGPNode parse( String data ) throws PGPException {
		if (debug_>0) System.out.println("Enter: parse()" );

		PGPNode topNode = new PGPNode();

		data_         = data;
		at_           = 0;
		onLine_       = 0;
		onCharOfLine_ = 0;
		bPastSpaces_  = false;

		char c;
		while ( (c = getNextNonSpaceChar()) != '\0')
		{
			if ( c != '<' )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "'<' expected at position " + at_ );
			pushBack();
			scanInElement(topNode);
		}

		return topNode;

	}//parse()

}//class XmlPGParsers
