package com.risenperspectives.pgparsers;

import com.risenperspectives.pgparsers.PGPNode.PGPDataType;

public class FlatPGPSerializer implements PGPSerializerInterface {

	public PGPOptionFlags flags_ = new PGPOptionFlags();
	private int    debug_;
	private String EOL = System.getProperty("line.separator");

	public String protocolName() { return "FLAT"; }

	public void setFlags( PGPOptionFlags flags ) {
		flags_ = flags;
	}
	public PGPOptionFlags getFlags() {
		return flags_;
	}

	public void setDebugLevel( int dbgLevel ) {
		;//nothing to do
	}

	public void setDepthSpaces( int depthSpaces)
	{
		;//nothing to do
	}
	public int    getDepthSpaces() { return 0; }
	public String getDepthString()  { return ""; }

	public String getLastErrorText() {
		return null;
	}

	// encodeAndAppend().  Encodes a string and appends it to the StringBuilder.
	public void encodeAndAppend( StringBuilder sb, String s ) {

		if ( null != s)
			sb.append(s);	//No encoding needed for Flat

	}//encodeAndAppend


	// appendValue(). serializes a value (quoted or unquoted)
	public void appendValue( StringBuilder sb, PGPNode node, boolean bAttrValue)
	{
		encodeAndAppend(sb, node.getValue());

	}//appendValue


	// serialize() -- with Option flags
	public String serialize( PGPNode topNode, PGPOptionFlags flags )
	{
		return serialize( new StringBuilder(1024), topNode, flags ).toString();
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

	// serialize() -- renders the node and subnodes to look like Flat Namespace output
	//  returns the StringBuilder.
	public StringBuilder serialize( StringBuilder sb, PGPNode node, int atDepth ) {

		if ( null == sb )
			sb = new StringBuilder( 2000 );
		StringBuilder sbFlatName = node.appendFlatName( sb );
		String flatName = sbFlatName.toString();

		// Add in the attributes if any
		PGPNode firstAttr = node.getAttr();
		PGPNode subnode   = firstAttr;
		while ( null != subnode ) {
			sb.append(flatName).append('#').append(subnode.getName()).append('=');
			appendValue(sb,subnode,true);
			sb.append(EOL);

			subnode = subnode.getNext();
			if ( subnode == firstAttr)
				break;	//Done
		}

		if ( null != node.getValue() || (null == node.getChild() && null == node.getAttr()) ) {
			// Output the FlatName=value
			sb.append(flatName).append('=');
			appendValue(sb, node, false);
			sb.append(EOL);
		}

		// Go through and output the children
		PGPNode firstChild = node.getChild();
		subnode            = firstChild;
		while ( null != subnode ) {
			serialize( sb, subnode, atDepth+1 );
			subnode = subnode.getNext();
			if ( subnode == firstChild )
				break;
		}

		return sb;

	}//serialize()

}//class FlatPGPSerializer
