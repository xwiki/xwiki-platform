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

package org.xwiki.notifications.rest.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityFactory;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.notifications.filters.watch.WatchedLocationReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultNotificationsWatchResource}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultNotificationsWatchResourceTest
{
    @InjectMockComponents
    private DefaultNotificationsWatchResource notificationsWatchResource;

    @MockComponent
    private WatchedEntitiesManager watchedEntitiesManager;

    @MockComponent
    private WatchedEntityFactory watchedEntityFactory;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeComponent
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void getPageWatchStatusEmptyWiki()
    {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
            this.notificationsWatchResource.getPageWatchStatus("", null, null));
        assertEquals("wikiName must be not null. Current value: []", illegalArgumentException.getMessage());
    }

    @Test
    void getPageWatchStatusGuestUser() throws Exception
    {
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userReference);
        when(this.userReferenceResolver.resolve(userReference)).thenReturn(GuestUserReference.INSTANCE);
        Response response = this.notificationsWatchResource.getPageWatchStatus("wiki", null, null);
        assertEquals(401, response.getStatus());
        assertEquals("Only logged-in users can access this.", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    void getPageWatchStatusWiki() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        WikiReference wikiReference = new WikiReference(wikiName);
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(wikiReference))
            .thenReturn(watchedLocationReference);
        WatchedEntityReference.WatchedStatus watchedStatus =
            WatchedEntityReference.WatchedStatus.WATCHED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS;
        when(watchedLocationReference.getWatchedStatus(userReference)).thenReturn(watchedStatus);

        Response response = this.notificationsWatchResource.getPageWatchStatus(wikiName, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(watchedStatus, response.getEntity());
    }

    @Test
    void getPageWatchStatusSpace() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        SpaceReference spaceReference = new SpaceReference(wikiName, List.of("Foo", "Bar", "Space"));
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(spaceReference))
            .thenReturn(watchedLocationReference);
        WatchedEntityReference.WatchedStatus watchedStatus =
            WatchedEntityReference.WatchedStatus.BLOCKED_FOR_ALL_EVENTS_AND_FORMATS;
        when(watchedLocationReference.getWatchedStatus(userReference)).thenReturn(watchedStatus);

        Response response = this.notificationsWatchResource
            .getPageWatchStatus(wikiName, "/spaces/Foo/spaces/Bar/spaces/Space", null);
        assertEquals(200, response.getStatus());
        assertEquals(watchedStatus, response.getEntity());
    }

    @Test
    void getPageWatchStatusPage() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        DocumentReference documentReference = new DocumentReference(wikiName, List.of("Foo", "Bar", "Space"), "MYDoc");
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(documentReference))
            .thenReturn(watchedLocationReference);
        WatchedEntityReference.WatchedStatus watchedStatus =
            WatchedEntityReference.WatchedStatus.CUSTOM;
        when(watchedLocationReference.getWatchedStatus(userReference)).thenReturn(watchedStatus);

        Response response = this.notificationsWatchResource
            .getPageWatchStatus(wikiName, "/spaces/Foo/spaces/Bar/spaces/Space", "/pages/MYDoc");
        assertEquals(200, response.getStatus());
        assertEquals(watchedStatus, response.getEntity());
    }

    @Test
    void watchPageEmptyWiki()
    {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
            this.notificationsWatchResource.watchPage("", null, null, false));
        assertEquals("wikiName must be not null. Current value: []", illegalArgumentException.getMessage());
    }

    @Test
    void watchPageGuestUser() throws Exception
    {
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userReference);
        when(this.userReferenceResolver.resolve(userReference)).thenReturn(GuestUserReference.INSTANCE);
        Response response = this.notificationsWatchResource.watchPage("wiki", null, null, false);
        assertEquals(401, response.getStatus());
        assertEquals("Only logged-in users can access this.", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    void watchPageWiki() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        WikiReference wikiReference = new WikiReference(wikiName);
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(wikiReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.watch(watchedLocationReference, userReference)).thenReturn(true);
        Response response = this.notificationsWatchResource.watchPage(wikiName, null, null, false);
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).watch(watchedLocationReference, userReference);
    }

    @Test
    void watchPageSpace() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        SpaceReference spaceReference = new SpaceReference(wikiName, List.of("Foo", "Bar", "Space"));
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(spaceReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.watch(watchedLocationReference, userReference)).thenReturn(true);

        Response response = this.notificationsWatchResource
            .watchPage(wikiName, "/spaces/Foo/spaces/Bar/spaces/Space", null, false);
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).watch(watchedLocationReference, userReference);
    }

    @Test
    void watchPage() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        DocumentReference documentReference = new DocumentReference(wikiName, List.of("Foo", "Bar", "Space"), "MYDoc");
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(documentReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.watch(watchedLocationReference, userReference)).thenReturn(true);

        Response response = this.notificationsWatchResource
            .watchPage(wikiName, "/spaces/Foo/spaces/Bar/spaces/Space", "/pages/MYDoc", false);
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).watch(watchedLocationReference, userReference);
    }

    @Test
    void blockPageEmptyWiki()
    {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
            this.notificationsWatchResource.watchPage("", null, null, true));
        assertEquals("wikiName must be not null. Current value: []", illegalArgumentException.getMessage());
    }

    @Test
    void blockPageGuestUser() throws Exception
    {
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userReference);
        when(this.userReferenceResolver.resolve(userReference)).thenReturn(GuestUserReference.INSTANCE);
        Response response = this.notificationsWatchResource.watchPage("wiki", null, null, true);
        assertEquals(401, response.getStatus());
        assertEquals("Only logged-in users can access this.", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    void blockPageWiki() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        WikiReference wikiReference = new WikiReference(wikiName);
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(wikiReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.block(watchedLocationReference, userReference)).thenReturn(true);
        Response response = this.notificationsWatchResource.watchPage(wikiName, null, null, true);
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).block(watchedLocationReference, userReference);
    }

    @Test
    void blockPageSpace() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        SpaceReference spaceReference = new SpaceReference(wikiName, List.of("Foo", "Bar", "Space"));
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(spaceReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.block(watchedLocationReference, userReference)).thenReturn(true);

        Response response = this.notificationsWatchResource
            .watchPage(wikiName, "/spaces/Foo/spaces/Bar/spaces/Space", null, true);
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).block(watchedLocationReference, userReference);
    }

    @Test
    void blockPage() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        DocumentReference documentReference = new DocumentReference(wikiName, List.of("Foo", "Bar", "Space"), "MYDoc");
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(documentReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.block(watchedLocationReference, userReference)).thenReturn(true);

        Response response = this.notificationsWatchResource
            .watchPage(wikiName, "/spaces/Foo/spaces/Bar/spaces/Space", "/pages/MYDoc", true);
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).block(watchedLocationReference, userReference);
    }

    @Test
    void unwatchPageEmptyWiki()
    {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
            this.notificationsWatchResource.unwatchPage("", null, null));
        assertEquals("wikiName must be not null. Current value: []", illegalArgumentException.getMessage());
    }

    @Test
    void unwatchPageGuestUser() throws Exception
    {
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userReference);
        when(this.userReferenceResolver.resolve(userReference)).thenReturn(GuestUserReference.INSTANCE);
        Response response = this.notificationsWatchResource.unwatchPage("wiki", null, null);
        assertEquals(401, response.getStatus());
        assertEquals("Only logged-in users can access this.", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    void unwatchPageWiki() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        WikiReference wikiReference = new WikiReference(wikiName);
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(wikiReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.removeWatchFilter(watchedLocationReference, userReference)).thenReturn(true);
        Response response = this.notificationsWatchResource.unwatchPage(wikiName, null, null);
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).removeWatchFilter(watchedLocationReference, userReference);
    }

    @Test
    void unwatchPageSpace() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        SpaceReference spaceReference = new SpaceReference(wikiName, List.of("Foo", "Bar", "Space"));
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(spaceReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.removeWatchFilter(watchedLocationReference, userReference)).thenReturn(true);

        Response response = this.notificationsWatchResource
            .unwatchPage(wikiName, "/spaces/Foo/spaces/Bar/spaces/Space", null);
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).removeWatchFilter(watchedLocationReference, userReference);
    }

    @Test
    void unwatchPage() throws Exception
    {
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.context.getUserReference()).thenReturn(userDocReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);
        String wikiName = "fooWiki";
        DocumentReference documentReference = new DocumentReference(wikiName, List.of("Foo", "Bar", "Space"), "MYDoc");
        WatchedLocationReference watchedLocationReference = mock(WatchedLocationReference.class);
        when(this.watchedEntityFactory.createWatchedLocationReference(documentReference))
            .thenReturn(watchedLocationReference);
        when(this.watchedEntitiesManager.removeWatchFilter(watchedLocationReference, userReference)).thenReturn(true);

        Response response = this.notificationsWatchResource
            .unwatchPage(wikiName, "/spaces/Foo/spaces/Bar/spaces/Space", "/pages/MYDoc");
        assertEquals(200, response.getStatus());
        assertEquals(true, response.getEntity());
        verify(this.watchedEntitiesManager).removeWatchFilter(watchedLocationReference, userReference);
    }
}