package com.xpn.xwiki.plugin.charts.exceptions;

public class EmptyDataSourceException extends DataSourceException {
    public EmptyDataSourceException() {
        super();
    }

    public EmptyDataSourceException(String arg0) {
        super(arg0);
    }

    public EmptyDataSourceException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public EmptyDataSourceException(Throwable arg0) {
        super(arg0);
    }

    private static final long serialVersionUID = -5868540511395442568L;
}
