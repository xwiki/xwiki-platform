package com.xpn.xwiki.plugin.charts.source;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public class ObjectidDataSourceFactory extends AbstractDataSourceFactory
		implements DataSourceFactory {
	private static DataSourceFactory uniqueInstance = new ObjectidDataSourceFactory();
	
	private ObjectidDataSourceFactory() {
		// empty
	}

	public static DataSourceFactory getInstance() {
		return uniqueInstance;
	}
	
	public DataSource create(String[] args, XWikiContext context)
			throws DataSourceException {
		checkArgumentCount(args, 2);
		int objectid;
		try {
			objectid = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new DataSourceException(e);
		}
		
		BaseObject xobj;
		try {
			List list = context.getWiki().getStore().search("from "+BaseObject.class.getName()
					+ " as obj where obj.id='" + objectid + "'", 0, 0, context);
			if (list.size() == 0) {
				throw new DataSourceException("Object ID not found");
			}
			xobj = (BaseObject)list.get(0);
			List propertyList = context.getWiki().getStore().search("from " + BaseProperty.class.getName()
					+ " as p where p.id.id='" + objectid + "'", 0, 0, context);
			Iterator it = propertyList.iterator();
			while (it.hasNext()) {
				BaseProperty prop = (BaseProperty)it.next();
				xobj.addField(prop.getName(), prop);
			}
		} catch (XWikiException e) {
			throw new DataSourceException(e);
		}
		
		String xclass = xobj.getClassName();
		if (!xclass.startsWith("XWiki.")) {
			throw new DataSourceException("XWiki prefix missing");
		}
		
		String className = DataSource.class.getPackage().getName()
			+ "." + xclass.substring("XWiki.".length());

		try {
			Class class_ = Class.forName(className);
			Constructor ctor = class_.getConstructor( new Class[] {
					BaseObject.class, XWikiContext.class});
			return (DataSource)ctor.newInstance(new Object[] {xobj, context});
		} catch (Exception e) {
			throw new DataSourceException(e);
		}
	}
}
