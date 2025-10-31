package com.risenperspectives.pgparsers;

public interface PGPSerializerInterface {

	// serialize() -- Creates output recursively from the GPNode structure
	//

	public String protocolName();	// Should return "XML", "JSON", "YAML", etc
	public void setDepthSpaces( int depthSpaces);
	public int  getDepthSpaces();

	void          setFlags( PGPOptionFlags flags );
	PGPOptionFlags getFlags();

	// serialize() -- render the node and sub-nodes by appending to the StringBuilder
	// If depthSpaces==0, then whitespace is minimalized.
	String serialize( PGPNode topNode );
	String serialize( PGPNode topNode, PGPOptionFlags flags );
	StringBuilder serialize( StringBuilder sb, PGPNode topNode, PGPOptionFlags flags );

	// setDebugLevel()
	// level 0 is none, 1 and 2 are increasing levels of info
	void setDebugLevel( int dbgLevel );

	// getLastErrorText()
	// Returns null if none, or info on the last error encountered.
	String getLastErrorText();

}
