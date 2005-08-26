package com.xpn.xwiki.plugin.charts.source;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public abstract class AbstractDataSourceFactory implements DataSourceFactory {

	public DataSource create(String source, XWikiContext context)
			throws DataSourceException {
		return create(source.split(":"), context);
	}

	public abstract DataSource create(String[] args,
			XWikiContext context) throws DataSourceException;

	protected void checkArgumentCount(String[] args, int expected) throws DataSourceException {
		if (args.length != expected) {
			throw new DataSourceException("Illegal datasource number of arguments for "
					+args[0]+" (expected:"+expected+", present:"+args.length+")"); 				
		}
	}
}
