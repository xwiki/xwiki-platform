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
package org.xwiki.messagestream.internal;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.filters.NotificationFilter.FilterPolicy;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.web.Utils;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.notifications.NotificationFormat.ALERT;
import static org.xwiki.notifications.filters.NotificationFilter.FilterPolicy.FILTER;
import static org.xwiki.notifications.filters.NotificationFilter.FilterPolicy.NO_EFFECT;

/**
 * Test of {@link GroupMessageStreamNotificationFilter}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
class GroupMessageStreamNotificationFilterTest
{
    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "User");

    private static final String GROUP_NAME = "GroupName";

    @InjectMockComponents
    private GroupMessageStreamNotificationFilter groupMessageFilter;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xWikiContext;

    @BeforeEach
    void setUp()
    {
        this.xWikiContext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xWikiContext);
    }

    @Test
    void filterEventWrongType()
    {
        DefaultEvent event = new DefaultEvent();
        event.setType("WRONG TYPE");
        FilterPolicy filterPolicy = this.groupMessageFilter.filterEvent(event, USER, emptyList(), ALERT);
        assertEquals(NO_EFFECT, filterPolicy);
    }

    @Test
    void filterEventUserNull()
    {
        DefaultEvent event = new DefaultEvent();
        event.setType(this.groupMessageFilter.getEventType());
        FilterPolicy filterPolicy = this.groupMessageFilter.filterEvent(event, null, emptyList(), ALERT);
        assertEquals(FILTER, filterPolicy);
    }

    @Test
    void filterEventUserGetUserNull(MockitoComponentManager rootComponentManager) throws Exception
    {
        Utils.setComponentManager(rootComponentManager);
        ComponentManager componentManager =
            rootComponentManager.registerMockComponent(ComponentManager.class, "context");
        when(componentManager.getInstance(DocumentReferenceResolver.class, "currentmixed"))
            .thenReturn(mock(DocumentReferenceResolver.class));

        XWiki xWiki = mock(XWiki.class);
        when(this.xWikiContext.getWiki()).thenReturn(xWiki);

        when(xWiki.getUser(USER, this.xWikiContext)).thenReturn(null);

        DefaultEvent event = new DefaultEvent();
        event.setType(this.groupMessageFilter.getEventType());
        event.setStream(GROUP_NAME);
        FilterPolicy filterPolicy = this.groupMessageFilter.filterEvent(event, USER, emptyList(), ALERT);
        assertEquals(NO_EFFECT, filterPolicy);
    }

    @Test
    void filterEventUserNotInGroup(MockitoComponentManager rootComponentManager) throws Exception
    {
        Utils.setComponentManager(rootComponentManager);
        ComponentManager componentManager =
            rootComponentManager.registerMockComponent(ComponentManager.class, "context");
        when(componentManager.getInstance(DocumentReferenceResolver.class, "currentmixed"))
            .thenReturn(mock(DocumentReferenceResolver.class));

        XWiki xWiki = mock(XWiki.class);
        when(this.xWikiContext.getWiki()).thenReturn(xWiki);

        User user = mock(User.class);
        when(xWiki.getUser(USER, this.xWikiContext)).thenReturn(user);
        when(user.isUserInGroup(GROUP_NAME)).thenReturn(false);

        DefaultEvent event = new DefaultEvent();
        event.setType(this.groupMessageFilter.getEventType());
        event.setStream(GROUP_NAME);
        FilterPolicy filterPolicy = this.groupMessageFilter.filterEvent(event, USER, emptyList(), ALERT);
        assertEquals(FILTER, filterPolicy);
    }

    @Test
    void filterEventUser(MockitoComponentManager rootComponentManager) throws Exception
    {
        Utils.setComponentManager(rootComponentManager);
        ComponentManager componentManager =
            rootComponentManager.registerMockComponent(ComponentManager.class, "context");
        when(componentManager.getInstance(DocumentReferenceResolver.class, "currentmixed"))
            .thenReturn(mock(DocumentReferenceResolver.class));

        XWiki xWiki = mock(XWiki.class);
        when(this.xWikiContext.getWiki()).thenReturn(xWiki);

        User user = mock(User.class);
        when(xWiki.getUser(USER, this.xWikiContext)).thenReturn(user);
        when(user.isUserInGroup(GROUP_NAME)).thenReturn(true);

        DefaultEvent event = new DefaultEvent();
        event.setType(this.groupMessageFilter.getEventType());
        event.setStream(GROUP_NAME);
        FilterPolicy filterPolicy = this.groupMessageFilter.filterEvent(event, USER, emptyList(), ALERT);
        assertEquals(NO_EFFECT, filterPolicy);
    }

    @Test
    void filterExpressionNull()
    {
        assertNull(this.groupMessageFilter.filterExpression(null, null, null));
    }

    @Test
    void getName()
    {
        assertEquals("Group Message Stream Notification Filter", this.groupMessageFilter.getName());
    }
}
