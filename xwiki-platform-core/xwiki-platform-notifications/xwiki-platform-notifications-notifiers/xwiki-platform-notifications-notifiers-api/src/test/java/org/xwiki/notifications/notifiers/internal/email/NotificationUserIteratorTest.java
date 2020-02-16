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
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.notifiers.email.NotificationEmailInterval;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
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
 * @version $Id$
 */
@ComponentTest
public class NotificationUserIteratorTest
{
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
        Query query1 = mock(Query.class);
        Query query2 = mock(Query.class);
        Query query3 = mock(Query.class);
        when(queryManager.createQuery(ArgumentMatchers.anyString(), eq(Query.XWQL))).thenReturn(query1, query2, query3,
                query1, query2, query3);
        when(query1.execute()).thenReturn(Arrays.asList("XWiki.UserA", "XWiki.UserB", "XWiki.UserC"));
        when(query2.execute()).thenReturn(Arrays.asList("XWiki.UserD"));
        when(query3.execute()).thenReturn(Collections.emptyList());

        DocumentReference userA = new DocumentReference("wikiA", "XWiki", "UserA");
        DocumentReference userB = new DocumentReference("wikiA", "XWiki", "UserB");
        DocumentReference userC = new DocumentReference("wikiA", "XWiki", "UserC");
        DocumentReference userD = new DocumentReference("wikiA", "XWiki", "UserD");
        when(resolver.resolve("XWiki.UserA", new WikiReference("wikiA"))).thenReturn(userA);
        when(resolver.resolve("XWiki.UserB", new WikiReference("wikiA"))).thenReturn(userB);
        when(resolver.resolve("XWiki.UserC", new WikiReference("wikiA"))).thenReturn(userC);
        when(resolver.resolve("XWiki.UserD", new WikiReference("wikiA"))).thenReturn(userD);

        DocumentReference classReference = new DocumentReference("wikiA",
                Arrays.asList("XWiki", "Notifications", "Code"), "NotificationEmailPreferenceClass");
        when(documentAccessBridge.getProperty(userA, classReference, "interval")).thenReturn("weekly");
        when(documentAccessBridge.getProperty(userB, classReference, "interval")).thenReturn("daily");
        when(documentAccessBridge.getProperty(userC, classReference, "interval")).thenReturn(null);
        when(documentAccessBridge.getProperty(userD, classReference, "interval")).thenReturn("daily");

        // Test with DAILY interval
        userIterator.initialize(NotificationEmailInterval.DAILY);

        assertTrue(userIterator.hasNext());
        assertEquals(userB, userIterator.next());
        assertTrue(userIterator.hasNext());
        assertEquals(userC, userIterator.next());
        assertTrue(userIterator.hasNext());
        assertEquals(userD, userIterator.next());
        assertFalse(userIterator.hasNext());

        // Test with WEEKLY interval
        userIterator.initialize(NotificationEmailInterval.WEEKLY);
        assertTrue(userIterator.hasNext());
        assertEquals(userA, userIterator.next());
        assertFalse(userIterator.hasNext());

        // Checks
        verify(query1, atLeastOnce()).setLimit(50);
        verify(query1, atLeastOnce()).setOffset(0);

        verify(query2, atLeastOnce()).setLimit(50);
        verify(query2, atLeastOnce()).setOffset(50);

        verify(query3, atLeastOnce()).setLimit(50);
        verify(query3, atLeastOnce()).setOffset(100);
    }

}
