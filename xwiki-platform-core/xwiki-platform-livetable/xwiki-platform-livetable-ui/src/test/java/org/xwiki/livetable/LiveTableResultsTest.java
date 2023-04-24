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
package org.xwiki.livetable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.mail.GeneralMailConfiguration;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.script.MailScriptServiceComponentList;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReferenceComponentList;
import org.xwiki.user.internal.converter.CurrentUserReferenceConverter;
import org.xwiki.user.internal.converter.DocumentUserReferenceConverter;
import org.xwiki.user.internal.converter.GuestUserReferenceConverter;
import org.xwiki.user.internal.converter.SuperAdminUserReferenceConverter;
import org.xwiki.user.script.UserScriptService;
import org.xwiki.velocity.tools.JSONTool;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.plugin.tag.TagPluginApi;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Unit tests for the {@code LiveTableResults} page.
 *
 * @version $Id$
 */
@XWikiSyntax20ComponentList
@SecurityScriptServiceComponentList
@MailScriptServiceComponentList
@UserReferenceComponentList
@ComponentList({
    ModelScriptService.class,
    SuperAdminUserReferenceConverter.class,
    GuestUserReferenceConverter.class,
    DocumentUserReferenceConverter.class,
    CurrentUserReferenceConverter.class,
    DocumentReferenceConverter.class
})
class LiveTableResultsTest extends PageTest
{
    private QueryManagerScriptService queryService;

    private Map<String, Object> results;

    @Mock
    private ScriptQuery query;

    @MockComponent
    private GeneralMailConfiguration generalMailConfiguration;

    @BeforeEach
    @SuppressWarnings("deprecation")
    public void setUp() throws Exception
    {
        // The LiveTableResultsMacros page expects that the HTTP query is done with the "get" action and asking for
        // plain output.
        setOutputSyntax(Syntax.PLAIN_1_0);
        this.request.put("outputSyntax", "plain");
        this.request.put("xpage", "plain");
        this.oldcore.getXWikiContext().setAction("get");

        // Prepare mock Query Service so that tests can control what the DB returns.
        this.queryService = mock(QueryManagerScriptService.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);

        // The LiveTableResultsMacros page uses the tag plugin for the LT tag cloud feature
        TagPluginApi tagPluginApi = mock(TagPluginApi.class);
        doReturn(tagPluginApi).when(this.oldcore.getSpyXWiki()).getPluginApi(eq("tag"), any(XWikiContext.class));

        loadPage(new DocumentReference("xwiki", "XWiki", "LiveTableResultsMacros"));

        // Mock this component as we are not interested in mail sending in this test suite.
        this.componentManager.registerMockComponent(MailSender.class);
        this.componentManager.registerMockComponent(ScriptService.class, "mail.storage");
    }

    @Test
    void plainPageResults() throws Exception
    {
        setColumns("doc.name", "doc.date");
        setSort("doc.date", false);
        setQueryFilters("currentlanguage", "hidden");
        // Offset starting from 1.
        setOffset(13);
        setLimit(7);

        when(this.queryService.hql("  where 1=1    order by doc.date desc")).thenReturn(this.query);
        when(this.query.addFilter("currentlanguage")).thenReturn(this.query);
        when(this.query.addFilter("hidden")).thenReturn(this.query);
        when(this.query.setLimit(7)).thenReturn(this.query);
        // Offset starting from 0.
        when(this.query.setOffset(12)).thenReturn(this.query);
        when(this.query.bindValues(anyMap())).thenReturn(this.query);

        when(this.query.count()).thenReturn(17L);
        when(this.query.execute()).thenReturn(Arrays.asList("A.B", "X.Y"));

        renderPage();

        assertEquals(17L, getTotalRowCount());
        assertEquals(2, getRowCount());
        assertEquals(13, getOffset());

        List<Map<String, Object>> rows = getRows();
        assertEquals(2, rows.size());

        Map<String, Object> ab = rows.get(0);
        assertEquals("A", ab.get("doc_space"));
        assertEquals("B", ab.get("doc_name"));

        Map<String, Object> xy = rows.get(1);
        assertEquals("X", xy.get("doc_space"));
        assertEquals("Y", xy.get("doc_name"));
    }

    /**
     * @see "XWIKI-12803: Class attribute not escaped in Live Tables"
     */
    @Test
    void sqlReservedKeywordAsPropertyName() throws Exception
    {
        setColumns("where");
        setSort("where", true);
        setClassName("My.Class");

        when(this.queryService.hql(any())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);

        renderPage();

        verify(this.queryService).hql(
            ", BaseObject as obj , StringProperty prop_where  "
                + "where obj.name=doc.fullName and obj.className = :className and "
                + "doc.fullName not in (:classTemplate1, :classTemplate2)  "
                + "and obj.id=prop_where.id.id and prop_where.name = :prop_where_name   "
                + "order by lower(prop_where.value) asc, prop_where.value asc");
    }

    /**
     * @see "XWIKI-12855: Unable to sort the Location column in Page Index"
     */
    @Test
    void orderByLocation() throws Exception
    {
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);

        setSort("doc.location", false);

        renderPage();

        verify(this.queryService).hql("  where 1=1    order by lower(doc.fullName) desc, doc.fullName desc");
    }

    /**
     * Verify we can restrict pages by using a location filter and that we can also filter by doc.location
     * at the same time. See <a href="https://jira.xwiki.org/browse/XWIKI-17463">XWIKI-17463</a>.
     */
    @Test
    void restrictLocationAndFilterByDocLocation() throws Exception
    {
        // Simulate the following type of URL:
        // http://localhost:8080/xwiki/bin/get/XWiki/LiveTableResults?outputSyntax=plain&collist=doc.location
        //   &location=Hello&offset=1&limit=15&reqNo=2&doc.location=t&sort=doc.location&dir=asc
        setColumns("doc.location");
        setLocation("Hello");
        setFilter("doc.location", "test");

        when(this.queryService.hql(any(String.class))).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(anyMap())).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);

        renderPage();

        verify(this.queryService).hql("  where 1=1  AND ((doc.name = 'WebHome' AND LOWER(doc.space) LIKE "
            + "LOWER(:locationFilterValue2) ESCAPE '!') OR (doc.name <> 'WebHome' AND LOWER(doc.fullName) LIKE "
            + "LOWER(:locationFilterValue2) ESCAPE '!'))  AND LOWER(doc.fullName) LIKE "
            + "LOWER(:locationFilterValue1) ESCAPE '!'");
        ArgumentCaptor<Map<String, ?>> argument = ArgumentCaptor.forClass(Map.class);
        verify(query).bindValues(argument.capture());
        assertEquals(2, argument.getValue().size());
        assertEquals("%Hello%", argument.getValue().get("locationFilterValue1"));
        assertEquals("%test%", argument.getValue().get("locationFilterValue2"));
    }

    /**
     * Verify the query and its bound values when an empty matcher is used on one of the values. In this test, the
     * filter is applied on a static string list with MultiSelect = false, which can be assimilated to a field of type 
     * String when filtering.
     */
    @Test
    void filterStringEmptyMatcher() throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("xwiki", "Panels", "PanelClass"));
        StaticListClass category = document.getXClass().addStaticListField("category");
        category.setValues("Information|Tools");
        this.xwiki.saveDocument(document, "creates PanelClass", true, this.context);
        
        setColumns("name,description,category");
        setSort("name", true);
        setClassName("Panels.PanelClass");
        setFilter("category_match", "empty");
        setFilter("category", "-");
        setFilter("category_match", "exact");
        setFilter("category", "Information");
        this.request.put("category/join_mode", "OR");

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);

        renderPage();

        verify(this.queryService)
            .hql(", BaseObject as obj , StringProperty as prop_category, StringProperty prop_name  "
                + "where obj.name=doc.fullName and obj.className = :className "
                + "and doc.fullName not in (:classTemplate1, :classTemplate2)  "
                + "and obj.id = prop_category.id.id and prop_category.id.name = :prop_category_id_name "
                + "and ((prop_category.value like :prop_category_value_1 or prop_category.value is null) "
                + "OR prop_category.value = :prop_category_value_2) "
                + "and obj.id=prop_name.id.id and prop_name.name = :prop_name_name   "
                + "order by lower(prop_name.value) asc, prop_name.value asc");
        Map<String, Object> values = new HashMap<>();
        values.put("className", "Panels.PanelClass");
        values.put("classTemplate1", "Panels.PanelClassTemplate");
        values.put("classTemplate2", "Panels.PanelTemplate");
        values.put("prop_category_id_name", "category");
        values.put("prop_category_value_1", "");
        values.put("prop_category_value_2", "Information");
        values.put("prop_name_name", "name");
        verify(this.query).bindValues(values);
    }

    /**
     * Verify the query and its bound values when an empty matcher is used on one of the values. In this test, the
     * filter is applied on a static string list with MultiSelect = true.
     */
    @Test
    void filterStringListEmptyMatcher() throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("xwiki", "Panels", "PanelClass"));
        StaticListClass category = document.getXClass().addStaticListField("category");
        category.setValues("Information|Tools");
        category.setMultiSelect(true);
        this.xwiki.saveDocument(document, "creates PanelClass", true, this.context);

        setColumns("name,description,category");
        setSort("name", true);
        setClassName("Panels.PanelClass");
        setFilter("category_match", "empty");
        setFilter("category", "-");
        setFilter("category_match", "exact");
        setFilter("category", "Information");
        setJoinMode("category", "OR");

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);

        renderPage();

        verify(this.queryService)
            .hql(", BaseObject as obj , StringListProperty as prop_category, StringProperty prop_name  "
                + "where obj.name=doc.fullName "
                + "and obj.className = :className "
                + "and doc.fullName not in (:classTemplate1, :classTemplate2)  "
                + "and obj.id = prop_category.id.id "
                + "and prop_category.id.name = :prop_category_id_name "
                + "and ("
                + "upper(concat('|', concat(prop_category.textValue, '|'))) like upper(:prop_category_textValue_1) "
                + "OR upper(concat('|', concat(prop_category.textValue, '|'))) like upper(:prop_category_textValue_2)"
                + ") "
                + "and obj.id=prop_name.id.id and prop_name.name = :prop_name_name   "
                + "order by lower(prop_name.value) asc, prop_name.value asc");
        Map<String, Object> values = new HashMap<>();
        values.put("className", "Panels.PanelClass");
        values.put("classTemplate1", "Panels.PanelClassTemplate");
        values.put("classTemplate2", "Panels.PanelTemplate");
        values.put("prop_category_id_name", "category");
        values.put("prop_category_textValue_1", "%||%");
        values.put("prop_category_textValue_2", "%|Information|%");
        values.put("prop_name_name", "name");
        verify(this.query).bindValues(values);
    }

    /**
     * Verify that we can filter String properties by multiple values and that the filter values are grouped by match
     * type (in order to optimize the query).
     */
    @Test
    void filterStringMultipleValues() throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("xwiki", "Panels", "PanelClass"));
        document.getXClass().addStaticListField("category");
        this.xwiki.saveDocument(document, "creates PanelClass", true, this.context);

        setColumns("name,description,category");
        setSort("name", true);
        setClassName("Panels.PanelClass");
        setFilter("category_match", "partial");
        setFilter("category", "a");
        setFilter("category_match", "prefix");
        setFilter("category", "b");
        setFilter("category_match", "exact");
        setFilter("category", "c");
        setFilter("category_match", "exact");
        setFilter("category", "d");
        this.request.put("category/join_mode", "OR");

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);

        renderPage();

        verify(this.queryService)
            .hql(", BaseObject as obj , StringProperty as prop_category, StringProperty prop_name  "
                + "where obj.name=doc.fullName and obj.className = :className "
                + "and doc.fullName not in (:classTemplate1, :classTemplate2)  "
                + "and obj.id = prop_category.id.id and prop_category.id.name = :prop_category_id_name "
                + "and (upper(prop_category.value) like upper(:prop_category_value_1) OR"
                + " upper(prop_category.value) like upper(:prop_category_value_2) OR"
                + " prop_category.value in (:prop_category_value_3, :prop_category_value_4)) "
                + "and obj.id=prop_name.id.id and prop_name.name = :prop_name_name   "
                + "order by lower(prop_name.value) asc, prop_name.value asc");
        Map<String, Object> values = new HashMap<>();
        values.put("className", "Panels.PanelClass");
        values.put("classTemplate1", "Panels.PanelClassTemplate");
        values.put("classTemplate2", "Panels.PanelTemplate");
        values.put("prop_category_id_name", "category");
        values.put("prop_category_value_1", "%a%");
        values.put("prop_category_value_2", "b%");
        values.put("prop_category_value_3", "c");
        values.put("prop_category_value_4", "d");
        values.put("prop_name_name", "name");
        verify(this.query).bindValues(values);
    }

    /**
     * When no match type is explicitly defined for a matcher on a short text, the matcher must be partial (i.e., the
     * filtering must match on substrings).
     */
    @Test
    void filterStringNoMatcherSpecified() throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("xwiki", "Test", "MyPage"));
        document.getXClass().addTextField("shortText", "Short Text", 10);
        this.xwiki.saveDocument(document, "creates my page", true, this.context);
        setColumns("shortText");
        setClassName("Test.MyPage");
        setFilter("shortText", "X");

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);

        renderPage();

        verify(this.queryService).hql(", BaseObject as obj , StringProperty as prop_shortText  "
            + "where obj.name=doc.fullName "
            + "and obj.className = :className "
            + "and doc.fullName not in (:classTemplate1, :classTemplate2)  "
            + "and obj.id = prop_shortText.id.id "
            + "and prop_shortText.id.name = :prop_shortText_id_name "
            + "and (upper(prop_shortText.value) like upper(:prop_shortText_value_1)) ");

        Map<String, Object> values = new HashMap<>();
        values.put("className", "Test.MyPage");
        values.put("classTemplate1", "Test.MyPageTemplate");
        values.put("classTemplate2", "Test.MyPage");
        values.put("prop_shortText_id_name", "shortText");
        values.put("prop_shortText_value_1", "%X%");
        verify(this.query).bindValues(values);
    }

    @Test
    void nonViewableResultsAreObfuscated() throws Exception
    {
        this.request.put("limit", "2");
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(3L);
        when(this.query.execute()).thenReturn(Arrays.asList("XWiki.NotViewable", "XWiki.Viewable"));

        when(this.oldcore.getMockContextualAuthorizationManager()
            .hasAccess(same(Right.VIEW), eq(new DocumentReference("xwiki", "XWiki", "NotViewable")))).thenReturn(false);

        renderPage();

        List<Map<String, Object>> rows = getRows();
        assertEquals(2, rows.size());
        assertEquals(2, getRowCount());
        Map<String, Object> obfuscated = rows.get(0);
        assertFalse((boolean) obfuscated.get("doc_viewable"));
        assertEquals("obfuscated", obfuscated.get("doc_fullName"));

        Map<String, Object> viewable = rows.get(1);
        assertTrue((boolean) viewable.get("doc_viewable"));
        assertEquals("XWiki.Viewable", viewable.get("doc_fullName"));
    }

    @Test
    void removeObfuscatedResultsWhenTotalrowsLowerThanLimit() throws Exception
    {
        this.request.put("limit", "2");
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(2L);
        when(this.query.execute()).thenReturn(Arrays.asList("XWiki.NotViewable", "XWiki.Viewable"));

        when(this.oldcore.getMockContextualAuthorizationManager()
            .hasAccess(same(Right.VIEW), eq(new DocumentReference("xwiki", "XWiki", "NotViewable")))).thenReturn(false);

        renderPage();

        List<Map<String, Object>> rows = getRows();
        assertEquals(1, rows.size());
        assertEquals(1, getRowCount());

        Map<String, Object> viewable = rows.get(0);
        assertTrue((boolean) viewable.get("doc_viewable"));
        assertEquals("XWiki.Viewable", viewable.get("doc_fullName"));
    }

    @Test
    void removeObfuscatedResultsWhenLimitIs0() throws Exception
    {
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);
        when(this.query.execute()).thenReturn(Arrays.asList("XWiki.NotViewable"));

        when(this.oldcore.getMockContextualAuthorizationManager()
            .hasAccess(same(Right.VIEW), eq(new DocumentReference("xwiki", "XWiki", "NotViewable")))).thenReturn(false);

        this.request.put("limit", "0");
        this.request.put("classname", "");
        this.request.put("collist", "doc.title,doc.location,doc.content");
        this.request.put("doc.title", "Sandbo");
        this.request.put("doc.location", "Sandbox.TestPage3");
        this.request.put("doc.content", "dummy");
        this.request.put("limit", "0");

        renderPage();

        assertEquals(0, getTotalRowCount());
        assertEquals(0, getRowCount());
        assertEquals(1, getOffset());
        assertEquals(emptyList(), getRows());
    }

    @Test
    void cleanupAccessToPasswordFields() throws Exception
    {
        // Initialize an XClass with a password field.
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "MyClass");
        XWikiDocument xwikiDocument = this.xwiki.getDocument(documentReference, this.context);
        BaseClass xClass = xwikiDocument.getXClass();
        xClass.addPasswordField("password", "Password", 30);
        this.xwiki.saveDocument(xwikiDocument, this.context);

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(0L);
        when(this.query.execute()).thenReturn(Collections.emptyList());

        this.request.put("classname", "XWiki.MyClass");
        this.request.put("password", "abcd");
        this.request.put("collist", "password");

        renderPage();

        verify(this.queryService).hql(", BaseObject as obj   "
            + "where obj.name=doc.fullName "
            + "and obj.className = :className "
            + "and doc.fullName not in (:classTemplate1, :classTemplate2)  ");
    }

    private static Stream<Arguments> provideObfuscateEmails()
    {
        return Stream.of(
            Arguments.of(
                true,
                ", BaseObject as obj , StringProperty as prop_name  "
                    + "where obj.name=doc.fullName "
                    + "and obj.className = :className "
                    + "and doc.fullName not in (:classTemplate1, :classTemplate2)  "
                    + "and obj.id = prop_name.id.id "
                    + "and prop_name.id.name = :prop_name_id_name "
                    + "and (upper(prop_name.value) like upper(:prop_name_value_1)) ",
                Map.of(
                    "className", "Space.MyClass",
                    "classTemplate1", "Space.MyClassTemplate",
                    "classTemplate2", "Space.MyTemplate",
                    "prop_name_id_name", "name",
                    "prop_name_value_1", "%filtername%"
                ),
                "t...@mail.com",
                "t...@mail.com"
            ),
            Arguments.of(
                false,
                ", BaseObject as obj , StringProperty as prop_mail, StringProperty as prop_name  "
                    + "where obj.name=doc.fullName "
                    + "and obj.className = :className "
                    + "and doc.fullName not in (:classTemplate1, :classTemplate2)  "
                    + "and obj.id = prop_mail.id.id "
                    + "and prop_mail.id.name = :prop_mail_id_name "
                    + "and (upper(prop_mail.value) like upper(:prop_mail_value_1)) "
                    + "and obj.id = prop_name.id.id "
                    + "and prop_name.id.name = :prop_name_id_name "
                    + "and (upper(prop_name.value) like upper(:prop_name_value_1)) ",
                Map.of(
                    "className", "Space.MyClass",
                    "classTemplate1", "Space.MyClassTemplate",
                    "classTemplate2", "Space.MyTemplate",
                    "prop_mail_id_name", "mail",
                    "prop_mail_value_1", "%filtermail%",
                    "prop_name_id_name", "name",
                    "prop_name_value_1", "%filtername%"),
                "<a href=\"mailto:test@mail.com\">test@mail.com</a>",
                "test@mail.com"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("provideObfuscateEmails")
    void obfuscateEmails(boolean obfuscate, String expectedHql, Map<String, Object> expectedBindValues,
        String expectedMail, String expectedMailValue) throws Exception
    {
        // TODO: We mock the mail configuration as it relies on document that are loaded through xar files from external
        //  modules, which is currently not possible (would it be loaded using a mandatory document initializer, it 
        //  would work though).
        when(this.generalMailConfiguration.shouldObfuscate()).thenReturn(obfuscate);

        DocumentReference myClassReference = new DocumentReference("xwiki", "Space", "MyClass");
        XWikiDocument xClassDocument = new XWikiDocument(myClassReference);
        xClassDocument.getXClass().addEmailField("mail", "Email", 100);
        xClassDocument.getXClass().addTextField("name", "Name", 100);
        this.xwiki.saveDocument(xClassDocument, this.context);

        XWikiDocument xObjectDocument = new XWikiDocument(new DocumentReference("xwiki", "Space", "MyObject"));
        xObjectDocument.setSyntax(XWIKI_2_1);
        BaseObject baseObject = xObjectDocument.newXObject(myClassReference, this.context);
        baseObject.set("mail", "test@mail.com", this.context);
        baseObject.set("name", "testName", this.context);
        this.xwiki.saveDocument(xObjectDocument, this.context);

        setColumns("mail,name");
        setClassName("Space.MyClass");
        setFilter("mail", "filtermail");
        setFilter("name", "filtername");

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);
        when(this.query.execute()).thenReturn(singletonList("Space.MyObject"));

        renderPage();

        List<Map<String, Object>> rows = getRows();
        assertEquals(expectedMail, StringUtils.trim((String) rows.get(0).get("mail")));
        assertEquals(expectedMailValue, rows.get(0).get("mail_value"));

        verify(this.queryService).hql(expectedHql);
        verify(this.query).bindValues(expectedBindValues);
    }

    public static Stream<Arguments> provideObfuscateEmailsSort()
    {
        return Stream.of(
            Arguments.of(
                true,
                ", BaseObject as obj   "
                    + "where obj.name=doc.fullName "
                    + "and obj.className = :className "
                    + "and doc.fullName not in (:classTemplate1, :classTemplate2)  ",
                Map.of(
                    "className", "Space.MyClass",
                    "classTemplate1", "Space.MyClassTemplate",
                    "classTemplate2", "Space.MyTemplate"
                ),
                "t...@mail.com",
                "t...@mail.com"
            ),
            Arguments.of(
                false,
                ", BaseObject as obj , StringProperty prop_email  "
                    + "where obj.name=doc.fullName "
                    + "and obj.className = :className "
                    + "and doc.fullName not in (:classTemplate1, :classTemplate2)  "
                    + "and obj.id=prop_email.id.id "
                    + "and prop_email.name = :prop_email_name   "
                    + "order by lower(prop_email.value) asc, prop_email.value asc",
                Map.of(
                    "className", "Space.MyClass",
                    "classTemplate1", "Space.MyClassTemplate",
                    "classTemplate2", "Space.MyTemplate",
                    "prop_email_name", "email"
                ),
                "<a href=\"mailto:test@mail.com\">test@mail.com</a>",
                "test@mail.com"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("provideObfuscateEmailsSort")
    void obfuscateEmailsSort(boolean obfuscate, String expectedHql, Map<String, Object> expectedBindValues,
        String expectedMail, String expectedMailValue) throws Exception
    {
        // TODO: We mock the mail configuration as it relies on document that are loaded through xar files from external
        //  modules, which is currently not possible (would it be loaded using a mandatory document initializer, it 
        //  would work though).
        when(this.generalMailConfiguration.shouldObfuscate()).thenReturn(obfuscate);

        DocumentReference myClassReference = new DocumentReference("xwiki", "Space", "MyClass");
        XWikiDocument xClassDocument = new XWikiDocument(myClassReference);
        xClassDocument.getXClass().addEmailField("mail", "Email", 100);
        xClassDocument.getXClass().addTextField("name", "Name", 100);
        this.xwiki.saveDocument(xClassDocument, this.context);

        XWikiDocument xObjectDocument = new XWikiDocument(new DocumentReference("xwiki", "Space", "MyObject"));
        xObjectDocument.setSyntax(XWIKI_2_1);
        BaseObject baseObject = xObjectDocument.newXObject(myClassReference, this.context);
        baseObject.set("mail", "test@mail.com", this.context);
        baseObject.set("name", "testName", this.context);
        this.xwiki.saveDocument(xObjectDocument, this.context);

        setColumns("mail,name");
        setClassName("Space.MyClass");
        setSort("email", true);

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);
        when(this.query.execute()).thenReturn(singletonList("Space.MyObject"));

        renderPage();

        List<Map<String, Object>> rows = getRows();
        assertEquals(expectedMail, StringUtils.trim((String) rows.get(0).get("mail")));
        assertEquals(expectedMailValue, rows.get(0).get("mail_value"));

        verify(this.queryService).hql(expectedHql);
        verify(this.query).bindValues(expectedBindValues);
    }

    /**
     * Verify that the author is correctly displayed when the last author is superadmin.
     */
    @Test
    void authorSuperAdmin() throws Exception
    {
        UserScriptService userScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "user", UserScriptService.class, false);
        when(userScriptService.getGuestUserReference()).thenReturn(GuestUserReference.INSTANCE);

        DocumentReference myClassReference = new DocumentReference("xwiki", "Space", "MyClass");
        XWikiDocument xClassDocument = new XWikiDocument(myClassReference);
        this.xwiki.saveDocument(xClassDocument, this.context);

        XWikiDocument xObjectDocument = new XWikiDocument(new DocumentReference("xwiki", "Space", "MyObject"));
        xObjectDocument.newXObject(myClassReference, this.context);
        xObjectDocument.getAuthors().setOriginalMetadataAuthor(SuperAdminUserReference.INSTANCE);
        this.xwiki.saveDocument(xObjectDocument, this.context);

        setColumns("doc.name");
        setClassName("Space.MyClass");

        DocumentReference superAdminReference = new DocumentReference("xwiki", "XWiki", "superadmin");
        when(this.xwiki.getPlainUserName(superAdminReference, this.context)).thenReturn("SuperAdmin");

        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(1L);
        when(this.query.execute()).thenReturn(singletonList("Space.MyObject"));

        renderPage();

        List<Map<String, Object>> rows = getRows();
        assertEquals("SuperAdmin", rows.get(0).get("doc_author"));
        assertEquals("/xwiki/bin/view/XWiki/superadmin", rows.get(0).get("doc_author_url"));
    }

    //
    // Helper methods
    //

    @SuppressWarnings("unchecked")
    private String renderPage() throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);

        String output = renderPage(new DocumentReference("xwiki", "XWiki", "LiveTableResults"));

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(jsonTool).serialize(argument.capture());

        this.results = (Map<String, Object>) argument.getValue();

        return output;
    }

    private void setClassName(String className)
    {
        this.request.put("classname", className);
    }

    private void setColumns(String... columns)
    {
        this.request.put("collist", StringUtils.join(columns, ','));
    }

    private void setLocation(String location)
    {
        this.request.put("location", location);
    }

    private void setOffset(int offset)
    {
        this.request.put("offset", String.valueOf(offset));
    }

    private void setLimit(int limit)
    {
        this.request.put("limit", String.valueOf(limit));
    }

    private void setSort(String column, Boolean ascending)
    {
        this.request.put("sort", column);
        if (ascending != null) {
            this.request.put("dir", ascending ? "asc" : "desc");
        }
    }

    private void setFilter(String column, String value)
    {
        this.request.put(column, value);
    }

    private void setJoinMode(String column, String joinMode)
    {
        this.request.put(column + "/join_mode", joinMode);
    }

    private void setQueryFilters(String... filters)
    {
        this.request.put("queryFilters", StringUtils.join(filters, ','));
    }

    private Object getTotalRowCount()
    {
        return this.results.get("totalrows");
    }

    private Object getRowCount()
    {
        return this.results.get("returnedrows");
    }

    private Object getOffset()
    {
        return this.results.get("offset");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRows()
    {
        return (List<Map<String, Object>>) this.results.get("rows");
    }
}
