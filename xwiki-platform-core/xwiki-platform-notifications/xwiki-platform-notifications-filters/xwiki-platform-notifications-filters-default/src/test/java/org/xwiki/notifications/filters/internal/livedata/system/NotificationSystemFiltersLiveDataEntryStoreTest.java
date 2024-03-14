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

package org.xwiki.notifications.filters.internal.livedata.system;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NotificationSystemFiltersLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 16.2.0RC1
 */
@ComponentTest
@ReferenceComponentList
class NotificationSystemFiltersLiveDataEntryStoreTest
{
    @InjectMockComponents
    private NotificationSystemFiltersLiveDataEntryStore entryStore;

    @MockComponent
    private NotificationFilterManager notificationFilterManager;

    @MockComponent
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeEach
    void beforeEach()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void getMissingTarget()
    {
        LiveDataQuery query = mock(LiveDataQuery.class);
        LiveDataQuery.Source source = mock(LiveDataQuery.Source.class);
        when(query.getSource()).thenReturn(source);
        when(source.getParameters()).thenReturn(Map.of());
        LiveDataException liveDataException = assertThrows(LiveDataException.class, () -> this.entryStore.get(query));
        assertEquals("The target source parameter is mandatory.", liveDataException.getMessage());
    }

    @Test
    void getBadAuthorization()
    {
        LiveDataQuery query = mock(LiveDataQuery.class);
        LiveDataQuery.Source source = mock(LiveDataQuery.Source.class);
        when(query.getSource()).thenReturn(source);
        String target = "wiki";
        String wiki = "Wiki foo";
        DocumentReference userDoc = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(context.getUserReference()).thenReturn(userDoc);
    }
}