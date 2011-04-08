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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public class ObjectDataSourceFactory implements DataSourceFactory
{

    private static DataSourceFactory uniqueInstance = new ObjectDataSourceFactory();

    private ObjectDataSourceFactory()
    {
        // empty
    }

    public static DataSourceFactory getInstance()
    {
        return uniqueInstance;
    }

    public DataSource create(Map params, XWikiContext context) throws DataSourceException
    {

        String docName = (String) params.get("doc");
        if (docName == null) {
            throw new DataSourceException("source=type:object implies the presence of a doc argument");
        }

        XWikiDocument doc;
        try {
            doc = context.getWiki().getDocument(docName, context);
        } catch (XWikiException e) {
            throw new DataSourceException(e);
        }

        String className = (String) params.get("class");
        if (className == null) {
            throw new DataSourceException("source=type:object implies the presence of a class argument");
        }

        int number;
        try {
            String s = (String) params.get("object_number");
            if (className != null) {
                try {
                    number = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    throw new DataSourceException(e);
                }
            } else {
                throw new DataSourceException("source=type:object implies the presence of a class argument");
            }
        } catch (NumberFormatException e) {
            throw new DataSourceException(e);
        }

        BaseObject xobj = doc.getObject("XWiki." + className, number);
        if (xobj == null) {
            throw new DataSourceException("XWiki." + className + "#" + number + " object not found");
        }

        try {
            Class class_ = Class.forName(getClass().getPackage().getName() + "." + className);
            Constructor ctor = class_.getConstructor(new Class[] {BaseObject.class, XWikiContext.class});
            return (DataSource) ctor.newInstance(new Object[] {xobj, context});
        } catch (InvocationTargetException e) {
            throw new DataSourceException(e.getTargetException());
        } catch (Exception e) {
            throw new DataSourceException(e);
        }
    }
}
