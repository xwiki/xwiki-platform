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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.model.internal.reference.converter.EntityReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.script.SecurityAuthorizationScriptService;
import org.xwiki.security.authorization.script.internal.RightConverter;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.velocity.tools.JSONTool;
import org.xwiki.velocity.tools.RegexTool;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.tag.TagPluginApi;

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

/**
 * Unit tests for the {@code LiveTableResults} page.
 * 
 * @version $Id$
 */
@XWikiSyntax20ComponentList
@ComponentList({
    ModelScriptService.class,
    // SecurityScriptService 
    SecurityScriptService.class,
    RightConverter.class,
    EntityReferenceConverter.class,
    SecurityAuthorizationScriptService.class
})
class LiveTableResultsTest extends PageTest
{
    private QueryManagerScriptService queryService;

    private Map<String, Object> results;

    @Mock
    private ScriptQuery query;

    @BeforeEach
    @SuppressWarnings("deprecation")
    public void setUp() throws Exception
    {
        // The LiveTableResults page uses Velocity macros from the macros.vm template. We need to overwrite the Velocity
        // configuration in order to use the ClasspathResourceLoader for loading the macros.vm template from the class
        // path.
        VelocityConfiguration velocityConfiguration = this.oldcore.getMocker().getInstance(VelocityConfiguration.class);
        Properties velocityConfigProps = velocityConfiguration.getProperties();
        velocityConfigProps.put(RuntimeConstants.RESOURCE_LOADERS, "class");
        velocityConfigProps.put(RuntimeConstants.RESOURCE_LOADER + ".class." + RuntimeConstants.RESOURCE_LOADER_CLASS,
            ClasspathResourceLoader.class.getName());
        velocityConfigProps.put(RuntimeConstants.VM_LIBRARY, "/templates/macros.vm");
        velocityConfiguration = this.oldcore.getMocker().registerMockComponent(VelocityConfiguration.class);
        when(velocityConfiguration.getProperties()).thenReturn(velocityConfigProps);

        // The LiveTableResultsMacros page includes the hierarchy_macros.vm template.
        this.oldcore.getMocker().registerMockComponent(TemplateManager.class);

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

        // Register velocity tools used by the LiveTableResultsMacros page
        registerVelocityTool("stringtool", new StringUtils());
        registerVelocityTool("mathtool", new MathTool());
        registerVelocityTool("regextool", new RegexTool());
        registerVelocityTool("numbertool", new NumberTool());
        registerVelocityTool("escapetool", new EscapeTool());

        loadPage(new DocumentReference("xwiki", "XWiki", "LiveTableResultsMacros"));
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
