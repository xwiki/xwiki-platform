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

import java.util.ArrayList;
import java.util.List;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link DBListClass}.
 * 
 * @version $Id$
 */
public class DBListClassTest extends AbstractBridgedXWikiComponentTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();

        getContext().setDoc(new XWikiDocument());

        XWiki xwiki = new XWiki();

        Mock mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {xwiki,
            getContext()});
        xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());

        Mock mockXWikiRenderingEngine = mock(XWikiRenderingEngine.class);
        mockXWikiRenderingEngine.stubs().method("interpretText").will(
            new CustomStub("Implements XWikiRenderingEngine.interpretText")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    return invocation.parameterValues.get(0);
                }
            });

        xwiki.setRenderingEngine((XWikiRenderingEngine) mockXWikiRenderingEngine.proxy());

        getContext().setWiki(xwiki);
    }

    public void testGetDefaultQueryWhenNoSqlSCriptSpecified()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("", dblc.getSql());
        assertEquals("select doc.name from XWikiDocument doc where 1 = 0", dblc.getQuery(getContext()));
    }

    public void testGetQueryWithSqlScriptSpecified()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("", dblc.getSql());
        String sql = "select doc.creator from XWikiDocument as doc";
        dblc.setSql(sql);
        assertEquals(sql, dblc.getQuery(getContext()));
    }

    public void testGetQueryWithClassSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setClassname("XWiki.XWikiUsers");
        assertEquals("select distinct doc.fullName from XWikiDocument as doc, BaseObject as obj where "
            + "doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dblc.getQuery(getContext()));
    }

    public void testGetQueryWithIdSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setIdField("doc.name");
        assertEquals("select distinct doc.name from XWikiDocument as doc", dblc.getQuery(getContext()));
        dblc.setIdField("obj.className");
        assertEquals("select distinct obj.className from BaseObject as obj", dblc.getQuery(getContext()));
        dblc.setIdField("property");
        assertEquals("select distinct doc.property from XWikiDocument as doc", dblc.getQuery(getContext()));
    }

    public void testGetQueryWithValueSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setValueField("doc.name");
        assertEquals("select distinct doc.name from XWikiDocument as doc", dblc.getQuery(getContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct obj.className from BaseObject as obj", dblc.getQuery(getContext()));
        dblc.setValueField("property");
        assertEquals("select distinct doc.property from XWikiDocument as doc", dblc.getQuery(getContext()));
    }

    public void testGetQueryWithIdAndClassnameSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setClassname("XWiki.XWikiUsers");
        dblc.setIdField("doc.name");
        assertEquals("select distinct doc.name from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dblc.getQuery(getContext()));
        dblc.setIdField("obj.className");
        assertEquals(
            "select distinct obj.className from BaseObject as obj" + " where obj.className='XWiki.XWikiUsers'", dblc
                .getQuery(getContext()));
        dblc.setIdField("property");
        assertEquals("select distinct idprop.value from BaseObject as obj, StringProperty as idprop"
            + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dblc.getQuery(getContext()));
    }

    public void testGetQueryWithIdAndValueSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setIdField("doc.name");
        dblc.setValueField("doc.name");
        assertEquals("select distinct doc.name from XWikiDocument as doc", dblc.getQuery(getContext()));
        dblc.setValueField("doc.creator");
        assertEquals("select distinct doc.name, doc.creator from XWikiDocument as doc", dblc.getQuery(getContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct doc.name, obj.className from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name", dblc.getQuery(getContext()));
        dblc.setValueField("property");
        assertEquals("select distinct doc.name, doc.property from XWikiDocument as doc", dblc.getQuery(getContext()));

        dblc.setIdField("obj.className");
        dblc.setValueField("doc.name");
        assertEquals("select distinct obj.className, doc.name"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dblc.getQuery(getContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct obj.className from BaseObject as obj", dblc.getQuery(getContext()));
        dblc.setValueField("obj.id");
        assertEquals("select distinct obj.className, obj.id from BaseObject as obj", dblc.getQuery(getContext()));
        dblc.setValueField("property");
        assertEquals("select distinct obj.className, doc.property"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dblc.getQuery(getContext()));

        dblc.setIdField("property");
        dblc.setValueField("doc.name");
        assertEquals("select distinct doc.property, doc.name from XWikiDocument as doc", dblc.getQuery(getContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct doc.property, obj.className"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dblc.getQuery(getContext()));
        dblc.setValueField("property");
        assertEquals("select distinct doc.property from XWikiDocument as doc", dblc.getQuery(getContext()));
        dblc.setValueField("otherProperty");
        assertEquals("select distinct doc.property, doc.otherProperty from XWikiDocument as doc", dblc
            .getQuery(getContext()));
    }

    public void testGetQueryWithIdValueAndClassSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setClassname("XWiki.XWikiUsers");
        dblc.setIdField("doc.name");
        dblc.setValueField("doc.name");
        assertEquals("select distinct doc.name from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dblc.getQuery(getContext()));
        dblc.setValueField("doc.creator");
        assertEquals("select distinct doc.name, doc.creator from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dblc.getQuery(getContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct doc.name, obj.className from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dblc.getQuery(getContext()));
        dblc.setValueField("property");
        assertEquals("select distinct doc.name, valueprop.value"
            + " from XWikiDocument as doc, BaseObject as obj, StringProperty as valueprop"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
            + " and obj.id=valueprop.id.id and valueprop.id.name='property'", dblc.getQuery(getContext()));

        dblc.setIdField("obj.className");
        dblc.setValueField("doc.name");
        assertEquals("select distinct obj.className, doc.name" + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dblc.getQuery(getContext()));
        dblc.setValueField("obj.className");
        assertEquals(
            "select distinct obj.className from BaseObject as obj" + " where obj.className='XWiki.XWikiUsers'", dblc
                .getQuery(getContext()));
        dblc.setValueField("obj.id");
        assertEquals("select distinct obj.className, obj.id from BaseObject as obj"
            + " where obj.className='XWiki.XWikiUsers'", dblc.getQuery(getContext()));
        dblc.setValueField("property");
        assertEquals("select distinct obj.className, valueprop.value"
            + " from BaseObject as obj, StringProperty as valueprop" + " where obj.className='XWiki.XWikiUsers'"
            + " and obj.id=valueprop.id.id and valueprop.id.name='property'", dblc.getQuery(getContext()));

        dblc.setIdField("property");
        dblc.setValueField("doc.name");
        assertEquals("select distinct idprop.value, doc.name"
            + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idprop"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='property'", dblc.getQuery(getContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct idprop.value, obj.className"
            + " from BaseObject as obj, StringProperty as idprop" + " where obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='property'", dblc.getQuery(getContext()));
        dblc.setValueField("property");
        assertEquals("select distinct idprop.value" + " from BaseObject as obj, StringProperty as idprop"
            + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dblc.getQuery(getContext()));
        dblc.setValueField("otherProperty");
        assertEquals("select distinct idprop.value, valueprop.value"
            + " from BaseObject as obj, StringProperty as idprop, StringProperty as valueprop"
            + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property'"
            + " and obj.id=valueprop.id.id and valueprop.id.name='otherProperty'", dblc.getQuery(getContext()));
    }

    /** Tests that {@link DBListClass#getList} returns values sorted according to the property's sort option. */
    public void testGetListIsSorted()
    {
        List<ListItem> values = new ArrayList<ListItem>(4);
        values.add(new ListItem("a", "A"));
        values.add(new ListItem("c", "D"));
        values.add(new ListItem("d", "C"));
        values.add(new ListItem("b", "B"));
        DBListClass dblc = new DBListClass();
        dblc.setCache(true);
        dblc.setCachedDBList(values, getContext());

        assertEquals("Default order was not preserved.", "[a, c, d, b]", dblc.getList(getContext()).toString());
        dblc.setSort("none");
        assertEquals("Default order was not preserved.", "[a, c, d, b]", dblc.getList(getContext()).toString());
        dblc.setSort("id");
        assertEquals("Items were not ordered by ID.", "[a, b, c, d]", dblc.getList(getContext()).toString());
        dblc.setSort("value");
        assertEquals("Items were not ordered by value.", "[a, b, d, c]", dblc.getList(getContext()).toString());
    }
}
