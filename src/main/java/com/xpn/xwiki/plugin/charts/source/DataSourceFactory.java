package com.xpn.xwiki.plugin.charts.source;

import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public interface DataSourceFactory {
	
	public DataSource create(Map params, XWikiContext context) throws DataSourceException;
	
}