package com.xpn.xwiki.plugin.charts.source;

import java.lang.reflect.Method;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;


public class MainDataSourceFactory implements DataSourceFactory {
	private static DataSourceFactory uniqueInstance = new MainDataSourceFactory();
	
	private MainDataSourceFactory() {
		// empty
	}
	
	public static DataSourceFactory getInstance() {
		return uniqueInstance;
	}
	
	public DataSource create(String source, XWikiContext context) throws DataSourceException {
		return create(source.split(":"), context);
	}
	
	public DataSource create (String[] args, XWikiContext context) throws DataSourceException {
		String prefix = args[0];
		
		if ("".equals(prefix)) {
			throw new DataSourceException("Empty datasource prefix "+prefix+" in: "+args); 
		}

		String className = DataSource.class.getPackage().getName() + "." +
			Character.toUpperCase(prefix.charAt(0)) +
			prefix.toLowerCase().substring(1) + "DataSourceFactory";
		
		DataSourceFactory factory; 
		try {
			Class class_ = Class.forName(className);
			Method method = class_.getMethod("getInstance", new Class[] {});
			factory = (DataSourceFactory)method.invoke(null, new Object[] {});
		} catch (Exception e) {
			throw new DataSourceException(e);
		}
		return factory.create(args, context);		
	}
}
