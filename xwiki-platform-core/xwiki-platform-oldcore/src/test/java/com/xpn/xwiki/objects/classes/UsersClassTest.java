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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.web.Utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link com.xpn.xwiki.objects.classes.UsersClass}.
 *
 * @version $Id$
 * @since 5.1M2
 */
public class UsersClassTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    @Test
    public void getMap() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);
        XWikiGroupService groupService = mock(XWikiGroupService.class);
        when(xwiki.getGroupService(any(XWikiContext.class))).thenReturn(groupService);
        when(groupService.getAllMatchedUsers(any(Object[][].class), anyBoolean(), anyInt(), anyInt(),
            any(Object[][].class), any(XWikiContext.class))).thenReturn((List)Arrays.asList("XWiki.Admin"));
        when(xwiki.getUserName(eq("XWiki.Admin"), anyString(), anyBoolean(), any(XWikiContext.class)))
            .thenReturn("Administrator");

        Utils.setComponentManager(this.componentManager);
        this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        this.componentManager.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");

        UsersClass usersClass = new UsersClass();
        Map<String, ListItem> results = usersClass.getMap(context);
        assertEquals(1, results.size());
        assertEquals("XWiki.Admin", results.get("XWiki.Admin").getId());
        assertEquals("Administrator", results.get("XWiki.Admin").getValue());
    }
}
