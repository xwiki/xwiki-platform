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
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.doc.XWikiDocument;
import junit.framework.TestCase;

/**
 * Unit tests for {@link com.xpn.xwiki.objects.classes.DBTreeListClass}.
 *
 * @version $Id: $
 */
public class DBTreeListClassTest extends TestCase
{
    private XWikiContext context;

    protected void setUp() throws Exception
    {
        this.context = new XWikiContext();
        this.context.setDoc(new XWikiDocument());
        XWikiHibernateStore store = new XWikiHibernateStore("dummy");
       	XWiki xwiki =  new XWiki(new XWikiConfig(), context);
        xwiki.setStore(store);
    }

    public void testGetQueryWhenNoSQLSCriptSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        assertEquals("select idprop.value, idprop.value, idprop.value from XWikiDocument as doc, "
            + "BaseObject as obj, StringProperty as idprop where doc.fullName=obj.name and obj.className='' and "
            + "obj.id=idprop.id.id and idprop.id.name=''", dbtlc.getQuery(this.context));
    }
}