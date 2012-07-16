/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.charts.source;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public class ObjectidDataSourceFactory implements DataSourceFactory
{
    private static DataSourceFactory uniqueInstance = new ObjectidDataSourceFactory();

    private ObjectidDataSourceFactory()
    {
        // empty
    }

    public static DataSourceFactory getInstance()
    {
        return uniqueInstance;
    }

    @Override
    public DataSource create(Map params, XWikiContext context) throws DataSourceException
    {
        int objectid;
        try {
            String id = (String) params.get("id");
            if (id != null) {
                objectid = Integer.parseInt(id);
            } else {
                throw new DataSourceException("source=type:objectid implies the presence of an id argument");
            }
        } catch (NumberFormatException e) {
            throw new DataSourceException(e);
        }

        BaseObject xobj;
        try {
            List list =
                context.getWiki().getStore().search(
                    "from " + BaseObject.class.getName() + " as obj where obj.id='" + objectid + "'", 0, 0, context);
            if (list.size() == 0) {
                throw new DataSourceException("Object ID not found");
            }
            xobj = (BaseObject) list.get(0);
            List propertyList =
                context.getWiki().getStore().search(
                    "from " + BaseProperty.class.getName() + " as p where p.id.id='" + objectid + "'", 0, 0, context);
            Iterator it = propertyList.iterator();
            while (it.hasNext()) {
                BaseProperty prop = (BaseProperty) it.next();
                xobj.addField(prop.getName(), prop);
            }
        } catch (XWikiException e) {
            throw new DataSourceException(e);
        }

        String xclass = xobj.getClassName();
        if (!xclass.startsWith("XWiki.")) {
            throw new DataSourceException("XWiki prefix missing in object class name " + xclass);
        }

        String className = DataSource.class.getPackage().getName() + "." + xclass.substring("XWiki.".length());

        try {
            Class class_ = Class.forName(className);
            Constructor ctor = class_.getConstructor(new Class[] {BaseObject.class, XWikiContext.class});
            return (DataSource) ctor.newInstance(new Object[] {xobj, context});
        } catch (InvocationTargetException e) {
            throw new DataSourceException(e.getTargetException());
        } catch (Exception e) {
            throw new DataSourceException(e);
        }
    }
}
