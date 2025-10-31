package com.risenperspectives.pgparsers;

//
// by: D. Lance Robinson
// Risen Perspectives, LLC
// February, 2025
//

import java.util.Hashtable;

import com.risenperspectives.pgparsers.PGPNode.PGPDataType;

public class FlatPGParser implements PGParsersInterface {

	public  PGPOptionFlags flags_ = new PGPOptionFlags();
	private int    debug_;

	private String data_;
	public  int    onLine_;
	public  int    at_;
	private boolean bPastSpaces_;
	public  int    onCharOfLine_;

	public String protocolName() { return "FLAT"; }

	public FlatPGParser() {
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


	public String decodeValue( String data, int start, int end ) {

		return data.substring(start,end);

	}//decodeValue()


	// parse() includes any optional flags.
	public PGPNode parse( String data, PGPOptionFlags flags ) throws PGPException
	{
		flags_ = flags;
		return parse( data );
	}

	// parse() -- Scan a FLAT message and create a PGPNode structure.
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
			// scan in the full name (before the '=', or an Attributes '#' )
			int nameAnchor = at_-1;
			while ( (c = getNext() ) != 0 && c != '=' && c != '#' && c != '\n' ) {
				;// keep scanning
			}
			if ( c == 0 )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Unexpected EOF found" + at_ );
			if ( c == '\n' )
				throw new PGPException( at_, onLine_, onCharOfLine_, 1, "'=' not found on line at" + at_ );

			String nodeName = data_.substring(nameAnchor, at_-1);
			PGPNode node = topNode.getNodeFromFlatName( nodeName );

			if ( c == '#' ) {
				// We have an attribute.
				// scan in the attribute name.
				nameAnchor = at_;
				while ( (c = getNext() ) != 0 && c != '=' && c != '\n' ) {
					;// keep scanning
				}
				if ( c != '=' )
					throw new PGPException( at_, onLine_, onCharOfLine_, 1, "Expected '=' not found after attribute name" + at_ );

				String attrName = data_.substring(nameAnchor, at_-1);
				node = node.addAttr( attrName );

			}

			// The last item scanned should have been '='
			int valueAnchor = at_;
			while ( (c = getNext() ) != 0 && c != '\n' ) {
				;// keep scanning
			}

			int valueLen = at_ - valueAnchor;
			if ( c != 0 )
				valueLen--; // don't include the '\n'
			if (  valueLen >= 0 ) {
				String value = decodeValue( data_, valueAnchor, valueAnchor+valueLen );
				node.setValue( value, PGPDataType.STRING );
			}

		}//while not at end

		return topNode;

	}//parse()

}//class FlatPGParsers
