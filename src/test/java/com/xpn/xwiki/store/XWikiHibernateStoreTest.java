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
package com.xpn.xwiki.store;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link XWikiHibernateStore} class.
 *
 * @version $Id: $
 */
public class XWikiHibernateStoreTest extends TestCase
{
    public void testGetColumnsForSelectStatement()
    {
        XWikiHibernateStore store = new XWikiHibernateStore("whatever");
        assertEquals(", doc.date",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date desc"));
        assertEquals(", doc.date",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date asc"));
        assertEquals(", doc.date",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date"));
        assertEquals(", doc.date, doc.name",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date, doc.name"));
        assertEquals(", doc.date, doc.name",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date ASC, doc.name DESC"));
        assertEquals("",
            store.getColumnsForSelectStatement(", BaseObject as obj where obj.name=doc.fullName"));
    }

    public void testCreateSQLQuery()
    {
        XWikiHibernateStore store = new XWikiHibernateStore("whatever");
        assertEquals("select distinct doc.web, doc.name from XWikiDocument as doc",
            store.createSQLQuery("select distinct doc.web, doc.name", ""));
        assertEquals("select distinct doc.web, doc.name, doc.date from XWikiDocument as doc "
            + "where 1=1 order by doc.date desc", store.createSQLQuery(
            "select distinct doc.web, doc.name", "where 1=1 order by doc.date desc"));
    }
}
