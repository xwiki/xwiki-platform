package com.xpn.xwiki.plugin.charts.exceptions;

public class ColumnIndexOutOfBoundsException extends DataSourceException {
	public ColumnIndexOutOfBoundsException() {
		super();
	}

	public ColumnIndexOutOfBoundsException(String message) {
		super(message);
	}

	public ColumnIndexOutOfBoundsException(String message, Throwable cause) {
		super(message, cause);
	}

	public ColumnIndexOutOfBoundsException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = -3084651859544518534L;
}
