package com.risenperspectives.pgparsers;

public interface PGParsersInterface {

	public String protocolName();	// Should return "XML", "JSON", "YAML", etc

	// parse() -- scans the text to produce a PGPNode structure
	// returns the top PGPNode or thows an exception if a syntax error was encountered.
	// If there was an error in the text. The getLastErrorXXXX() methods will reveal.
	//
	PGPNode parse( String data ) throws PGPException;
	PGPNode parse( String data, PGPOptionFlags flags ) throws PGPException;

	void          setFlags( PGPOptionFlags flags );
	PGPOptionFlags getFlags();

	// setDebugLevel()
	// level 0 is none, 1 and 2 are increasing levels of info
	void setDebugLevel( int dbgLevel );

	// getLastErrorText()
	// Returns null if none, or info on the last error encountered.
	String getLastErrorText();

}// PGParsersInterface
