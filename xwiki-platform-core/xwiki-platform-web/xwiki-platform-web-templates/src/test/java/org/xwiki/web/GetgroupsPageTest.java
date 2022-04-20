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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.tools.JSONTool;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin;
import com.xpn.xwiki.user.api.XWikiGroupService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@code getgroups.vm} template. Assert that the returned results are well-formed.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
@SecurityScriptServiceComponentList
class GetgroupsPageTest extends PageTest
{
    private static final String GETGROUPS = "getgroups.vm";

    private TemplateManager templateManager;

    private XWikiGroupService groupService;

    private ContextualAuthorizationManager contextualAuthorizationManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        // Enable the Rights Manager plugin.
        this.oldcore.getSpyXWiki().getPluginManager().addPlugin("rightsmanager", RightsManagerPlugin.class.getName(),
            this.oldcore.getXWikiContext());

        this.groupService = this.context.getWiki().getGroupService(this.context);

        this.contextualAuthorizationManager = this.componentManager.getInstance(ContextualAuthorizationManager.class);
    }

    @Test
    void removeObuscatedResultsWhenTotalrowsLowerThanLimit() throws Exception
    {
        this.request.put("wiki", "local");
        this.request.put("limit", "10");
        this.request.put("offset", "1");

        DocumentReference group1DocumentReference = new DocumentReference("xwiki", "XWiki", "G1");
        DocumentReference group2DocumentReference = new DocumentReference("xwiki", "XWiki", "G2");
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, group2DocumentReference)).thenReturn(false);
        XWikiDocument group1 = this.xwiki.getDocument(group1DocumentReference, this.context);
        XWikiDocument group2 = this.xwiki.getDocument(group2DocumentReference, this.context);

        
        doReturn(Arrays.asList(group1, group2))
            .when(this.groupService)
            .getAllMatchedGroups(eq(null), eq(true), eq(10), eq(0), any(), eq(this.context));
        when(this.groupService.countAllMatchedGroups(any(), eq(this.context))).thenReturn(2);

        Map<String, Object> results = getJsonResultMap();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) results.get("rows");
        assertEquals(1, rows.size());
        assertEquals("XWiki.G1", rows.get(0).get("doc_fullName"));
    }

    @Test
    void nonViewableResultsAreObfuscated() throws Exception
    {
        this.request.put("wiki", "local");
        this.request.put("limit", "2");
        this.request.put("offset", "1");

        DocumentReference group1DocumentReference = new DocumentReference("xwiki", "XWiki", "G1");
        DocumentReference group2DocumentReference = new DocumentReference("xwiki", "XWiki", "G2");
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, group2DocumentReference)).thenReturn(false);
        XWikiDocument group1 = this.xwiki.getDocument(group1DocumentReference, this.context);
        XWikiDocument group2 = this.xwiki.getDocument(group2DocumentReference, this.context);


        doReturn(Arrays.asList(group1, group2))
            .when(this.groupService)
            .getAllMatchedGroups(eq(null), eq(true), eq(2), eq(0), any(), eq(this.context));
        when(this.groupService.countAllMatchedGroups(any(), eq(this.context))).thenReturn(10);

        Map<String, Object> results = getJsonResultMap();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) results.get("rows");
        assertEquals(2, rows.size());
        assertEquals("XWiki.G1", rows.get(0).get("doc_fullName"));
        assertTrue((Boolean) rows.get(0).get("doc_viewable"));
        assertEquals("obfuscated", rows.get(1).get("doc_fullName"));
        assertFalse((Boolean) rows.get(1).get("doc_viewable"));
    }

    private Map<String, Object> getJsonResultMap() throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);

        this.templateManager.render(GETGROUPS);

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(jsonTool).serialize(argument.capture());

        return (Map<String, Object>) argument.getValue();
    }
}
