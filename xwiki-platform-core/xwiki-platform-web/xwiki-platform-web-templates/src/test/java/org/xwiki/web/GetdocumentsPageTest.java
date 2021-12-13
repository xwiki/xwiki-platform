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
package org.xwiki.web;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.spy;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.SecurityConfiguration;
import org.xwiki.security.internal.DefaultSecurityConfiguration;
import org.xwiki.security.script.SecurityScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.internal.XWikiDateTool;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.velocity.tools.JSONTool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@code getdocuments.vm} template. Assert that the returned results are well-formed.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
@ComponentList({
    XWikiDateTool.class,
    ModelScriptService.class,
    SecurityScriptService.class,
    DefaultSecurityConfiguration.class
})
class GetdocumentsPageTest extends PageTest
{
    private static final String GETDOCUMENTS = "getdocuments.vm";

    @Mock
    private QueryManagerScriptService queryService;

    @Mock
    private ScriptQuery query;

    private TemplateManager templateManager;

    private JSONTool jsonTool;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);

        this.jsonTool = spy(new JSONTool());
        registerVelocityTool("jsontool", this.jsonTool);
        registerVelocityTool("mathtool", new MathTool());
        registerVelocityTool("escapetool", new EscapeTool());
        registerVelocityTool("numbertool", new NumberTool());
    }

    @Test
    void removeObfuscatedResultsWhenTotalrowsLowerThanLimit() throws Exception
    {
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("view"), any(), any(), any()))
            .thenReturn(false, true);
        this.request.put("limit", "2");
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.bindValues(any(List.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(3L);
        when(this.query.execute()).thenReturn(Arrays.asList("XWiki.NotViewable", "XWiki.Viewable"));

        Map<String, Object> result = getJsonResultMap();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals(2, rows.size());
        assertEquals(2, result.get("returnedrows"));
        Map<String, Object> obfuscated = rows.get(0);
        assertFalse((boolean) obfuscated.get("doc_viewable"));
        assertEquals("obfuscated", obfuscated.get("doc_fullName"));

        Map<String, Object> viewable = rows.get(1);
        assertTrue((boolean) viewable.get("doc_viewable"));
        assertEquals("xwiki:XWiki.Viewable", viewable.get("doc_fullName"));
    }

    @Test
    void nonViewableResultsAreObfuscated() throws Exception
    {
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("view"), any(), any(), any())).thenReturn(false,
            true);
        this.request.put("limit", "2");
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.bindValues(any(List.class))).thenReturn(this.query);
        when(this.query.count()).thenReturn(2L);
        when(this.query.execute()).thenReturn(Arrays.asList("XWiki.NotViewable", "XWiki.Viewable"));

        Map<String, Object> result = getJsonResultMap();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
        assertEquals(1, rows.size());
        assertEquals(1, result.get("returnedrows"));

        Map<String, Object> viewable = rows.get(0);
        assertTrue((boolean) viewable.get("doc_viewable"));
        assertEquals("xwiki:XWiki.Viewable", viewable.get("doc_fullName"));
    }

    @Test
    void preventDOSAttackOnQueryItemsReturned() throws Exception
    {
        // Simulating the fact that when getdocuments.vm executes, the xwikivars.vm template has already been loaded by
        // the page rendering.
        this.templateManager.render("xwikivars.vm");

        this.request.put("limit", "101");
        when(this.queryService.hql(anyString())).thenReturn(this.query);
        when(this.query.setLimit(anyInt())).thenReturn(this.query);
        when(this.query.setOffset(anyInt())).thenReturn(this.query);
        when(this.query.bindValues(any(Map.class))).thenReturn(this.query);
        when(this.query.bindValues(any(List.class))).thenReturn(this.query);

        // Simulate the query limit
        SecurityConfiguration securityConfiguration =
            this.oldcore.getMocker().registerMockComponent(SecurityConfiguration.class);
        when(securityConfiguration.getQueryItemsLimit()).thenReturn(100);

        this.templateManager.render(GETDOCUMENTS);

        ArgumentCaptor<Integer> argument = ArgumentCaptor.forClass(Integer.class);
        verify(this.query).setLimit(argument.capture());

        // Verify that even though the guest user is asking for 101 items, we only return 100.
        assertEquals(100, argument.getValue());
    }

    /**
     * @return the captured JSON map before serialization, to make it easier for each test to assert the map content.
     */
    private Map<String, Object> getJsonResultMap() throws Exception
    {
        this.templateManager.render(GETDOCUMENTS);

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(this.jsonTool).serialize(argument.capture());

        return (Map<String, Object>) argument.getValue();
    }
}
