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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.internal.reference.converter.EntityReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.user.DefaultUserComponentList;
import org.xwiki.user.internal.group.AbstractGroupCache.GroupCacheEntry;
import org.xwiki.user.internal.group.MembersCache;
import org.xwiki.user.script.GroupScriptService;
import org.xwiki.user.script.UserScriptService;
import org.xwiki.velocity.tools.JSONTool;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin;
import com.xpn.xwiki.user.api.XWikiGroupService;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@code getgroupmembers.vm} template. Assert that the returned results are well-formed.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
@ComponentList({
    ModelScriptService.class,
    EntityReferenceConverter.class,
    UserScriptService.class,
    GroupScriptService.class
})
@DefaultUserComponentList
class GetgroupmembersPageTest extends PageTest
{
    private static final String GETGROUPMEMBERS = "getgroupmembers.vm";

    private TemplateManager templateManager;

    private XWikiGroupService groupService;
    
    private MembersCache membersCache;

    @BeforeEach
    void setUp() throws Exception
    {
        setOutputSyntax(Syntax.PLAIN_1_0);

        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        // Enable the Rights Manager plugin.
        this.oldcore.getSpyXWiki().getPluginManager().addPlugin("rightsmanager", RightsManagerPlugin.class.getName(),
            this.oldcore.getXWikiContext());

        this.groupService = this.context.getWiki().getGroupService(this.context);

        // Override the members cache component with a mock.
        this.membersCache = this.componentManager.registerMockComponent(MembersCache.class);

        // Make sure User and Group script services load properly.
        this.componentManager.getInstance(ScriptService.class, "user");
        this.componentManager.getInstance(ScriptService.class, "user.group");
    }

    @Test
    void removeObuscatedResultsWhenTotalrowsLowerThanLimit() throws Exception
    {
        this.request.put("limit", "10");
        this.request.put("offset", "1");

        DocumentReference groupDocumentReference = new DocumentReference("xwiki", "XWiki", "group");

        XWikiDocument groupDoc = this.xwiki.getDocument(groupDocumentReference, this.context);
        this.context.setDoc(groupDoc);

        this.xwiki.createUser("U1", Collections.emptyMap(), this.context);
        this.xwiki.createUser("U2", Collections.emptyMap(), this.context);

        when(this.xwiki.getRightService().hasAccessLevel("view", "XWiki.XWikiGuest", "U2", this.context))
            .thenAnswer(invocationOnMock -> false);

        GroupCacheEntry groupCacheEntry = mock(GroupCacheEntry.class);
        when(this.membersCache.getCacheEntry(groupDocumentReference, true)).thenReturn(groupCacheEntry);
        when(groupCacheEntry.getAll()).thenReturn(Arrays.asList(
            new DocumentReference("xwiki", "XWiki", "U1"),
            new DocumentReference("xwiki", "XWiki", "U2")
        ));

        when(this.groupService.getAllMatchedMembersNamesForGroup("XWiki.group", null, 10, 0, true, this.context))
            .thenReturn(asList(
                "U1",
                "U2"));
        Map<String, Object> results = getJsonResultMap();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) results.get("rows");
        assertEquals(1, rows.size());
        assertEquals("U1", rows.get(0).get("doc_fullName"));
    }

    @Test
    void nonViewableResultsAreObfuscated() throws Exception
    {
        this.request.put("limit", "2");
        this.request.put("offset", "1");

        DocumentReference groupDocumentReference = new DocumentReference("xwiki", "XWiki", "group");

        XWikiDocument groupDoc = this.xwiki.getDocument(groupDocumentReference, this.context);
        this.context.setDoc(groupDoc);

        this.xwiki.createUser("U1", Collections.emptyMap(), this.context);
        this.xwiki.createUser("U2", Collections.emptyMap(), this.context);

        when(this.xwiki.getRightService().hasAccessLevel("view", "XWiki.XWikiGuest", "U2", this.context))
            .thenAnswer(invocationOnMock -> false);

        GroupCacheEntry groupCacheEntry = mock(GroupCacheEntry.class);
        when(this.membersCache.getCacheEntry(groupDocumentReference, true)).thenReturn(groupCacheEntry);
        when(groupCacheEntry.getAll()).thenReturn(Arrays.asList(
            new DocumentReference("xwiki", "XWiki", "U1"),
            new DocumentReference("xwiki", "XWiki", "U2"),
            new DocumentReference("xwiki", "XWiki", "U3")
        ));

        when(this.groupService.getAllMatchedMembersNamesForGroup("XWiki.group", null, 2, 0, true, this.context))
            .thenReturn(asList(
                "U1",
                "U2"));
        Map<String, Object> results = getJsonResultMap();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) results.get("rows");
        assertEquals(2, rows.size());
        assertEquals("U1", rows.get(0).get("doc_fullName"));
        assertTrue((Boolean) rows.get(0).get("doc_viewable"));
        assertFalse((Boolean) rows.get(1).get("doc_viewable"));
    }

    private Map<String, Object> getJsonResultMap() throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);

        this.templateManager.render(GETGROUPMEMBERS);

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(jsonTool).serialize(argument.capture());

        return (Map<String, Object>) argument.getValue();
    }
}
