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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
@ComponentList({IntervalUsersManager.class, EntityReferenceFactory.class})
public class NotificationUserIteratorTest
{
    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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

    @BeforeEach
    void setUp()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiA");
    }

    @Test
    public void iterate() throws Exception
    {
        // Mocks
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(ArgumentMatchers.eq("select distinct doc.fullName from Document doc, "
            + "doc.object(XWiki.XWikiUsers) objUser where objUser.active = 1 and length(objUser.email) > 0 order by doc.fullName"),
            eq(Query.XWQL))).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("XWiki.UserA", "XWiki.UserB", "XWiki.UserC", "XWiki.UserD"));

        DocumentReference userA = new DocumentReference("wikiA", "XWiki", "UserA");
        DocumentReference userB = new DocumentReference("wikiA", "XWiki", "UserB");
        DocumentReference userC = new DocumentReference("wikiA", "XWiki", "UserC");
        DocumentReference userD = new DocumentReference("wikiA", "XWiki", "UserD");
        when(this.resolver.resolve("XWiki.UserA", new WikiReference("wikiA"))).thenReturn(userA);
        when(this.resolver.resolve("XWiki.UserB", new WikiReference("wikiA"))).thenReturn(userB);
        when(this.resolver.resolve("XWiki.UserC", new WikiReference("wikiA"))).thenReturn(userC);
        when(this.resolver.resolve("XWiki.UserD", new WikiReference("wikiA"))).thenReturn(userD);

        DocumentReference classReference = new DocumentReference("wikiA",
            Arrays.asList("XWiki", "Notifications", "Code"), "NotificationEmailPreferenceClass");
        when(documentAccessBridge.getProperty(userA, classReference, "interval")).thenReturn("weekly");
        when(documentAccessBridge.getProperty(userB, classReference, "interval")).thenReturn("daily");
        when(documentAccessBridge.getProperty(userC, classReference, "interval")).thenReturn(null);
        when(documentAccessBridge.getProperty(userD, classReference, "interval")).thenReturn("daily");

        // Test with DAILY interval
        this.userIterator.initialize(NotificationEmailInterval.DAILY);

        assertTrue(this.userIterator.hasNext());
        assertEquals(userB, this.userIterator.next());
        assertTrue(this.userIterator.hasNext());
        assertEquals(userC, this.userIterator.next());
        assertTrue(this.userIterator.hasNext());
        assertEquals(userD, this.userIterator.next());
        assertFalse(this.userIterator.hasNext());

        // Test with WEEKLY interval
        this.userIterator.initialize(NotificationEmailInterval.WEEKLY);
        assertTrue(this.userIterator.hasNext());
        assertEquals(userA, this.userIterator.next());
        assertFalse(this.userIterator.hasNext());

        // Checks
        verify(query, atLeastOnce()).setLimit(100);
        verify(query, atLeastOnce()).setOffset(0);
    }

    @Test
    public void iterateWhenException() throws Exception
    {
        when(this.queryManager.createQuery(ArgumentMatchers.anyString(), eq(Query.XWQL)))
            .thenThrow(new QueryException("error", null, null));

        this.userIterator.initialize(NotificationEmailInterval.DAILY);
        assertFalse(this.userIterator.hasNext());

        assertEquals("Failed to retrieve the notification users. Root error [QueryException: error]",
            logCapture.getMessage(0));
    }
}
