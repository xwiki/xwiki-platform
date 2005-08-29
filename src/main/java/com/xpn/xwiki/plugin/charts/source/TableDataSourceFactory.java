package com.xpn.xwiki.plugin.charts.source;

import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public class TableDataSourceFactory implements DataSourceFactory {
    private static DataSourceFactory uniqueInstance = new TableDataSourceFactory();

    private TableDataSourceFactory() {
        // empty
    }

    public static DataSourceFactory getInstance() {
        return uniqueInstance;
    }

	public DataSource create(Map params, XWikiContext context) throws DataSourceException{
		return new TableDataSource(params, context);
	}
}
