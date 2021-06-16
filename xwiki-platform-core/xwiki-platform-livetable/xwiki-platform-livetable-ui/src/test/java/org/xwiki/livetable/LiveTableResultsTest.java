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

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.Query;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.velocity.tools.JSONTool;
import org.xwiki.velocity.tools.RegexTool;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.plugin.tag.TagPluginApi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
public class LiveTableResultsTest extends PageTest
{
    private QueryManagerScriptService queryService;

    private ModelScriptService modelService;

    private Map<String, Object> results;

    @Mock
    private ScriptQuery query;

    @BeforeEach
    @SuppressWarnings("deprecation")
    public void setUp() throws Exception
    {
        // The LiveTableResultsMacros page expects that the HTTP query is done with the "get" action and asking for
        // plain output.
        setOutputSyntax(Syntax.PLAIN_1_0);
        request.put("outputSyntax", "plain");
        request.put("xpage", "plain");
        oldcore.getXWikiContext().setAction("get");

        // Prepare mock Query Service so that tests can control what the DB returns.
        queryService = mock(QueryManagerScriptService.class);
        oldcore.getMocker().registerComponent(ScriptService.class, "query", queryService);

        // Prepare mock ModelScriptService so that tests can control what the model returns.
        modelService = mock(ModelScriptService.class);
        oldcore.getMocker().registerComponent(ScriptService.class, "model", modelService);

        // The LiveTableResultsMacros page uses the tag plugin for the LT tag cloud feature
        TagPluginApi tagPluginApi = mock(TagPluginApi.class);
        doReturn(tagPluginApi).when(oldcore.getSpyXWiki()).getPluginApi(eq("tag"), any(XWikiContext.class));

        // Register velocity tools used by the LiveTableResultsMacros page
        registerVelocityTool("stringtool", new StringUtils());
        registerVelocityTool("mathtool", new MathTool());
        registerVelocityTool("regextool", new RegexTool());
        registerVelocityTool("numbertool", new NumberTool());
        registerVelocityTool("escapetool", new EscapeTool());

        loadPage(new DocumentReference("xwiki", "XWiki", "LiveTableResultsMacros"));
    }

    @Test
    public void plainPageResults() throws Exception
    {
        setColumns("doc.name", "doc.date");
        setSort("doc.date", false);
        setQueryFilters("currentlanguage", "hidden");
        // Offset starting from 1.
        setOffset(13);
        setLimit(7);

        when(queryService.hql("  where 1=1    order by doc.date desc")).thenReturn(this.query);
        when(this.query.addFilter("currentlanguage")).thenReturn(this.query);
        when(this.query.addFilter("hidden")).thenReturn(this.query);
        when(this.query.setLimit(7)).thenReturn(this.query);
        // Offset starting from 0.
        when(this.query.setOffset(12)).thenReturn(this.query);
        when(this.query.bindValues(anyMap())).thenReturn(this.query);

        when(this.query.count()).thenReturn(17L);
        when(this.query.execute()).thenReturn(Arrays.asList("A.B", "X.Y"));

        DocumentReference abReference = new DocumentReference("wiki", "A", "B");
        when(modelService.resolveDocument("A.B")).thenReturn(abReference);
        when(modelService.serialize(abReference.getParent(), "local")).thenReturn("A");

        DocumentReference xyReference = new DocumentReference("wiki", "X", "Y");
        when(modelService.resolveDocument("X.Y")).thenReturn(xyReference);
        when(modelService.serialize(xyReference.getParent(), "local")).thenReturn("X");

        renderPage();

        assertEquals(17L, getTotalRowCount());
        assertEquals(2, getRowCount());
        assertEquals(13, getOffset());

        List<Map<String, String>> rows = getRows();
        assertEquals(2, rows.size());

        Map<String, String> ab = rows.get(0);
        assertEquals("A", ab.get("doc_space"));
        assertEquals("B", ab.get("doc_name"));

        Map<String, String> xy = rows.get(1);
        assertEquals("X", xy.get("doc_space"));
        assertEquals("Y", xy.get("doc_name"));
    }

    /**
     * @see "XWIKI-12803: Class attribute not escaped in Live Tables"
     */
    @Test
    public void sqlReservedKeywordAsPropertyName() throws Exception
    {
        setColumns("where");
        setSort("where", true);
        setClassName("My.Class");

        renderPage();

        verify(queryService).hql(
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
    public void orderByLocation() throws Exception
    {
        setSort("doc.location", false);

        renderPage();

        verify(queryService).hql("  where 1=1    order by lower(doc.fullName) desc, doc.fullName desc");
    }

    /**
     * Verify we can restrict pages by using a location filter and that we can also filter by doc.location
     * at the same time. See <a href="https://jira.xwiki.org/browse/XWIKI-17463">XWIKI-17463</a>.
     */
    @Test
    public void restrictLocationAndFilterByDocLocation() throws Exception
    {
        // Simulate the following type of URL:
        // http://localhost:8080/xwiki/bin/get/XWiki/LiveTableResults?outputSyntax=plain&collist=doc.location
        //   &location=Hello&offset=1&limit=15&reqNo=2&doc.location=t&sort=doc.location&dir=asc
        setColumns("doc.location");
        setLocation("Hello");
        setFilter("doc.location", "test");

        Query query = mock(Query.class);
        when(queryService.hql(any(String.class))).thenReturn(query);
        when(query.setLimit(anyInt())).thenReturn(query);
        when(query.setOffset(anyInt())).thenReturn(query);
        when(query.bindValues(anyMap())).thenReturn(query);

        renderPage();

        verify(queryService).hql("  where 1=1  AND ((doc.name = 'WebHome' AND LOWER(doc.space) LIKE "
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

        renderPage();

        verify(this.queryService)
            .hql(", BaseObject as obj , StringProperty as prop_category, StringProperty prop_name  "
                + "where obj.name=doc.fullName and obj.className = :className "
                + "and doc.fullName not in (:classTemplate1, :classTemplate2)  "
                + "and obj.id = prop_category.id.id and prop_category.id.name = :prop_category_id_name "
                + "and (prop_category.value in (:prop_category_value_1, :prop_category_value_2)) "
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
        request.put("classname", className);
    }

    private void setColumns(String... columns)
    {
        request.put("collist", StringUtils.join(columns, ','));
    }

    private void setLocation(String location)
    {
        request.put("location", location);
    }

    private void setOffset(int offset)
    {
        request.put("offset", String.valueOf(offset));
    }

    private void setLimit(int limit)
    {
        request.put("limit", String.valueOf(limit));
    }

    private void setSort(String column, Boolean ascending)
    {
        request.put("sort", column);
        if (ascending != null) {
            request.put("dir", ascending ? "asc" : "desc");
        }
    }

    private void setFilter(String column, String value)
    {
        request.put(column, value);
    }

    private void setJoinMode(String column, String joinMode)
    {
        this.request.put(column + "/join_mode", joinMode);
    }

    private void setQueryFilters(String... filters)
    {
        request.put("queryFilters", StringUtils.join(filters, ','));
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
    private List<Map<String, String>> getRows()
    {
        return (List<Map<String, String>>) this.results.get("rows");
    }
}
