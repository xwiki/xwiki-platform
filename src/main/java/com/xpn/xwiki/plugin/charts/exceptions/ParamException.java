package com.xpn.xwiki.plugin.charts.exceptions;

public abstract class ParamException extends ChartingException {
	public ParamException() {
		super();
	}

	public ParamException(String arg0) {
		super(arg0);
	}

	public ParamException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ParamException(Throwable arg0) {
		super(arg0);
	}

	private static final long serialVersionUID = -5396436999112187487L;
}
