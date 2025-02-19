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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.DBTreeListClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImplicitlyAllowedValuesDBListQueryBuilder}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@OldcoreTest
@ReferenceComponentList
@SuppressWarnings("checkstyle:MultipleStringLiterals")
class ImplicitlyAllowedValuesDBListQueryBuilderTest
{
    @InjectMockComponents
    private ImplicitlyAllowedValuesDBListQueryBuilder implicitlyAllowedValuesDBListQueryBuilder;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("viewableAllowedDBListPropertyValue")
    private QueryFilter viewableValueFilter;

    @MockComponent
    private GeneralMailConfiguration mailConfiguration;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private DBListClass dbListClass = new DBListClass();

    @BeforeEach
    void configure()
    {
        XWikiDocument ownerDocument = mock(XWikiDocument.class);
        when(ownerDocument.getDocumentReference()).thenReturn(new DocumentReference("tests", "Some", "Page"));
        this.dbListClass.setOwnerDocument(ownerDocument);
    }

    private Query assertQuery(String statement) throws Exception
    {
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(statement, Query.HQL)).thenReturn(query);

        assertSame(query, this.implicitlyAllowedValuesDBListQueryBuilder.build(this.dbListClass));
        return query;
    }

    @Test
    void buildDefaultQuery() throws Exception
    {
        Query query = assertQuery("select doc.name from XWikiDocument doc where 1 = 0");

        verify(query).setWiki("tests");
        verify(query).addFilter(this.viewableValueFilter);
    }

    @Test
    void buildWithClassName() throws Exception
    {
        this.dbListClass.setClassname("Blog.CategoryClass");

        Query query = assertQuery("select distinct doc.fullName from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName = obj.name and obj.className = :className and doc.fullName <> :templateName");

        verify(query).bindValue("className", "Blog.CategoryClass");
        verify(query).bindValue("templateName", "Blog.CategoryTemplate");
    }

    @Test
    void buildWithId() throws Exception
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
    void buildWithValue() throws Exception
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
    void buildWithIdAndClassName() throws Exception
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
    void buildWithIdAndValue() throws Exception
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
    void buildWithIdValueAndClassName() throws Exception
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
    void buildWithParent() throws Exception
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

    @ParameterizedTest
    @ValueSource(strings = { "doc.invalid, other", "foo, bar", "obj.a, b" })
    void buildWithInvalidId(String field)
    {
        this.dbListClass.setIdField(field);
        QueryException queryException = assertThrows(QueryException.class, () -> assertQuery(null));
        assertEquals("Invalid field name [%s]".formatted(field), queryException.getMessage());
        this.dbListClass.setIdField("");

        this.dbListClass.setValueField(field);
        queryException = assertThrows(QueryException.class, () -> assertQuery(null));
        assertEquals("Invalid field name [%s]".formatted(field), queryException.getMessage());
    }

    @Test
    void buildWithPasswordField() throws Exception
    {
        DocumentReference classReference = new DocumentReference("tests", "Space", "XClass");
        XWikiDocument classDocument =
            this.oldcore.getSpyXWiki().getDocument(classReference, this.oldcore.getXWikiContext());
        String fieldName = "passwordField";
        classDocument.getXClass().addPasswordField(fieldName, "My Password", 10);
        this.oldcore.getSpyXWiki().saveDocument(classDocument, "Add password field", this.oldcore.getXWikiContext());

        this.dbListClass.setIdField(fieldName);
        this.dbListClass.setClassname("Space.XClass");
        QueryException queryException = assertThrows(QueryException.class, () -> assertQuery(null));

        assertEquals("Queries for password field [passwordField] on class [Space.XClass] aren't allowed",
            queryException.getMessage());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void buildWithEmailField(boolean obfuscate) throws Exception
    {
        when(this.mailConfiguration.shouldObfuscate()).thenReturn(obfuscate);

        DocumentReference classReference = new DocumentReference("tests", "Space", "XClass");
        XWikiDocument classDocument =
            this.oldcore.getSpyXWiki().getDocument(classReference, this.oldcore.getXWikiContext());
        String fieldName = "emailField";
        classDocument.getXClass().addEmailField(fieldName, "My Email", 10);
        this.oldcore.getSpyXWiki().saveDocument(classDocument, "Add email field", this.oldcore.getXWikiContext());

        this.dbListClass.setIdField(fieldName);
        this.dbListClass.setClassname("Space.XClass");

        if (obfuscate) {
            QueryException queryException = assertThrows(QueryException.class, () -> assertQuery(null));
            assertEquals(
                "Queries for email property [emailField] on class [Space.XClass] aren't allowed as email"
                    + " obfuscation is enabled.",
                queryException.getMessage());
        } else {
            Query query =
                assertQuery(
                    "select distinct doc.fullName as unfilterable0, idProp.value from XWikiDocument as doc, "
                        + "BaseObject as obj, StringProperty as idProp where doc.fullName = obj.name and "
                        + "obj.className = :className and doc.fullName <> :templateName and obj.id = idProp.id.id "
                        + "and idProp.id.name = :idProp");
            verify(query).bindValue("className", "Space.XClass");
            verify(query).bindValue("templateName", "Space.XTemplate");
            verify(query).bindValue("idProp", fieldName);
        }
    }
}
