package com.risenperspectives.pgparsers;

//
// by: D. Lance Robinson
// Risen Perspectives, LLC
// November, 2020
//

import java.util.Hashtable;

import com.risenperspectives.pgparsers.PGPNode.PGPDataType;

public class JsonPGParser implements PGParsersInterface {

	public  PGPOptionFlags flags_ = new PGPOptionFlags();
	private int    depthSpaces_;
    private String depthString_;
	private int    debug_;

	private String data_;
	public  int    onLine_;
	public  int    at_;
	private boolean bPastSpaces_;
	public  int    indent_; // spaces before first non-space on line.
	public  int    onCharOfLine_;
	public  Hashtable<String,PGPNode> aliases_;
	public  int[]  indentStack_;
	public  int    indentCount_;

	public String protocolName() { return "JSON"; }

	public JsonPGParser() {
	}

	public void setDepthSpaces( int depthSpaces)
	{
		depthSpaces_ = depthSpaces;
		depthString_ = "            ".substring(0,depthSpaces);
	}
	public int    getDepthSpaces() { return depthSpaces_; }
	public String getDepthString()  { return depthString_; }

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
				indent_       = 0;
			}
			else if ( ! bPastSpaces_  ) {
				if ( c != ' ' )
					bPastSpaces_ = true;
				else
					++indent_;
			}
			return c;
		}
		return '\0';	// at end, return 0
	}//getNext();


	protected char lookAhead() {
		return ( at_ < data_.length()) ? data_.charAt(at_) : '\0';
	}

	// lookAhead(n) can look at the current(n==0) or future chars(n>0) or previous chars(n<0).
	protected char lookAhead( int count) {
		int at = at_ + count - 1;
		return ( (at >= 0) && (at < data_.length())) ? data_.charAt(at) : '\0';
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
			if ( c != ' ' && c != '\n' && c != '\t' ) {
				if ( c == '#' ) {
					// see if this is a comment. Comments start lines with '#',
					// or have " #' pattern (while not in a quote).
					c = lookAhead(-2);
					if ( c == '\0' || c==' ' || c=='\t' ) {
						// We have reached a comment
						// skip to the eol
						c = getNext();
						while ( c != '\n' && c != '\0')
							c = getNext();
						continue; // continue on the next line (if any)
					}
				}
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


	// scanInNumber() -- scans in a number following JSON rules.
	// at_ assumed to be at the first char of the nmber string.
	// throws an exception if this was not a valid number.
	// Returns the string of the number
	// at_  is left at the char after the number
	// Accepts: JSON formats + hex (0xXXXXXX) + octal (0XXXXXXX)
	// The hex and octal values are converted to decimal values.
	protected String scanInNumber() throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInNumber() at="+at_ );

		boolean bNegate = false;
		char c = data_.charAt(at_++);
		int anchor = at_ -1;
		if ( c=='-' ) {
			bNegate = true;
			c = getNext();	// accept the '-' (get the next one)
		}
		if ( !Character.isDigit(c) ) {
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected digit for Number" );
		}
		if ( c == '0' ) {
			// JSON allows no other digits allowed if starts with 0
			// However, YAML supports Octal, and Hexadecimals.
			c = getNext();
			if ( c == 'x' ) {
				// possible Hexadecimal value.
			}
		} else {
			// Scan till we have no digit
			while ( Character.isDigit( (c=getNext()) ) )
				;//keep looping till we do not have a digit.
		}
		if ( c=='.' ) {
			// we have decimal places.  Expect digit after '.'
			if ( !Character.isDigit( (c=getNext()) ))
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected digit after '.' in number" );
			// skip to the first non digit.
			while ( Character.isDigit( (c=getNext()) ))
				;// keep in loop till not a digit.
		}

		// Allow Exponent for a Number.
		if ( c=='e' || c=='E') {
			c = getNext();
			// allow '-' or '+'
			if ( c=='-' || c=='+' )
				c = getNext();
			// we expect at least one digit.
			if ( !Character.isDigit(c))
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected digit for exponent of number" );
			// Now continue till we have reached a non-digit.
			while ( Character.isDigit( (c=getNext()) ))
				;// keep in loop till not a digit.
		}
		// If we got here, then we have a number. Get the string of it.
		--at_;	// back up to the char just after the number.
		String sNumber = data_.substring(anchor,at_);
		return sNumber;

	}//scanInNumber()


	byte XXX = (byte)0xFF;	// Not Escaped Char
	byte NUL = 0x00;
	byte SOH = 0x01;
	byte STX = 0x02;
	byte ETX = 0x03;
	byte EOT = 0x04;
	byte ENQ = 0x05;
	byte ACK = 0x06;
	byte BEL = 0x07; // \a
	byte BS  = 0x08; // \b
	byte TAB = 0x09; // \t
	byte LF  = 0x0A; // \n
	byte VT  = 0x0B; // \v
	byte FF  = 0x0C; // \f
	byte CR  = 0x0D; // \r
	byte SO  = 0x0E; //
	byte SI  = 0x0F; //
	byte DLE = 0x10; //
	byte DC1 = 0x11; //
	byte DC2 = 0x12; //
	byte DC3 = 0x13; //
	byte DC4 = 0x14; //
	byte NAK = 0x15; //
	byte SYN = 0x16; //
	byte ETB = 0x17; //
	byte CAN = 0x18; //
	byte EM  = 0x19; //
	byte SUB = 0x1A; //
	byte ESC = 0x1B; // \e
	byte FS  = 0x1C; //
	byte GS  = 0x1D; //
	byte RS  = 0x1E; //
	byte US  = 0x1F; //

	// TODO: Find out what the following values should be.
	byte LS  = XXX;
	byte NEL = XXX;
	byte PS  = XXX;
	byte NBSP= XXX;

	byte escapedCodes[] = {
		 // NUL  SOH  STX  ETX  EOT  ENQ  ACK  BEL  BS   HT   LF   VT   FF   CR   SO   SI
			XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
		 // DLE  DC1  DC2  DC3  DC4  NAK  SYN  ETB  CAN  EM   SUB  ESC  FS   GS   RS   US
			XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
		 // ' '  !    "    #    $    %    &    '    (    )    *    +    ,    -    .    /
			' ', XXX, '\"',XXX, XXX, XXX, XXX, '\'',XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
		 // 0    1    2    3    4    5    6    7    8    9    :    ;    <    =    >    ?
			XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
		 // @    A    B    C    D    E    F    G    H    I    J    K    L    M    N    O
			XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, LS,  XXX, NEL, XXX,
		 // P    Q    R    S    T    U    V    W    X    Y    Z    [    \    ]    ^    _
			PS,  XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, NBSP,
		 // `    a    b    c    d    e    f    g    h    i    j    k    l    m    n    o
	        XXX, BEL, BS,  XXX, XXX, ESC, FF,  XXX, XXX, XXX, XXX, XXX, XXX, XXX, LF,  XXX,
		 // p    q    r    s    t    u    v    w    x    y    z    {    |    }    ~
	        XXX, XXX, CR,  XXX, TAB, XXX, VT,  XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
	     };


	// scanInQuotedString() returns quoted string.
	// at_ assumed to be at the first char of the string.
	// throws an exception if this was not a valid Quoted string.
	// returns the decoded string within the quotes (outside quotes omitted)
	// at_ is left at the char after the ending quote.
	protected String scanInQuotedString() throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInQuotedString() at="+at_ );

		char c = data_.charAt(at_++);
		if ( c != '\"' ) {
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected Quote to start string.");
		}
		StringBuilder sb = new StringBuilder();
		do {
			c = getNext();
			switch (c) {
			case '\"':
				// we have the full string
				String s = sb.toString();
				if (debug_>0) System.out.println("  STRING = " +s );
				return sb.toString();
			case '\n':
			case '\0':
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unbalanced Quotes before EOL");
			case '\\':
				c = getNext();
				byte code = escapedCodes[ c ];
				if ( code != XXX )
				{
					c = (char)code;
				}
				else if ( c == 'u' ) {
					boolean unicodeError = true;
					int uchar = 0;
					if ( at_+ 4 <= data_.length() ) {
						// Unicode expected
						String u = data_.substring(at_,at_+4 );
						try {
							uchar = Integer.parseInt(u,16);
							unicodeError = false;
							at_ += 4;
							c = (char)uchar;
						} catch ( Exception e ) {
							;//error condition.
						}
					}
					if ( unicodeError )
						throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected 4 hex digits for unicode.");
				}
				else {
					throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Invalid escape sequence.");
				}//switch after \
				break;
			default:
				;
			}//switch c
			sb.append( c );
		} while (true);
		// Will never exit while loop.

	}//scanInQoutedString()

	// appendQuotedString(). serializes a quoted string
	StringBuilder appendQuotedString( StringBuilder sb, String s) {
		sb.append('\"');	// opening quote
		int  len = s.length();
		char c;
		int  i;
		for (i=0; i<len; i++) {
			c = s.charAt(i);
			if ( Character.isAlphabetic(c) || Character.isDigit(c) )
				sb.append(c);
			else if ( c=='\\' || c=='\"' ) {
				sb.append('\\').append(c);
			}
			else if ( c >= 0x20 && c <= 0x7F ) {
				sb.append(c);
			}
			else {
				char echar = '\0';
				switch (c) {
				case '\n': echar='n'; break;
				case '\r': echar='r'; break;
				case '\t': echar='t'; break;
				case '\f': echar='f'; break;
				case '\b': echar='b'; break;
				default:
					String ucoded = String.format("\\u%04X", 0xFF & (int)c );
					sb.append(ucoded);
				}
				if ( echar != '\0' )
					sb.append('\\').append(echar);
			}//else
		}//for

		sb.append('\"');	// closing quote
		return sb;

	}//appendQuotedString


	// scanInAliasName()
	// Alias name stops at a whitespace boundary.
	// leaves at_ at the character after the first whitespace.
	// returns String ending with whitespace
	protected String scanInAliasName() throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInAliasName() at="+at_ );

		int anchor = at_;
		char c;
		do {
			c = getNext();
			if ( c == 0 || Character.isWhitespace(c) ) {
				return data_.substring(anchor, (c==0)?at_:at_-1 );
			}
			if ( c == ',' ) {
				pushBack();	// leave at_ on the ','
				return data_.substring(anchor, at_ );
			}
		} while (true);

	}//scanInAliasName()


	// scanInKey()
	protected String scanInKey() throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInKey() at="+at_ );

		char c = getNextNonSpaceChar();
		pushBack();
		switch(c) {
		case '"':
			return scanInQuotedString();
		case '\'':
			return scanInSingleString();
		case '\0':
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Enexpected end" );
		default:
			return scanInUnquotedKey();
		}
	}//scanInKey()


	// scanInUnquotedKey()
	// -- only escapes '\\' and '\ '
	// This allows for ab:\ barbor to equal the key "ab: barbor"
	//
	// throws exceptions for \n
	protected String scanInUnquotedKey() throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInUnquotkedKey() at="+at_ );

		char c = getNextNonSpaceChar();
		StringBuilder sb = new StringBuilder();
		sb.append(c);
		int spaces = 0;
		do {
			c = getNext();
			if ( c == ' ') {
				if ( lookAhead() == '#' ) {
					// stop on the comment. This should generate error.
					return sb.toString();
				}
				++spaces;
				continue;
			}
			switch(c) {
			case '\\':
				for ( ;spaces > 0; --spaces )
					sb.append(' '); // add in unpushed spaces.
				c = getNext();
				if ( c != '\\'  &&  c != ' ' )
					sb.append('\\');
				break;
			case ':':
				switch( lookAhead() ) {
				case ' ':
				case '\n':
				case '[':
				case ']':
				case '{':
				case '}':
				case '~':
				case ',':
					pushBack();
					return sb.toString();
				case '\0':
					return sb.toString();
				default:
					break;
				}
				break;
			case '\0':
			case '\r':
			case '\n':
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected end of data looking for Key." );
			default:
				break;
			}
			for ( ;spaces > 0; --spaces )
				sb.append(' '); // add in unpushed spaces.
			sb.append(c);
		} while (true);
	}// scanInUnquotedKey()


	// scanInUnquotedString() (Unquoted String) Does not accept encoding.
	// at_ assumed to be on the first char of the string.
	// The string ends with the last non whitespace char before a ':', or '\n' or '#'
	protected String scanInUnquotedString( ) {
		if (debug_>0) System.out.println("Enter: scanInUnquotedString() at="+at_ );

		char c = getNextNonSpaceChar();
		if (c == '\0')
			return "";
		int start      = at_-1;
		boolean bBreak = false;
		int spaces     = 0;

		do {
			c = getNext();
			switch( c ) {
			case ' ':
				++spaces;
				break;
//			case ':':
			case ',':
			case '}':
			case ']':
			case '\t':
			case '\n':
			case '\r':
			case '\0':
				bBreak = true;	// Stop scanning when one of these symbols have been reached.
				break;
			case '#':
				if ( spaces > 0 )
					bBreak = true;	// Stop at this comment.
				else
					spaces = 0;
				break;
			default:
				spaces = 0;
			}//switch
		} while ( !bBreak );

		if ( c != '\0' )
			pushBack();

		String s = data_.substring(start,at_-spaces);
		if (debug_>0) System.out.println("  STRING = " + s );

		return s;

	}//scanInUnquotedString


	// scanInSingleString() (Single quoted String (')) Does not decode except for ''.
	// The string ends with single '
	// The first non-space char must be '
	protected String scanInSingleString( ) throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInSingleString() at="+at_ );

		char c = getNextNonSpaceChar();
		if ( c != '\'' )
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected (') symbol." );

		StringBuilder sb = new StringBuilder();

		do {
			c = getNext();
			if ( c=='\'' ) {
				if ( lookAhead() != '\'' ) {
					// At end of string. Return what we have.
					return sb.toString();
				}
				// we have a (') character to absorb.
				c = getNext();
			}
			else if ( c == '\0' ) {
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "End of single quoted string invalid" );
			}
			sb.append(c);
		} while ( true );

	}//scanInSingleString


	// scanInJsonObject()
	// Object = '{' [ string : Value [, string : Value ]* ] '}'
	//
	protected PGPNode scanInJsonObject( PGPNode node ) throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInObject() at="+at_ );

		char c = getNextNonSpaceChar();
		if ( c != '{' ) {
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected '{' to start Object." );
		}
		node.setType( PGPDataType.OBJECT );

		do {
			c = getNextNonSpaceChar();
			if ( c == '}' )
				break;	// At end of the Object

			if ( c == ',' ) {
				if ( node.headChild_ == null )
					throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected ','" );
				c = getNext();
			}

			// Expect a string : VALUE

			pushBack();	// We let the string routine take in the first char
			PGPNode child;
			String key = scanInKey();
			child = node.addChild( key );

			c = getNextNonSpaceChar();
			if ( c != ':' )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected ':'" );

			scanInJsonValue( child );

		} while (true);

		if (debug_>0) System.out.println("Exit:  scanInObject()" );

		return node;

	}//scanInJsonObject()


	// scanInJsonArray()
	// Object = '[' [ Value [, Value ]* ] ']'
	//
	protected PGPNode scanInJsonArray( PGPNode node ) throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInArray() at="+at_ );

		char c = getNextNonSpaceChar();
		if ( c != '[' ) {
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected '[' to start Array." );
		}
		node.setType( PGPDataType.ARRAY );

		do {
			c = getNextNonSpaceChar();
			if ( c == ']' )
				break;	// At end of the Object

			if ( c == ',' ) {
				if ( node.headChild_ == null )
					throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected ','" );
				c = getNext();
			}

			// Expect a string : VALUE

			pushBack();	// We let the string routine take in the first char
			PGPNode child = node.addChild( );
			scanInJsonValue( child );

		} while (true);

		if (debug_>0) System.out.println("Exit:  scanInArray()" );

		return node;

	}//scanInJsonArray()


	// scanInJsonValue
	//
	protected PGPNode scanInJsonValue( PGPNode node ) throws PGPException {
		if (debug_>0) System.out.println("Enter: scanInValue() at="+at_ );

		String aliasName = null;

		char c = getNextNonSpaceChar();

		// Alias Support
		if ( c == '*' ) {
			// Reference an existing alias
			aliasName = scanInAliasName();
			// Lookup the alias and copy it's node
			try {
				PGPNode aliasNode = aliases_.get(aliasName);
				node.setValue( aliasNode );
			}
			catch(Exception e) {
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unknown alias encountered ("+aliasName+")" );
			}
			return node;
		}
		if ( c == '&' ) {
			// Will define an alias at this node
			// This can then recall this node when the alias is referenced.
			aliasName = scanInAliasName();
			if ( aliases_.containsKey(aliasName) )
				aliases_.replace(aliasName, node);
			else
				aliases_.put(aliasName, node);
			c = getNextNonSpaceChar();
		}

		pushBack();
		if ( c == '\"' ) {
			node.setValue( scanInQuotedString(), PGPNode.PGPDataType.STRING );
			if (debug_>0) System.out.println("  STRING: "+node.getValue() );
		}
		else if ( c == '\'' ) {
			node.setValue( scanInSingleString(), PGPNode.PGPDataType.STRING );
			if (debug_>0) System.out.println("  Single STRING: "+node.getValue() );
		}
		else if ( c == '{' ) {
			scanInJsonObject(node);
		}
		else if ( c == '[' ) {
			scanInJsonArray(node);
		}
		else if ( (c == '-') || Character.isDigit(c) ) {
			node.setValue( scanInNumber(), PGPNode.PGPDataType.NUMBER );
			if (debug_>0) System.out.println("  NUMBER: "+node.getValue() );
		}
		else if ( Character.isAlphabetic(c)) {
			String value = scanInUnquotedString();
			switch(value) {
			case "null":
				node.setType( PGPNode.PGPDataType.NULL );
				if (debug_>0) System.out.println("  NULL" );
				break;
			case "true":
			case "True":
			case "TRUE":
			case "on":
			case "On":
			case "ON":
			case "y":
			case "Y":
			case "yes":
			case "Yes":
			case "YES":
				node.setValue( "true", PGPNode.PGPDataType.BOOLEAN );
				if (debug_>0) System.out.println("  BOOLEAN: "+value );
				break;
			case "false":
			case "False":
			case "FALSE":
			case "off":
			case "Off":
			case "OFF":
			case "n":
			case "N":
			case "no":
			case "No":
			case "NO":
				node.setValue( "false", PGPNode.PGPDataType.BOOLEAN );
				if (debug_>0) System.out.println("  BOOLEAN: "+value );
				break;
			default:
				node.setValue( value, PGPNode.PGPDataType.UNQUOTED_STRING );
				if (debug_>0) System.out.println("  UNQUOTED_STRING: "+value );
			}
		}
		else
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Invalid character encountered ("+c+")" );

		if (debug_>0) System.out.println("Exit:  scanInValue()" );

		return node;

	}//scanInValue()


	// parse() includes any optional flags.
	public PGPNode parse( String data, PGPOptionFlags flags ) throws PGPException
	{
		flags_ = flags;
		return parse( data );
	}

	// parser() -- Scan a JSON message and create a PGPNode structure.
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
		aliases_      = new Hashtable<String,PGPNode>();
		indentStack_  = new int[100];
		indentCount_  = -1;
		indentPush(0);

		scanInJsonObject(topNode);
		if ( getNextNonSpaceChar() != '\0' ) {
			throw new PGPException( at_, onLine_, onCharOfLine_, 1, "END Expected");
		}

		return topNode;
	}//parse()

	public void indentPush( int value ) {
		indentStack_[ ++indentCount_ ] = value;
	}

	public int indentPop() {
		return indentStack_[ --indentCount_ ];
	}

}//class JsonPGParsers
