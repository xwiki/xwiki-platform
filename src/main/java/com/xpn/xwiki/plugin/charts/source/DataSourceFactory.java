package com.xpn.xwiki.plugin.charts.source;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public interface DataSourceFactory {

	public DataSource create(String source, XWikiContext context)
			throws DataSourceException;

	public DataSource create(String[] args, XWikiContext context)
			throws DataSourceException;

}