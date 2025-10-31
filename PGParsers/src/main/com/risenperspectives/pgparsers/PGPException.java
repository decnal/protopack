package com.risenperspectives.pgparsers;

public class PGPException extends Exception {

	public int at_;
	public int line_;
	public int onCharOfLine_;
	public int error_;
	public String message_;

	public PGPException( int at, int line, int onCharOfLine, int error, String message ) {
		at_           = at;
		line_         = line;
		onCharOfLine_ = onCharOfLine;
		error_        = error;
		message_      = message;
	}

	public PGPException( int error, String message ) {
		at_           = 0;
		line_         = 0;
		onCharOfLine_ = 0;
		error_        = error;
		message_      = message;
	}

	public String asString()      { return message_; }
	public int    getError()      { return error_;   }
	public int    getErrorLine()  { return line_;    }
	public int    getErrorCharOfLine() { return onCharOfLine_; }
	public int    getErrorAt()    { return at_;      }
	public String getMessage()    { return message_; }

}
