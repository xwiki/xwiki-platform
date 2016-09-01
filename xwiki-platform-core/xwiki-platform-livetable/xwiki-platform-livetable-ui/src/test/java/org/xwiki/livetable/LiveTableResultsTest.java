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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.velocity.tools.JSONTool;
import org.xwiki.velocity.tools.RegexTool;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.tag.TagPluginApi;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@code LiveTableResults} page.
 * 
 * @version $Id$
 */
@XWikiSyntax20ComponentList
public class LiveTableResultsTest extends PageTest
{
    private QueryManagerScriptService queryService;

    private Map<String, Object> results;

    @Before
    @SuppressWarnings("deprecation")
    public void setUp() throws Exception
    {
        setOutputSyntax(Syntax.PLAIN_1_0);
        request.put("outputSyntax", "plain");
        request.put("xpage", "plain");
        oldcore.getXWikiContext().setAction("get");

        queryService = mock(QueryManagerScriptService.class);
        oldcore.getMocker().registerComponent(ScriptService.class, "query", queryService);

        TagPluginApi tagPluginApi = mock(TagPluginApi.class);
        doReturn(tagPluginApi).when(oldcore.getSpyXWiki()).getPluginApi(eq("tag"), any(XWikiContext.class));

        registerVelocityTool("stringtool", new StringUtils());
        registerVelocityTool("mathtool", new MathTool());
        registerVelocityTool("regextool", new RegexTool());
        registerVelocityTool("numbertool", new NumberTool());

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

        ScriptQuery query = mock(ScriptQuery.class);
        when(queryService.hql("  where 1=1    order by doc.date desc")).thenReturn(query);
        when(query.addFilter("currentlanguage")).thenReturn(query);
        when(query.addFilter("hidden")).thenReturn(query);
        when(query.setLimit(7)).thenReturn(query);
        // Offset starting from 0.
        when(query.setOffset(12)).thenReturn(query);
        when(query.bindValues(anyListOf(Object.class))).thenReturn(query);

        when(query.count()).thenReturn(17L);
        when(query.execute()).thenReturn(Arrays.<Object>asList("A.B", "X.Y"));

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
                + "where obj.name=doc.fullName and obj.className = ? and doc.fullName not in (?, ?)  "
                + "and obj.id=prop_where.id.id and prop_where.name = ?   "
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

    @Test
    public void filterByLocation() throws Exception
    {
        setColumns("doc.location");
        setFilter("doc.location", "web");

        renderPage();

        verify(queryService).hql(
            "  where 1=1  and ((doc.name = 'WebHome' and upper(doc.space) like upper(?) escape '!')"
                + " or (doc.name <> 'WebHome' and upper(doc.fullName) like upper(?) escape '!')) ");
    }

    //
    // Helper methods
    //

    @SuppressWarnings("unchecked")
    private void renderPage() throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);

        renderPage(new DocumentReference("xwiki", "XWiki", "LiveTableResults"));

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(jsonTool).serialize(argument.capture());

        this.results = (Map<String, Object>) argument.getValue();
    }

    private void setClassName(String className)
    {
        request.put("classname", className);
    }

    private void setColumns(String... columns)
    {
        request.put("collist", StringUtils.join(columns, ','));
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
