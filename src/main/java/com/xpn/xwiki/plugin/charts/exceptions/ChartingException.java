package com.xpn.xwiki.plugin.charts.exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;

public class ChartingException extends Exception {

	public ChartingException() {
		super();
	}

	public ChartingException(String message) {
		super(message);
	}

	public ChartingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChartingException(Throwable cause) {
		super(cause);
	}

	public String getMessage() {
		if (getCause()!=null) {
			return getCause().getMessage();
		} else {
			return super.getMessage();
		}
	}
	
	public void printStackTrace() {
		if (getCause()!=null) {
			getCause().printStackTrace();
		} else {
			super.printStackTrace();
		}
	}

	public void printStackTrace(PrintStream s) {
		if (getCause()!=null) {
			getCause().printStackTrace(s);
		} else {
			super.printStackTrace(s);
		}
	}
	
	public void printStackTrace(PrintWriter s) {
		if (getCause()!=null) {
			getCause().printStackTrace(s);
		} else {
			super.printStackTrace(s);
		}
	}
}
