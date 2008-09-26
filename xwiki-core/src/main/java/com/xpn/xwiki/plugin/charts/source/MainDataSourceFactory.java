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
 *
 */
package com.xpn.xwiki.plugin.charts.source;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public class MainDataSourceFactory implements DataSourceFactory
{
    private static DataSourceFactory uniqueInstance = new MainDataSourceFactory();

    private MainDataSourceFactory()
    {
        // empty
    }

    public static DataSourceFactory getInstance()
    {
        return uniqueInstance;
    }

    public DataSource create(Map params, XWikiContext context) throws DataSourceException
    {
        String type = (String) params.get("type");
        if (type == null || "".equals(type)) {
            throw new DataSourceException("Empty datasource type");
        }
        String factoryClassName =
            DataSource.class.getPackage().getName() + "." + Character.toUpperCase(type.charAt(0))
                + type.toLowerCase().substring(1) + "DataSourceFactory";

        try {
            Class class_ = Class.forName(factoryClassName);
            Method method = class_.getMethod("getInstance", new Class[] {});
            DataSourceFactory factory = (DataSourceFactory) method.invoke(null, new Object[] {});
            return factory.create(params, context);
        } catch (InvocationTargetException e) {
            throw new DataSourceException(e.getTargetException());
        } catch (Exception e) {
            throw new DataSourceException(e);
        }
    }
}
