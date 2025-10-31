package com.risenperspectives.pgparsers;

import com.risenperspectives.pgparsers.PGPNode.PGPDataType;

public class JsonPGPSerializer implements PGPSerializerInterface {

	public PGPOptionFlags flags_ = new PGPOptionFlags();
	private int    depthSpaces_ = 2;
    private String depthString_ = "  ";
	private int    debug_;

	public String protocolName() { return "JSON"; }

	public void setFlags( PGPOptionFlags flags ) {
		flags_ = flags;
	}
	public PGPOptionFlags getFlags() {
		return flags_;
	}

	public void setDebugLevel( int dbgLevel ) {
		debug_ = dbgLevel;
	}

	public void setDepthSpaces( int depthSpaces)
	{
		depthSpaces_ = depthSpaces;
		depthString_ = "                ".substring(0,depthSpaces);
	}
	public int    getDepthSpaces() { return depthSpaces_; }
	public String getDepthString()  { return depthString_; }

	public String getLastErrorText() {
		return null;
	}

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

	byte encodingCodes[] = {
			 // NUL  SOH  STX  ETX  EOT  ENQ  ACK  BEL  BS   HT   LF   VT   FF   CR   SO   SI
				XXX, XXX, XXX, XXX, XXX, XXX, XXX, 'a', 'b', 't', 'n', 'v', XXX, 'l', XXX, XXX,
			 // DLE  DC1  DC2  DC3  DC4  NAK  SYN  ETB  CAN  EM   SUB  ESC  FS   GS   RS   US
				XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
			 // ' '  !    "    #    $    %    &    '    (    )    *    +    ,    -    .    /
				' ', XXX, '\"',XXX, XXX, XXX, XXX, '\'',XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
			 // 0    1    2    3    4    5    6    7    8    9    :    ;    <    =    >    ?
				XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
			 // @    A    B    C    D    E    F    G    H    I    J    K    L    M    N    O
				XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
			 // P    Q    R    S    T    U    V    W    X    Y    Z    [    \    ]    ^    _
				XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, '\\', XXX, XXX, XXX,
			 // `    a    b    c    d    e    f    g    h    i    j    k    l    m    n    o
		        XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,
			 // p    q    r    s    t    u    v    w    x    y    z    {    |    }    ~
		        XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX, XXX,

	};


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


	// serialize() -- with Option flags
	public String serialize( PGPNode topNode, PGPOptionFlags flags )
	{
		return serialize( new StringBuilder(1024), topNode, 0 ).toString();
	}

	// serialize() -- simplest form.
	public String serialize( PGPNode topNode ) {
		return serialize( new StringBuilder(1024), topNode, null ).toString();
	}

	public StringBuilder serialize( StringBuilder sb, PGPNode node, PGPOptionFlags flags ) {
		flags_ = (null == flags) ? new PGPOptionFlags() : flags;
		PGPNode siblingNode = node;
		do {
			serialize( sb, siblingNode, 0 );
			siblingNode = siblingNode.next_;
		} while ( (null == getLastErrorText()) && (siblingNode != null) && (siblingNode != node) );

		return serialize( sb, node, 0 );
	}

	// serialize() -- renders the node and subnodes to look like JSON output
	//  returns the StringBuilder.
	public StringBuilder serialize( StringBuilder sb, PGPNode node, int atDepth ) {
		int d;

		if ( (null != node.parent_) && (node != node.parent_.headChild_) ) {
			// we are mid list
			sb.append(',');
		}

		if ( getDepthSpaces() > 0 ) {
			// Flush the current line if any.
			if ( sb.length() > 0  && sb.charAt(sb.length()-1) != '\n')
				sb.append('\n');

			//  depth
			for (d=0; d<atDepth; ++d)
				sb.append( getDepthString() );
		}

		//  the Name;
		if ( null != node.name_ ) {
			sb.append('"').append(node.name_).append("\":");
			if ( getDepthSpaces() != 0 )
				sb.append(' ');		// Add space after ':'
		}

		//  the value;
		if ( null != node.value_ ) {
			PGPDataType type = node.type_;
			if ( type == PGPDataType.STRING || type == PGPDataType.UNQUOTED_STRING || type == PGPDataType.UNESCAPED_STRING )
				appendQuotedString( sb, node.getValue() );
			else
				node.appendValue(sb);
		}//if value_

		PGPNode subnode;

		//  the Attributes
		// This assumes attributes have no children.
		// But really, JSON should never have an attribute list.
		if ( node.headAttr_ != null ) {
			sb.append( (getDepthSpaces()<=0)?",attr:{" : ", attr:{" );
			subnode = node.headAttr_;
			boolean isFirst = true;
			do {
				if ( isFirst )
					isFirst = false;
				else {
					// need comma between attributes
					sb.append( (getDepthSpaces() <= 0) ? ',' : ", " );
				}
				sb.append(subnode.name_).append(':');
				if ( getDepthSpaces() <= 0 )
					sb.append(' ');
				subnode.appendValue(sb);
				subnode = subnode.next_;
			} while ( subnode != node.headAttr_ );
			sb.append("}");
			if ( getDepthSpaces() <= 0 )
				sb.append(' ');
		}//if headAttr_

		switch( node.type_ ) {
		case ARRAY:	sb.append("[" );
					break;
		case OBJECT:sb.append("{" );
		default:
		}

		//  The Children
		if ( null != node.headChild_ ) {
			// There are children. They take the form of an ARRAY or of an OBJECT
			//
			subnode = node.headChild_;
			do {
				serialize( sb, subnode, atDepth+1 );
				subnode = subnode.next_;
			} while ( subnode != node.headChild_ );

			if ( getDepthSpaces() > 0 ) {
				// Flush sb buffer if not empty
				if ( sb.length() > 0 ) {
					sb.append('\n');
				}
			}

			//  depth
			for (d=0; d<atDepth; ++d)
				sb.append( getDepthString() );
		}//if headChild_

		switch( node.type_ ) {
		case ARRAY:	sb.append(']' );
					break;
		case OBJECT: sb.append('}');
					break;
		default:
		}

		return sb;

	}//serialize()

}//class JsonPGPSerializer
