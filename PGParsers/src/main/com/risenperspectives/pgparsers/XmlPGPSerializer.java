package com.risenperspectives.pgparsers;

import com.risenperspectives.pgparsers.PGPNode.PGPDataType;

public class XmlPGPSerializer implements PGPSerializerInterface {

	public PGPOptionFlags flags_ = new PGPOptionFlags();
	private int    depthSpaces_ = 2;
    private String depthString_ = "  ";
	private int    debug_;

	public String protocolName() { return "XML"; }

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


	// encodeAndAppend().  Encodes a string and appends it to the StringBuilder.
	public void encodeAndAppend( StringBuilder sb, String s ) {
		if ( null == s ) return;
		int len = s.length();
		char c;
		for (int i=0; i<len; i++) {
			c = s.charAt(i);
			switch (c) {
			case '&': sb.append("&amp;"); break;
			case '<': sb.append("&lt;"); break;
			case '>': sb.append("&gt;"); break;
			case '\"': sb.append("&quot;"); break;
			case '\'': sb.append("&apos;"); break;
			default:
				if ( c >= 0x20 && c <= 0x7F )
					sb.append(c);
				else {
					String ucoded = String.format("&#x%04X;", 0xFF & (int)c );
					sb.append(ucoded);
				}
			}
		}
	}//encodeAndAppend


	// appendValue(). serializes a value (quoted or unquoted)
	public void appendValue( StringBuilder sb, PGPNode node, boolean bAttrValue)
	{
		String value = node.getValue();
		if ( null == value )
			return;

		PGPDataType type = node.getType();
		if ( type == PGPDataType.STRING && !bAttrValue )
			type = PGPDataType.UNQUOTED_STRING;

		switch ( type )
		{
		case UNQUOTED_STRING:
				encodeAndAppend(sb, node.getValue());
				break;
		case UNESCAPED_STRING:
				sb.append("<![CDATA[").append(node.getValue()).append("]]>");
				break;
		default:
				sb.append('"');
				encodeAndAppend(sb, node.getValue());
				sb.append('"');
				break;
		}
	}//appendValue


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

	// serialize() -- renders the node and subnodes to look like XML output
	//  returns the StringBuilder.
	public StringBuilder serialize( StringBuilder sb, PGPNode node, int atDepth ) {
		int d;

		if ( getDepthSpaces() > 0 ) {
			// Flush the current line if any.
			if ( sb.length() > 0  && sb.charAt(sb.length()-1) != '\n')
				sb.append('\n');

			//  depth
			for (d=0; d<atDepth; ++d)
				sb.append( getDepthString() );
		}

		//  the <Name>, or <Name attr='xxx'> or <Name/>, or <Name attr='xxx'/>;
		if ( null != node.getName() )
		{
			sb.append('<').append(node.name_);

			// Add in the attributes if any
			PGPNode firstAttr = node.getAttr();
			PGPNode subnode   = firstAttr;
			while ( null != subnode ) {
				sb.append(' ').append(subnode.getName()).append('=');
				appendValue(sb,subnode,true);

				subnode = subnode.getNext();
				if ( subnode == firstAttr)
					break;	//Done
			}

			// Close the element name
			if ( node.getType() == PGPDataType.NULL && null == node.getChild() )
				sb.append('\'');
			sb.append('>');

		}//if name

		// Output the value (if specified)
		appendValue( sb, node, false);

		// Go through and output the children
		PGPNode firstChild = node.getChild();
		PGPNode subnode    = firstChild;
		while ( null != subnode ) {
			serialize( sb, subnode, atDepth+1 );
			subnode = subnode.getNext();
			if ( subnode == firstChild )
				break;
		}

		// Add in the element closing tag
		if ( null != node.getName() )
		{
			if ( null != subnode && getDepthSpaces() > 0 ) {
				// Flush the current line if any.
				if ( sb.length() > 0  && sb.charAt(sb.length()-1) != '\n')
					sb.append('\n');

				//  depth
				for (d=0; d<atDepth; ++d)
					sb.append( getDepthString() );
			}

			sb.append('<').append('/').append(node.getName()).append('>');

		}//if name

		return sb;

	}//serialize()

}//class XmlPGPSerializer
