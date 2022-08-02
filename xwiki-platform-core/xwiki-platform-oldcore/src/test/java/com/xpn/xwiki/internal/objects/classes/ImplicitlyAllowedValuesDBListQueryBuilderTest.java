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
package com.xpn.xwiki.internal.objects.classes;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.DBTreeListClass;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImplicitlyAllowedValuesDBListQueryBuilder}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class ImplicitlyAllowedValuesDBListQueryBuilderTest
{
    @Rule
    public MockitoComponentMockingRule<QueryBuilder<DBListClass>> mocker =
        new MockitoComponentMockingRule<QueryBuilder<DBListClass>>(ImplicitlyAllowedValuesDBListQueryBuilder.class);

    private QueryManager queryManager;

    private QueryFilter viewableValueFilter;

    private DBListClass dbListClass = new DBListClass();

    @Before
    public void configure() throws Exception
    {
        this.queryManager = this.mocker.getInstance(QueryManager.class);
        this.viewableValueFilter = this.mocker.getInstance(QueryFilter.class, "viewableAllowedDBListPropertyValue");

        XWikiDocument ownerDocument = mock(XWikiDocument.class);
        when(ownerDocument.getDocumentReference()).thenReturn(new DocumentReference("tests", "Some", "Page"));
        this.dbListClass.setOwnerDocument(ownerDocument);
    }

    private Query assertQuery(String statement) throws Exception
    {
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(statement, Query.HQL)).thenReturn(query);

        assertSame(query, this.mocker.getComponentUnderTest().build(this.dbListClass));
        return query;
    }

    @Test
    public void buildDefaultQuery() throws Exception
    {
        Query query = assertQuery("select doc.name from XWikiDocument doc where 1 = 0");

        verify(query).setWiki("tests");
        verify(query).addFilter(this.viewableValueFilter);
    }

    @Test
    public void buildWithClassName() throws Exception
    {
        this.dbListClass.setClassname("Blog.CategoryClass");

        Query query = assertQuery("select distinct doc.fullName from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        verify(query).bindValue("className", "Blog.CategoryClass");
        verify(query).bindValue("templateName", "Blog.CategoryTemplate");
    }

    @Test
    public void buildWithId() throws Exception
    {
        this.dbListClass.setIdField("doc.name");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name from XWikiDocument as doc");

        this.dbListClass.setIdField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj " + "where doc.fullName = obj.name");

        this.dbListClass.setIdField("property");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.property from XWikiDocument as doc");
    }

    @Test
    public void buildWithValue() throws Exception
    {
        this.dbListClass.setValueField("doc.name");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name from XWikiDocument as doc");

        this.dbListClass.setValueField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj " + "where doc.fullName = obj.name");

        this.dbListClass.setValueField("property");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.property from XWikiDocument as doc");
    }

    @Test
    public void buildWithIdAndClassName() throws Exception
    {
        this.dbListClass.setClassname("XWiki.XWikiUsers");
        this.dbListClass.setIdField("doc.name");
        Query query = assertQuery(
            "select distinct doc.fullName as unfilterable0, doc.name " + "from XWikiDocument as doc, BaseObject as obj "
                + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");
        verify(query).bindValue("className", "XWiki.XWikiUsers");
        verify(query).bindValue("templateName", "XWiki.XWikiUsersTemplate");

        this.dbListClass.setIdField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        this.dbListClass.setIdField("property");
        query = assertQuery("select distinct doc.fullName as unfilterable0, idProp.value "
            + "from XWikiDocument as doc, BaseObject as obj, StringProperty as idProp "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName"
            + " and obj.id = idProp.id.id and idProp.id.name = :idProp");
        verify(query).bindValue("idProp", "property");
    }

    @Test
    public void buildWithIdAndValue() throws Exception
    {
        this.dbListClass.setIdField("doc.name");
        this.dbListClass.setValueField("doc.name");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name from XWikiDocument as doc");

        this.dbListClass.setValueField("doc.creator");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name, doc.creator from XWikiDocument as doc");

        this.dbListClass.setValueField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj where doc.fullName = obj.name");

        this.dbListClass.setValueField("property");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name, doc.property from XWikiDocument as doc");

        this.dbListClass.setIdField("obj.className");
        this.dbListClass.setValueField("doc.name");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className, doc.name "
            + "from XWikiDocument as doc, BaseObject as obj where doc.fullName = obj.name");

        this.dbListClass.setValueField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj where doc.fullName = obj.name");

        this.dbListClass.setValueField("obj.id");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className, obj.id "
            + "from XWikiDocument as doc, BaseObject as obj where doc.fullName = obj.name");

        this.dbListClass.setValueField("property");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className, doc.property "
            + "from XWikiDocument as doc, BaseObject as obj where doc.fullName = obj.name");

        this.dbListClass.setIdField("property");
        this.dbListClass.setValueField("doc.name");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.property, doc.name from XWikiDocument as doc");

        this.dbListClass.setValueField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.property, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj where doc.fullName = obj.name");

        this.dbListClass.setValueField("property");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.property from XWikiDocument as doc");

        this.dbListClass.setValueField("otherProperty");
        assertQuery(
            "select distinct doc.fullName as unfilterable0, doc.property, doc.otherProperty from XWikiDocument as doc");
    }

    @Test
    public void buildWithIdValueAndClassName() throws Exception
    {
        this.dbListClass.setClassname("XWiki.TagClass");
        this.dbListClass.setIdField("doc.name");
        this.dbListClass.setValueField("doc.name");
        assertQuery(
            "select distinct doc.fullName as unfilterable0, doc.name " + "from XWikiDocument as doc, BaseObject as obj "
                + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        this.dbListClass.setValueField("doc.creator");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name, doc.creator "
            + "from XWikiDocument as doc, BaseObject as obj "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        this.dbListClass.setValueField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        this.dbListClass.setValueField("property");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.name, valueProp.value "
            + "from XWikiDocument as doc, BaseObject as obj, StringProperty as valueProp "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName "
            + "and obj.id = valueProp.id.id and valueProp.id.name = :valueProp");

        this.dbListClass.setIdField("obj.className");
        this.dbListClass.setValueField("doc.name");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className, doc.name "
            + "from XWikiDocument as doc, BaseObject as obj "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        this.dbListClass.setValueField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        this.dbListClass.setValueField("obj.id");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className, obj.id "
            + "from XWikiDocument as doc, BaseObject as obj "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        this.dbListClass.setValueField("property");
        assertQuery("select distinct doc.fullName as unfilterable0, obj.className, valueProp.value "
            + "from XWikiDocument as doc, BaseObject as obj, StringProperty as valueProp "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName "
            + "and obj.id = valueProp.id.id and valueProp.id.name = :valueProp");

        this.dbListClass.setIdField("property");
        this.dbListClass.setValueField("doc.name");
        assertQuery("select distinct doc.fullName as unfilterable0, idProp.value, doc.name "
            + "from XWikiDocument as doc, BaseObject as obj, StringProperty as idProp "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName "
            + "and obj.id = idProp.id.id and idProp.id.name = :idProp");

        this.dbListClass.setValueField("obj.className");
        assertQuery("select distinct doc.fullName as unfilterable0, idProp.value, obj.className "
            + "from XWikiDocument as doc, BaseObject as obj, StringProperty as idProp "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName "
            + "and obj.id = idProp.id.id and idProp.id.name = :idProp");

        this.dbListClass.setValueField("property");
        assertQuery("select distinct doc.fullName as unfilterable0, idProp.value "
            + "from XWikiDocument as doc, BaseObject as obj, StringProperty as idProp "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName "
            + "and obj.id = idProp.id.id and idProp.id.name = :idProp");

        this.dbListClass.setValueField("otherProperty");
        assertQuery("select distinct doc.fullName as unfilterable0, idProp.value, valueProp.value "
            + "from XWikiDocument as doc, BaseObject as obj, StringProperty as idProp, StringProperty as valueProp "
            + "where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName "
            + "and obj.id = idProp.id.id and idProp.id.name = :idProp "
            + "and obj.id = valueProp.id.id and valueProp.id.name = :valueProp");
    }

    @Test
    public void buildWithParent() throws Exception
    {
        DBTreeListClass dbTreeListClass = new DBTreeListClass();
        dbTreeListClass.setOwnerDocument(this.dbListClass.getOwnerDocument());
        dbTreeListClass.setParentField("parent");

        this.dbListClass = dbTreeListClass;
        assertQuery("select doc.name from XWikiDocument doc where 1 = 0");

        this.dbListClass.setIdField("doc.fullName");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.fullName, doc.fullName, doc.parent"
            + " from XWikiDocument as doc");

        this.dbListClass.setIdField("");
        this.dbListClass.setValueField("title");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.title, doc.title, doc.parent"
            + " from XWikiDocument as doc");
        
        this.dbListClass.setIdField("title");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.title, doc.title, doc.parent"
            + " from XWikiDocument as doc");

        this.dbListClass.setIdField("fullName");
        assertQuery("select distinct doc.fullName as unfilterable0, doc.fullName, doc.title, doc.parent"
            + " from XWikiDocument as doc");

        this.dbListClass.setClassname("XWiki.TagClass");
        assertQuery("select distinct doc.fullName as unfilterable0, idProp.value, valueProp.value, parentProp.value"
            + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idProp, StringProperty as valueProp,"
            + " StringProperty as parentProp where doc.fullName = obj.name and obj.className = :className and"
            + " doc.fullName <> :templateName and obj.id = idProp.id.id and idProp.id.name = :idProp and"
            + " obj.id = valueProp.id.id and valueProp.id.name = :valueProp and obj.id = parentProp.id.id and"
            + " parentProp.id.name = :parentProp");
    }
}
