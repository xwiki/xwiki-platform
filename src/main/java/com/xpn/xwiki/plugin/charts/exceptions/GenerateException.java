package com.xpn.xwiki.plugin.charts.exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;

public class GenerateException extends Exception {

	public GenerateException() {
		super();
	}

	public GenerateException(String arg0) {
		super(arg0);
	}

	public GenerateException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public GenerateException(Throwable arg0) {
		super(arg0);
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
	
	private static final long serialVersionUID = -5964622165845472767L;
}
