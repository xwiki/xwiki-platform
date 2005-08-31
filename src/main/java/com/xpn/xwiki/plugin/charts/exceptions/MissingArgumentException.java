package com.xpn.xwiki.plugin.charts.exceptions;

public class MissingArgumentException extends ParamException {

	public MissingArgumentException() {
		super();
	}

	public MissingArgumentException(String arg0) {
		super(arg0);
	}

	public MissingArgumentException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public MissingArgumentException(Throwable arg0) {
		super(arg0);
	}
}
