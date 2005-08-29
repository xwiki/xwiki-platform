package com.xpn.xwiki.plugin.charts.source;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

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
    
	public DataSource create(Map params, XWikiContext context)
			throws DataSourceException {
		String type = (String)params.get("type");
		if (type == null || "".equals(type)) {
			throw new DataSourceException("Empty datasource type"); 
		}
		String factoryClassName = DataSource.class.getPackage().getName() + "." +
				Character.toUpperCase(type.charAt(0)) +
				type.toLowerCase().substring(1) + "DataSourceFactory";
	
		try {
			Class class_ = Class.forName(factoryClassName);
			Method method = class_.getMethod("getInstance", new Class[] {});
			DataSourceFactory factory = (DataSourceFactory)method.invoke(null, new Object[] {});
			return factory.create(params, context);
		} catch (InvocationTargetException e) {
			throw new DataSourceException(e.getTargetException());
		} catch (Exception e) {
			throw new DataSourceException(e);
		}
	}
}
