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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.security.SecurityConfiguration;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DBListClass}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class DBListClassTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private QueryBuilder<DBListClass> queryBuilder;

    @MockComponent
    private SecurityConfiguration securityConfiguration;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @BeforeEach
    void before()
    {
        doAnswer(invocation -> invocation.getArgument(0))
            .when(this.oldcore.getSpyXWiki()).parseContent(any(), any(XWikiContext.class));

        this.oldcore.getXWikiContext().setDoc(new XWikiDocument());
    }

    @Test
    void testGetDefaultQueryWhenNoSqlSCriptSpecified()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("", dblc.getSql());
        assertEquals("select doc.name from XWikiDocument doc where 1 = 0",
            dblc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetQueryWithSqlScriptSpecified()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("", dblc.getSql());
        String sql = "select doc.creator from XWikiDocument as doc";
        dblc.setSql(sql);
        assertEquals(sql, dblc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetQueryWithClassSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setClassname("XWiki.XWikiUsers");
        assertEquals(
            "select distinct doc.fullName from XWikiDocument as doc, BaseObject as obj where "
                + "doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetQueryWithIdSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setIdField("doc.name");
        assertEquals("select distinct doc.name from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setIdField("obj.className");
        assertEquals("select distinct obj.className from BaseObject as obj",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setIdField("property");
        assertEquals("select distinct doc.property from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetQueryWithValueSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setValueField("doc.name");
        assertEquals("select distinct doc.name from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct obj.className from BaseObject as obj",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("property");
        assertEquals("select distinct doc.property from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetQueryWithIdAndClassnameSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setClassname("XWiki.XWikiUsers");
        dblc.setIdField("doc.name");
        assertEquals(
            "select distinct doc.name from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setIdField("obj.className");
        assertEquals("select distinct obj.className from BaseObject as obj" + " where obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setIdField("property");
        assertEquals(
            "select distinct idprop.value from BaseObject as obj, StringProperty as idprop"
                + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetQueryWithIdAndValueSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setIdField("doc.name");
        dblc.setValueField("doc.name");
        assertEquals("select distinct doc.name from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("doc.creator");
        assertEquals("select distinct doc.name, doc.creator from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct doc.name, obj.className from XWikiDocument as doc, BaseObject as obj"
            + " where doc.fullName=obj.name", dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("property");
        assertEquals("select distinct doc.name, doc.property from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));

        dblc.setIdField("obj.className");
        dblc.setValueField("doc.name");
        assertEquals(
            "select distinct obj.className, doc.name"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct obj.className from BaseObject as obj",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.id");
        assertEquals("select distinct obj.className, obj.id from BaseObject as obj",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("property");
        assertEquals(
            "select distinct obj.className, doc.property"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dblc.getQuery(this.oldcore.getXWikiContext()));

        dblc.setIdField("property");
        dblc.setValueField("doc.name");
        assertEquals("select distinct doc.property, doc.name from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.className");
        assertEquals(
            "select distinct doc.property, obj.className"
                + " from XWikiDocument as doc, BaseObject as obj where doc.fullName=obj.name",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("property");
        assertEquals("select distinct doc.property from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("otherProperty");
        assertEquals("select distinct doc.property, doc.otherProperty from XWikiDocument as doc",
            dblc.getQuery(this.oldcore.getXWikiContext()));
    }

    @Test
    void testGetQueryWithIdValueAndClassSpecified()
    {
        DBListClass dblc = new DBListClass();
        dblc.setClassname("XWiki.XWikiUsers");
        dblc.setIdField("doc.name");
        dblc.setValueField("doc.name");
        assertEquals(
            "select distinct doc.name from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("doc.creator");
        assertEquals(
            "select distinct doc.name, doc.creator from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.className");
        assertEquals(
            "select distinct doc.name, obj.className from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("property");
        assertEquals(
            "select distinct doc.name, valueprop.value"
                + " from XWikiDocument as doc, BaseObject as obj, StringProperty as valueprop"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
                + " and obj.id=valueprop.id.id and valueprop.id.name='property'",
            dblc.getQuery(this.oldcore.getXWikiContext()));

        dblc.setIdField("obj.className");
        dblc.setValueField("doc.name");
        assertEquals(
            "select distinct obj.className, doc.name" + " from XWikiDocument as doc, BaseObject as obj"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.className");
        assertEquals("select distinct obj.className from BaseObject as obj" + " where obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.id");
        assertEquals(
            "select distinct obj.className, obj.id from BaseObject as obj" + " where obj.className='XWiki.XWikiUsers'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("property");
        assertEquals(
            "select distinct obj.className, valueprop.value" + " from BaseObject as obj, StringProperty as valueprop"
                + " where obj.className='XWiki.XWikiUsers'"
                + " and obj.id=valueprop.id.id and valueprop.id.name='property'",
            dblc.getQuery(this.oldcore.getXWikiContext()));

        dblc.setIdField("property");
        dblc.setValueField("doc.name");
        assertEquals(
            "select distinct idprop.value, doc.name"
                + " from XWikiDocument as doc, BaseObject as obj, StringProperty as idprop"
                + " where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'"
                + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("obj.className");
        assertEquals(
            "select distinct idprop.value, obj.className" + " from BaseObject as obj, StringProperty as idprop"
                + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("property");
        assertEquals(
            "select distinct idprop.value" + " from BaseObject as obj, StringProperty as idprop"
                + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
        dblc.setValueField("otherProperty");
        assertEquals(
            "select distinct idprop.value, valueprop.value"
                + " from BaseObject as obj, StringProperty as idprop, StringProperty as valueprop"
                + " where obj.className='XWiki.XWikiUsers'" + " and obj.id=idprop.id.id and idprop.id.name='property'"
                + " and obj.id=valueprop.id.id and valueprop.id.name='otherProperty'",
            dblc.getQuery(this.oldcore.getXWikiContext()));
    }

    /** Tests that {@link DBListClass#getList} returns values sorted according to the property's sort option. */
    @Test
    void testGetListIsSorted()
    {
        List<ListItem> values = new ArrayList<>(4);
        values.add(new ListItem("a", "A"));
        values.add(new ListItem("c", "D"));
        values.add(new ListItem("d", "C"));
        values.add(new ListItem("b", "B"));
        DBListClass dblc = new DBListClass();
        dblc.setCache(true);
        dblc.setCachedDBList(values, this.oldcore.getXWikiContext());

        assertEquals("[a, c, d, b]",
            dblc.getList(this.oldcore.getXWikiContext()).toString(),
            "Default order was not preserved.");
        dblc.setSort("none");
        assertEquals("[a, c, d, b]",
            dblc.getList(this.oldcore.getXWikiContext()).toString(),
            "Default order was not preserved.");
        dblc.setSort("id");
        assertEquals("[a, b, c, d]",
            dblc.getList(this.oldcore.getXWikiContext()).toString(),
            "Items were not ordered by ID.");
        dblc.setSort("value");
        assertEquals("[a, b, d, c]",
            dblc.getList(this.oldcore.getXWikiContext()).toString(),
            "Items were not ordered by value.");
    }

    /** Tests that {@link DBListClass#getMap} returns values sorted according to the property's sort option. */
    @Test
    void testGetMapIsSorted()
    {
        List<ListItem> values = new ArrayList<>(4);
        values.add(new ListItem("a", "A"));
        values.add(new ListItem("c", "D"));
        values.add(new ListItem("d", "C"));
        values.add(new ListItem("b", "B"));
        DBListClass dblc = new DBListClass();
        dblc.setCache(true);
        dblc.setCachedDBList(values, this.oldcore.getXWikiContext());

        assertEquals("{a=[a, A, ], c=[c, D, ], d=[d, C, ], b=[b, B, ]}",
            dblc.getMap(this.oldcore.getXWikiContext()).toString(),
            "Default order was not preserved.");
        dblc.setSort("none");
        assertEquals("{a=[a, A, ], c=[c, D, ], d=[d, C, ], b=[b, B, ]}",
            dblc.getMap(this.oldcore.getXWikiContext()).toString(),
            "Default order was not preserved.");
        dblc.setSort("id");
        assertEquals("{a=[a, A, ], b=[b, B, ], c=[c, D, ], d=[d, C, ]}",
            dblc.getMap(this.oldcore.getXWikiContext()).toString(),
            "Items were not ordered by ID.");
        dblc.setSort("value");
        assertEquals("{a=[a, A, ], b=[b, B, ], d=[d, C, ], c=[c, D, ]}",
            dblc.getMap(this.oldcore.getXWikiContext()).toString(),
            "Items were not ordered by value.");
    }

    @Test
    void testReturnColWithOneColumn()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("doc.fullName", dblc.returnCol("select doc.fullName from XWikiDocument as doc", true));
        assertEquals("-", dblc.returnCol("select doc.fullName from XWikiDocument as doc", false));
    }

    @Test
    void testReturnColWithOneColumnAndExtraWhitespace()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("doc.fullName", dblc.returnCol("select   doc.fullName   from XWikiDocument as doc", true));
        assertEquals("-", dblc.returnCol("select   doc.fullName   from XWikiDocument as doc", false));
    }

    @Test
    void testReturnColWithOneColumnAndUppercaseTokens()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("doc.fullName", dblc.returnCol("SELECT doc.fullName FROM XWikiDocument as doc", true));
        assertEquals("-", dblc.returnCol("SELECT doc.fullName FROM XWikiDocument as doc", false));
    }

    @Test
    void testReturnColWithTwoColumns()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("doc.fullName", dblc.returnCol("select doc.fullName, doc.title from XWikiDocument as doc", true));
        assertEquals("doc.title", dblc.returnCol("select doc.fullName, doc.title from XWikiDocument as doc", false));
    }

    @Test
    void testReturnColWithTwoColumnsAndExtraWhitespace()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("doc.fullName",
            dblc.returnCol("select   doc.fullName  ,  doc.title  from XWikiDocument as doc", true));
        assertEquals("doc.title",
            dblc.returnCol("select   doc.fullName  ,  doc.title  from XWikiDocument as doc", false));
    }

    @Test
    void testReturnColWithTwoColumnsAndUppercaseTokens()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("doc.fullName", dblc.returnCol("SELECT doc.fullName, doc.title FROM XWikiDocument as doc", true));
        assertEquals("doc.title", dblc.returnCol("SELECT doc.fullName, doc.title FROM XWikiDocument as doc", false));
    }

    @Test
    void testReturnColWithNullQuery()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("-", dblc.returnCol(null, true));
        assertEquals("-", dblc.returnCol(null, false));
    }

    @Test
    void testReturnColWithEmptyQuery()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("-", dblc.returnCol("", true));
        assertEquals("-", dblc.returnCol("", false));
    }

    @Test
    void testReturnColWithInvalidQuery()
    {
        DBListClass dblc = new DBListClass();
        assertEquals("-", dblc.returnCol("do something", true));
        assertEquals("-", dblc.returnCol("do something", false));
    }

    @ParameterizedTest
    @CsvSource({
        "10, 20, false",
        "20, 10, true",
        "0, 0, false",
        "0, -1, false",
        "20, -1, false",
        "0, 20, true"
    })
    void getDBListLimit(int queryLimit, int configuredLimit, boolean setLimit) throws Exception
    {
        Query mockQuery = mock();
        when(mockQuery.getLimit()).thenReturn(queryLimit);
        when(mockQuery.execute()).thenReturn(List.of());
        when(this.securityConfiguration.getQueryItemsLimit()).thenReturn(configuredLimit);

        when(this.authorExecutor.call(any(), any(), any())).then(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return callable.call();
        });

        XWikiDocument ownerDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        DBListClass dbListClass = new DBListClass();
        dbListClass.setOwnerDocument(ownerDocument);

        when(this.queryBuilder.build(dbListClass)).thenReturn(mockQuery);

        dbListClass.getDBList(this.oldcore.getXWikiContext());

        if (setLimit) {
            verify(mockQuery).setLimit(configuredLimit);
        } else {
            verify(mockQuery, never()).setLimit(anyInt());
        }
        verify(mockQuery).execute();
    }
}
