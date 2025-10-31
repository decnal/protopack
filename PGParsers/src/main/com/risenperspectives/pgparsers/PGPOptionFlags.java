package com.risenperspectives.pgparsers;

public class PGPOptionFlags {

	// FLAG values must be a power of 2 (representing a bit)
	public static final int NONE = 0;
	public static final int PRETTY_PRINT = 1;
	public static final int STRIP_WHITESPACE = 2;
	public static final int CRLF_EOL = 4;
	public static final int DOUBLE_QUOTE_ATTR = 8;	// For XML, default is Single quotes.

	private int flags_ = 0;

	public PGPOptionFlags() {
		flags_ = 0;
	}

	public PGPOptionFlags( int flags ) {
		flags_ = flags;
	}

	public int getFlagsInt() { return flags_; }

	public boolean isSet( int flags )
	{
		if ( flags == NONE)
			return (flags_ == 0);
		else
			return (flags_ & flags) == flags;
	}

	public boolean isNotSet( int flags )
	{
		if ( flags == NONE)
			return (flags_ != 0);
		else
			return (flags_ & flags) != flags;
	}

	public void setFlag( int flags)
	{
		if ( flags == NONE )
			flags_ = 0;
		else
			flags_ |= flags;
	}

	public void clearFlag( int flags)
	{
		if ( flags != NONE )
			flags_ &= ~flags;
	}

}// PGOptionalFlags
