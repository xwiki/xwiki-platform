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

import org.xwiki.component.manager.ComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for {@link com.xpn.xwiki.objects.classes.DBTreeListClass}.
 * 
 * @version $Id$
 */
public class DBTreeListClassTest extends AbstractXWikiComponentTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
        
        // Statically store the component manager in {@link Utils} to be able to access it without
        // the context.
        // @FIXME : move this initialization in AbstractXWikiComponentTestCase.setUp() when
        // shared-tests will depends on core 1.5 branch
        Utils.setComponentManager((ComponentManager) getContext().get(ComponentManager.class.getName()));

        getContext().setDoc(new XWikiDocument());

        XWikiHibernateStore store = new XWikiHibernateStore("dummy");
        XWiki xwiki = new XWiki(new XWikiConfig(), getContext());
        xwiki.setStore(store);
    }

    public void testGetQueryWhenNoSQLSCriptSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        assertEquals("select doc.name from XWikiDocument doc where 1 = 0", dbtlc.getQuery(getContext()));
    }

    public void testGetQueryWithSqlScriptSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        assertEquals("", dbtlc.getSql());
        String sql = "select doc.name, doc.title, doc.creator from XWikiDocument as doc";
        dbtlc.setSql(sql);
        assertEquals(sql, dbtlc.getQuery(getContext()));
    }

    public void testGetQueryWithClassSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        assertEquals("select distinct doc.fullName, doc.fullName, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
    }

    public void testGetQueryWithClassAndParentSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setParentField("obj.id");
        assertEquals("select distinct doc.fullName, doc.fullName, obj.id"
            + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
    }

    public void testGetQueryWithIdSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setIdField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.parent from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setIdField("obj.className");
        assertEquals("select distinct obj.className, obj.className, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj" + " where doc.fullName=obj.name", dbtlc
            .getQuery(getContext()));
        dbtlc.setIdField("property");
        assertEquals("select distinct doc.property, doc.property, doc.parent" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
    }

    public void testGetQueryWithIdAndParentSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setIdField("doc.name");
        dbtlc.setParentField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.name from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setParentField("obj.className");
        assertEquals("select distinct doc.name, doc.name, obj.className"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setParentField("property");
        assertEquals("select distinct doc.name, doc.name, doc.property from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));

        dbtlc.setIdField("obj.className");
        dbtlc.setParentField("doc.name");
        assertEquals("select distinct obj.className, obj.className, doc.name"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setParentField("obj.className");
        assertEquals("select distinct obj.className, obj.className, obj.className" + " from BaseObject as obj", dbtlc
            .getQuery(getContext()));
        dbtlc.setParentField("property");
        assertEquals("select distinct obj.className, obj.className, doc.property"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));

        dbtlc.setIdField("property");
        dbtlc.setParentField("doc.name");
        assertEquals("select distinct doc.property, doc.property, doc.name" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setParentField("obj.className");
        assertEquals("select distinct doc.property, doc.property, obj.className"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setParentField("property");
        assertEquals("select distinct doc.property, doc.property, doc.property" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setParentField("property2");
        assertEquals("select distinct doc.property, doc.property, doc.property2" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
    }

    public void testGetQueryWithValueSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setValueField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.parent from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
    }

    public void testGetQueryWithIdAndClassnameSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setIdField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.parent" + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("obj.className");
        assertEquals("select distinct obj.className, obj.className, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("property");
        assertEquals("select distinct idprop.value, idprop.value, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idprop"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='property'", dbtlc.getQuery(getContext()));
    }

    public void testGetQueryWithIdParentAndClassnameSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setParentField("doc.name");
        dbtlc.setIdField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.name" + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("obj.className");
        assertEquals("select distinct obj.className, obj.className, doc.name"
            + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("property");
        assertEquals("select distinct idprop.value, idprop.value, doc.name"
            + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idprop"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='property'", dbtlc.getQuery(getContext()));

        dbtlc.setParentField("obj.className");
        dbtlc.setIdField("doc.name");
        assertEquals("select distinct doc.name, doc.name, obj.className"
            + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("obj.className");
        assertEquals("select distinct obj.className, obj.className, obj.className"
            + " from BaseObject as obj where obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("property");
        assertEquals("select distinct idprop.value, idprop.value, obj.className"
            + " from BaseObject as obj, StringProperty as idprop" + " where obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='property'", dbtlc.getQuery(getContext()));

        dbtlc.setParentField("property");
        dbtlc.setIdField("doc.name");
        assertEquals("select distinct doc.name, doc.name, parentprop.value"
            + " from XWikiDocument as doc, BaseObject as obj, StringProperty as parentprop"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
            + " and obj.id=parentprop.id.id and parentprop.id.name='property'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("obj.className");
        assertEquals("select distinct obj.className, obj.className, parentprop.value"
            + " from BaseObject as obj, StringProperty as parentprop" + " where obj.className='XWiki.XWikiUsers'"
            + " and obj.id=parentprop.id.id and parentprop.id.name='property'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("property");
        assertEquals("select distinct idprop.value, idprop.value, idprop.value"
            + " from BaseObject as obj, StringProperty as idprop" + " where obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='property'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("property2");
        assertEquals("select distinct idprop.value, idprop.value, parentprop.value"
            + " from BaseObject as obj, StringProperty as idprop, StringProperty as parentprop"
            + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property2'"
            + " and obj.id=parentprop.id.id and parentprop.id.name='property'", dbtlc.getQuery(getContext()));
    }

    public void testGetQueryWithIdAndValueSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setIdField("doc.name");
        dbtlc.setValueField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.parent" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setValueField("doc.creator");
        assertEquals("select distinct doc.name, doc.creator, doc.parent" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setValueField("obj.className");
        assertEquals("select distinct doc.name, obj.className, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setValueField("property");
        assertEquals("select distinct doc.name, doc.property, doc.parent" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));

        dbtlc.setIdField("obj.className");
        dbtlc.setValueField("doc.name");
        assertEquals("select distinct obj.className, doc.name, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setValueField("obj.className");
        assertEquals("select distinct obj.className, obj.className, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setValueField("obj.id");
        assertEquals("select distinct obj.className, obj.id, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setValueField("property");
        assertEquals("select distinct obj.className, doc.property, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));

        dbtlc.setIdField("property");
        dbtlc.setValueField("doc.name");
        assertEquals("select distinct doc.property, doc.name, doc.parent" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setValueField("obj.className");
        assertEquals("select distinct doc.property, obj.className, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setValueField("property");
        assertEquals("select distinct doc.property, doc.property, doc.parent" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setValueField("otherProperty");
        assertEquals("select distinct doc.property, doc.otherProperty, doc.parent" + " from XWikiDocument as doc",
            dbtlc.getQuery(getContext()));
    }

    public void testGetQueryWithIdValueAndParentSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setIdField("doc.name");
        dbtlc.setValueField("doc.title");
        dbtlc.setParentField("doc.space");
        assertEquals("select distinct doc.name, doc.title, doc.space" + " from XWikiDocument as doc", dbtlc
            .getQuery(getContext()));
        dbtlc.setValueField("obj.name");
        assertEquals("select distinct doc.name, obj.name, doc.space"
            + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("obj.className");
        dbtlc.setParentField("obj.id");
        assertEquals("select distinct obj.className, obj.name, obj.id" + " from BaseObject as obj", dbtlc
            .getQuery(getContext()));
    }

    public void testGetQueryWithIdValueAndClassSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setIdField("doc.name");
        dbtlc.setValueField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.parent" + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setValueField("obj.className");
        assertEquals("select distinct doc.name, obj.className, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setValueField("property");
        assertEquals("select distinct doc.name, valueprop.value, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj, StringProperty as valueprop"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
            + " and obj.id=valueprop.id.id and valueprop.id.name='property'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("property");
        assertEquals("select distinct idprop.value, idprop.value, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idprop"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='property'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("property2");
        assertEquals("select distinct idprop.value, valueprop.value, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj,"
            + " StringProperty as idprop, StringProperty as valueprop"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='property2'"
            + " and obj.id=valueprop.id.id and valueprop.id.name='property'", dbtlc.getQuery(getContext()));
    }

    public void testGetQueryWithIdValueParentAndClassSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setIdField("doc.name");
        dbtlc.setValueField("doc.name");
        dbtlc.setParentField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.name" + " from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", dbtlc.getQuery(getContext()));
        dbtlc.setIdField("prop1");
        dbtlc.setValueField("prop1");
        dbtlc.setParentField("prop1");
        assertEquals("select distinct idprop.value, idprop.value, idprop.value"
            + " from BaseObject as obj, StringProperty as idprop" + " where obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='prop1'", dbtlc.getQuery(getContext()));
        dbtlc.setValueField("prop2");
        assertEquals("select distinct idprop.value, valueprop.value, idprop.value"
            + " from BaseObject as obj, StringProperty as idprop, StringProperty as valueprop"
            + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='prop1'"
            + " and obj.id=valueprop.id.id and valueprop.id.name='prop2'", dbtlc.getQuery(getContext()));
        dbtlc.setParentField("prop2");
        assertEquals("select distinct idprop.value, valueprop.value, valueprop.value"
            + " from BaseObject as obj, StringProperty as idprop, StringProperty as valueprop"
            + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='prop1'"
            + " and obj.id=valueprop.id.id and valueprop.id.name='prop2'", dbtlc.getQuery(getContext()));
        dbtlc.setParentField("prop3");
        assertEquals("select distinct idprop.value, valueprop.value, parentprop.value"
            + " from BaseObject as obj, StringProperty as idprop,"
            + " StringProperty as valueprop, StringProperty as parentprop" + " where obj.className='XWiki.XWikiUsers'"
            + " and obj.id=idprop.id.id and idprop.id.name='prop1'"
            + " and obj.id=valueprop.id.id and valueprop.id.name='prop2'"
            + " and obj.id=parentprop.id.id and parentprop.id.name='prop3'", dbtlc.getQuery(getContext()));
    }
}
