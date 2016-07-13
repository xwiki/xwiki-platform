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
package com.xpn.xwiki.objects.classes;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Unit tests for {@link com.xpn.xwiki.objects.classes.DBTreeListClass}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
public class DBTreeListClassTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    @Before
    public void before() throws Exception
    {
        doAnswer(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgumentAt(0, String.class);
            }
        }).when(this.oldcore.getSpyXWiki()).parseContent(anyString(), any(XWikiContext.class));

        this.oldcore.getXWikiContext().setDoc(new XWikiDocument());
    }

    @Test
    public void testGetQueryWhenNoSQLSCriptSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        assertEquals("select doc.name from XWikiDocument doc where 1 = 0",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithSqlScriptSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        assertEquals("", dbtlc.getSql());
        String sql = "select doc.name, doc.title, doc.creator from XWikiDocument as doc";
        dbtlc.setSql(sql);
        assertEquals(sql, dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithClassSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        assertEquals(
            "select distinct doc.fullName, doc.fullName, doc.parent" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithClassAndParentSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setParentField("obj.id");
        assertEquals(
            "select distinct doc.fullName, doc.fullName, obj.id" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithIdSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setIdField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.parent from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("obj.className");
        assertEquals("select distinct obj.className, obj.className, doc.parent"
            + " from XWikiDocument as doc, BaseObject as obj" + " where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("property");
        assertEquals("select distinct doc.property, doc.property, doc.parent" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithIdAndParentSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setIdField("doc.name");
        dbtlc.setParentField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.name from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("obj.className");
        assertEquals(
            "select distinct doc.name, doc.name, obj.className"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("property");
        assertEquals("select distinct doc.name, doc.name, doc.property from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));

        dbtlc.setIdField("obj.className");
        dbtlc.setParentField("doc.name");
        assertEquals(
            "select distinct obj.className, obj.className, doc.name"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("obj.className");
        assertEquals("select distinct obj.className, obj.className, obj.className" + " from BaseObject as obj",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("property");
        assertEquals(
            "select distinct obj.className, obj.className, doc.property"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));

        dbtlc.setIdField("property");
        dbtlc.setParentField("doc.name");
        assertEquals("select distinct doc.property, doc.property, doc.name" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("obj.className");
        assertEquals(
            "select distinct doc.property, doc.property, obj.className"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("property");
        assertEquals("select distinct doc.property, doc.property, doc.property" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("property2");
        assertEquals("select distinct doc.property, doc.property, doc.property2" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithValueSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setValueField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.parent from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithIdAndClassnameSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setIdField("doc.name");
        assertEquals(
            "select distinct doc.name, doc.name, doc.parent" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("obj.className");
        assertEquals(
            "select distinct obj.className, obj.className, doc.parent" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("property");
        assertEquals(
            "select distinct idprop.value, idprop.value, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idprop"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
                + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithIdParentAndClassnameSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setParentField("doc.name");
        dbtlc.setIdField("doc.name");
        assertEquals(
            "select distinct doc.name, doc.name, doc.name" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("obj.className");
        assertEquals(
            "select distinct obj.className, obj.className, doc.name" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("property");
        assertEquals(
            "select distinct idprop.value, idprop.value, doc.name"
                + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idprop"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
                + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));

        dbtlc.setParentField("obj.className");
        dbtlc.setIdField("doc.name");
        assertEquals(
            "select distinct doc.name, doc.name, obj.className" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("obj.className");
        assertEquals(
            "select distinct obj.className, obj.className, obj.className"
                + " from BaseObject as obj where obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("property");
        assertEquals(
            "select distinct idprop.value, idprop.value, obj.className"
                + " from BaseObject as obj, StringProperty as idprop" + " where obj.className='XWiki.XWikiUsers'"
                + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));

        dbtlc.setParentField("property");
        dbtlc.setIdField("doc.name");
        assertEquals(
            "select distinct doc.name, doc.name, parentprop.value"
                + " from XWikiDocument as doc, BaseObject as obj, StringProperty as parentprop"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
                + " and obj.id=parentprop.id.id and parentprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("obj.className");
        assertEquals(
            "select distinct obj.className, obj.className, parentprop.value"
                + " from BaseObject as obj, StringProperty as parentprop" + " where obj.className='XWiki.XWikiUsers'"
                + " and obj.id=parentprop.id.id and parentprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("property");
        assertEquals(
            "select distinct idprop.value, idprop.value, idprop.value"
                + " from BaseObject as obj, StringProperty as idprop" + " where obj.className='XWiki.XWikiUsers'"
                + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("property2");
        assertEquals(
            "select distinct idprop.value, idprop.value, parentprop.value"
                + " from BaseObject as obj, StringProperty as idprop, StringProperty as parentprop"
                + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property2'"
                + " and obj.id=parentprop.id.id and parentprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithIdAndValueSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setIdField("doc.name");
        dbtlc.setValueField("doc.name");
        assertEquals("select distinct doc.name, doc.name, doc.parent" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("doc.creator");
        assertEquals("select distinct doc.name, doc.creator, doc.parent" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("obj.className");
        assertEquals(
            "select distinct doc.name, obj.className, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("property");
        assertEquals("select distinct doc.name, doc.property, doc.parent" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));

        dbtlc.setIdField("obj.className");
        dbtlc.setValueField("doc.name");
        assertEquals(
            "select distinct obj.className, doc.name, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("obj.className");
        assertEquals(
            "select distinct obj.className, obj.className, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("obj.id");
        assertEquals(
            "select distinct obj.className, obj.id, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("property");
        assertEquals(
            "select distinct obj.className, doc.property, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));

        dbtlc.setIdField("property");
        dbtlc.setValueField("doc.name");
        assertEquals("select distinct doc.property, doc.name, doc.parent" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("obj.className");
        assertEquals(
            "select distinct doc.property, obj.className, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("property");
        assertEquals("select distinct doc.property, doc.property, doc.parent" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("otherProperty");
        assertEquals("select distinct doc.property, doc.otherProperty, doc.parent" + " from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithIdValueAndParentSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setIdField("doc.name");
        dbtlc.setValueField("doc.title");
        dbtlc.setParentField("doc.space");
        assertEquals("select distinct doc.name, doc.title, doc.space from XWikiDocument as doc",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("obj.name");
        assertEquals(
            "select distinct doc.name, obj.name, doc.space"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("obj.className");
        dbtlc.setParentField("obj.id");
        assertEquals("select distinct obj.className, obj.name, obj.id from BaseObject as obj",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithIdValueAndClassSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setIdField("doc.name");
        dbtlc.setValueField("doc.name");
        assertEquals(
            "select distinct doc.name, doc.name, doc.parent" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("obj.className");
        assertEquals(
            "select distinct doc.name, obj.className, doc.parent" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("property");
        assertEquals(
            "select distinct doc.name, valueprop.value, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj, StringProperty as valueprop"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
                + " and obj.id=valueprop.id.id and valueprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("property");
        assertEquals(
            "select distinct idprop.value, idprop.value, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idprop"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
                + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("property2");
        assertEquals(
            "select distinct idprop.value, valueprop.value, doc.parent"
                + " from XWikiDocument as doc, BaseObject as obj,"
                + " StringProperty as idprop, StringProperty as valueprop"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
                + " and obj.id=idprop.id.id and idprop.id.name='property2'"
                + " and obj.id=valueprop.id.id and valueprop.id.name='property'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testGetQueryWithIdValueParentAndClassSpecified()
    {
        DBTreeListClass dbtlc = new DBTreeListClass();
        dbtlc.setClassname("XWiki.XWikiUsers");
        dbtlc.setIdField("doc.name");
        dbtlc.setValueField("doc.name");
        dbtlc.setParentField("doc.name");
        assertEquals(
            "select distinct doc.name, doc.name, doc.name" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setIdField("prop1");
        dbtlc.setValueField("prop1");
        dbtlc.setParentField("prop1");
        assertEquals(
            "select distinct idprop.value, idprop.value, idprop.value"
                + " from BaseObject as obj, StringProperty as idprop" + " where obj.className='XWiki.XWikiUsers'"
                + " and obj.id=idprop.id.id and idprop.id.name='prop1'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setValueField("prop2");
        assertEquals(
            "select distinct idprop.value, valueprop.value, idprop.value"
                + " from BaseObject as obj, StringProperty as idprop, StringProperty as valueprop"
                + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='prop1'"
                + " and obj.id=valueprop.id.id and valueprop.id.name='prop2'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("prop2");
        assertEquals(
            "select distinct idprop.value, valueprop.value, valueprop.value"
                + " from BaseObject as obj, StringProperty as idprop, StringProperty as valueprop"
                + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='prop1'"
                + " and obj.id=valueprop.id.id and valueprop.id.name='prop2'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
        dbtlc.setParentField("prop3");
        assertEquals(
            "select distinct idprop.value, valueprop.value, parentprop.value"
                + " from BaseObject as obj, StringProperty as idprop,"
                + " StringProperty as valueprop, StringProperty as parentprop"
                + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='prop1'"
                + " and obj.id=valueprop.id.id and valueprop.id.name='prop2'"
                + " and obj.id=parentprop.id.id and parentprop.id.name='prop3'",
            dbtlc.getQuery(this.oldcore.getXWikiContext()));
    }
}
