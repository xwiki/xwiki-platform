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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.Arrays;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.notifications.preferences.internal.email.DefaultNotificationEmailUserPreferenceManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link NotificationUserIterator} and {@link IntervalUsersManager}.
 * 
 * @version $Id$
 */
@ComponentTest
// @formatter:off
@ComponentList({
    IntervalUsersManager.class,
    EntityReferenceFactory.class,
    DefaultNotificationEmailUserPreferenceManager.class
})
// @formatter:on
class NotificationUserIteratorTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private NotificationUserIterator userIterator;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private NotificationConfiguration notificationConfiguration;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @MockComponent
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @BeforeEach
    void setUp()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiA");
    }

    @Test
    void iterate() throws Exception
    {
        // Mocks
        Query query = mock(Query.class);
        when(this.queryManager.createQuery("select distinct doc.fullName from Document doc, "
            + "doc.object(XWiki.XWikiUsers) objUser where objUser.active = 1 and length(objUser.email) > 0 "
                + "order by doc.fullName",
            Query.XWQL)).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("XWiki.UserA", "XWiki.UserB", "XWiki.UserC", "XWiki.UserD"));

        DocumentReference userDocA = new DocumentReference("wikiA", "XWiki", "UserA");
        DocumentReference userDocB = new DocumentReference("wikiA", "XWiki", "UserB");
        DocumentReference userDocC = new DocumentReference("wikiA", "XWiki", "UserC");
        DocumentReference userDocD = new DocumentReference("wikiA", "XWiki", "UserD");

        when(this.resolver.resolve("XWiki.UserA", new WikiReference("wikiA"))).thenReturn(userDocA);
        when(this.resolver.resolve("XWiki.UserB", new WikiReference("wikiA"))).thenReturn(userDocB);
        when(this.resolver.resolve("XWiki.UserC", new WikiReference("wikiA"))).thenReturn(userDocC);
        when(this.resolver.resolve("XWiki.UserD", new WikiReference("wikiA"))).thenReturn(userDocD);

        UserReference userA = mock(UserReference.class, "userA");
        UserReference userB = mock(UserReference.class, "userB");
        UserReference userC = mock(UserReference.class, "userC");
        UserReference userD = mock(UserReference.class, "userD");

        when(this.userReferenceSerializer.serialize(userA)).thenReturn(userDocA);
        when(this.userReferenceSerializer.serialize(userB)).thenReturn(userDocB);
        when(this.userReferenceSerializer.serialize(userC)).thenReturn(userDocC);
        when(this.userReferenceSerializer.serialize(userD)).thenReturn(userDocD);

        when(this.userReferenceResolver.resolve(userDocA)).thenReturn(userA);
        when(this.userReferenceResolver.resolve(userDocB)).thenReturn(userB);
        when(this.userReferenceResolver.resolve(userDocC)).thenReturn(userC);
        when(this.userReferenceResolver.resolve(userDocD)).thenReturn(userD);

        when(wikiDescriptorManager.getMainWikiId()).thenReturn("wikiA");

        DocumentReference classReference = new DocumentReference("wikiA",
            Arrays.asList("XWiki", "Notifications", "Code"), "NotificationEmailPreferenceClass");
        when(documentAccessBridge.getProperty(userDocA, classReference, "interval")).thenReturn("weekly");
        when(documentAccessBridge.getProperty(userDocB, classReference, "interval")).thenReturn("daily");
        when(documentAccessBridge.getProperty(userDocC, classReference, "interval")).thenReturn(null);
        when(documentAccessBridge.getProperty(userDocD, classReference, "interval")).thenReturn("daily");

        // Test with DAILY interval
        this.userIterator.initialize(NotificationEmailInterval.DAILY);

        assertTrue(this.userIterator.hasNext());
        assertEquals(userDocB, this.userIterator.next());
        assertTrue(this.userIterator.hasNext());
        assertEquals(userDocC, this.userIterator.next());
        assertTrue(this.userIterator.hasNext());
        assertEquals(userDocD, this.userIterator.next());
        assertFalse(this.userIterator.hasNext());

        // Test with WEEKLY interval
        this.userIterator.initialize(NotificationEmailInterval.WEEKLY);
        assertTrue(this.userIterator.hasNext());
        assertEquals(userDocA, this.userIterator.next());
        assertFalse(this.userIterator.hasNext());

        // Checks
        verify(query, atLeastOnce()).setLimit(100);
        verify(query, atLeastOnce()).setOffset(0);
    }

    @Test
    void iterateWhenException() throws Exception
    {
        when(this.queryManager.createQuery(ArgumentMatchers.anyString(), eq(Query.XWQL)))
            .thenThrow(new QueryException("error", null, null));

        this.userIterator.initialize(NotificationEmailInterval.DAILY);
        assertFalse(this.userIterator.hasNext());

        assertEquals("Failed to retrieve the notification users. Root error [QueryException: error]",
            logCapture.getMessage(0));
    }
}
