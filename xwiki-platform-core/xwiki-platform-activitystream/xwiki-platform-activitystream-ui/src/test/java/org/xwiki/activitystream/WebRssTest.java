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
package org.xwiki.activitystream;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.test.page.XWikiSyntax20ComponentList;

import com.xpn.xwiki.plugin.feed.FeedPlugin;
import com.xpn.xwiki.plugin.feed.FeedPluginApi;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for testing the {@code Main.WebRss} wiki page.
 *
 * @version $Id$
 * @since 7.3M1
 */
@XWikiSyntax20ComponentList
@XHTML10ComponentList
public class WebRssTest extends PageTest
{
    private ScriptQuery query;

    @Before
    public void setUp() throws Exception
    {
        setOutputSyntax(Syntax.PLAIN_1_0);
        request.put("outputSyntax", "plain");
        request.put("xpage", "plain");

        QueryManagerScriptService qmss = mock(QueryManagerScriptService.class);
        oldcore.getMocker().registerComponent(ScriptService.class, "query", qmss);
        query = mock(ScriptQuery.class);
        when(qmss.xwql("where 1=1 order by doc.date desc")).thenReturn(query);
    }

    @Test
    public void webRssFiltersHiddenDocuments() throws Exception
    {
        // Render the page to test
        renderPage(new DocumentReference("xwiki", "Main", "WebRss"));

        // This is the real test!!
        // We want to verify that the hidden document filter is called when executing the XWQL
        // query to get the list of modified pages
        verify(query).addFilter("hidden/document");
    }

    @Test
    public void webRssDisplay() throws Exception
    {
        when(query.addFilter(anyString())).thenReturn(query);
        when(query.setLimit(20)).thenReturn(query);
        when(query.setOffset(0)).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.<Object>asList("Space1.Page1", "Space2.Page2"));

        FeedPlugin plugin = new FeedPlugin("feed", FeedPlugin.class.getName(), context);
        FeedPluginApi pluginApi = new FeedPluginApi(plugin, context);
        doReturn(pluginApi).when(xwiki).getPluginApi("feed", context);

        // Render the page to test
        String xml = renderPage(new DocumentReference("xwiki", "Main", "WebRss"));

        assertTrue(xml.contains("<title>activity.rss.feed.description</title>"));
        assertTrue(xml.contains("<title>Page1</title>"));
        assertTrue(xml.contains("<title>Page2</title>"));
    }
}
