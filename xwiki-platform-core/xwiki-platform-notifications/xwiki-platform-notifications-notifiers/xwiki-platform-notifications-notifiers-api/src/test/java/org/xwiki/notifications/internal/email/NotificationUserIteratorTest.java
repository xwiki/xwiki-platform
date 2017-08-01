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
package org.xwiki.notifications.internal.email;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.notifiers.email.NotificationEmailInterval;
import org.xwiki.notifications.notifiers.internal.email.NotificationUserIterator;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class NotificationUserIteratorTest
{
    @Rule
    public final MockitoComponentMockingRule<NotificationUserIterator> mocker =
            new MockitoComponentMockingRule<>(NotificationUserIterator.class);

    private QueryManager queryManager;
    private DocumentReferenceResolver<String> resolver;
    private WikiDescriptorManager wikiDescriptorManager;
    private DocumentAccessBridge documentAccessBridge;

    @Before
    public void setUp() throws Exception
    {
        queryManager = mocker.getInstance(QueryManager.class);
        resolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);

        Mockito.when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiA");
    }

    @Test
    public void test() throws Exception
    {
        // Mocks
        Query query1 = Mockito.mock(Query.class);
        Query query2 = Mockito.mock(Query.class);
        Query query3 = Mockito.mock(Query.class);
        Mockito.when(queryManager.createQuery(ArgumentMatchers.anyString(), ArgumentMatchers.eq(Query.XWQL))).thenReturn(query1, query2, query3,
                query1, query2, query3);
        Mockito.when(query1.execute()).thenReturn(Arrays.asList("XWiki.UserA", "XWiki.UserB", "XWiki.UserC"));
        Mockito.when(query2.execute()).thenReturn(Arrays.asList("XWiki.UserD"));
        Mockito.when(query3.execute()).thenReturn(Collections.emptyList());

        DocumentReference userA = new DocumentReference("wikiA", "XWiki", "UserA");
        DocumentReference userB = new DocumentReference("wikiA", "XWiki", "UserB");
        DocumentReference userC = new DocumentReference("wikiA", "XWiki", "UserC");
        DocumentReference userD = new DocumentReference("wikiA", "XWiki", "UserD");
        Mockito.when(resolver.resolve("XWiki.UserA", new WikiReference("wikiA"))).thenReturn(userA);
        Mockito.when(resolver.resolve("XWiki.UserB", new WikiReference("wikiA"))).thenReturn(userB);
        Mockito.when(resolver.resolve("XWiki.UserC", new WikiReference("wikiA"))).thenReturn(userC);
        Mockito.when(resolver.resolve("XWiki.UserD", new WikiReference("wikiA"))).thenReturn(userD);

        DocumentReference classReference = new DocumentReference("wikiA",
                Arrays.asList("XWiki", "Notifications", "Code"), "NotificationEmailPreferenceClass");
        Mockito.when(documentAccessBridge.getProperty(userA, classReference, "interval")).thenReturn("weekly");
        Mockito.when(documentAccessBridge.getProperty(userB, classReference, "interval")).thenReturn("daily");
        Mockito.when(documentAccessBridge.getProperty(userC, classReference, "interval")).thenReturn(null);
        Mockito.when(documentAccessBridge.getProperty(userD, classReference, "interval")).thenReturn("daily");

        // Test with DAILY interval
        NotificationUserIterator userIterator = mocker.getComponentUnderTest();
        userIterator.initialize(NotificationEmailInterval.DAILY);

        Assert.assertTrue(userIterator.hasNext());
        assertEquals(userB, userIterator.next());
        Assert.assertTrue(userIterator.hasNext());
        assertEquals(userC, userIterator.next());
        Assert.assertTrue(userIterator.hasNext());
        assertEquals(userD, userIterator.next());
        Assert.assertFalse(userIterator.hasNext());

        // Test with WEEKLY interval
        userIterator = mocker.getComponentUnderTest();
        userIterator.initialize(NotificationEmailInterval.WEEKLY);
        Assert.assertTrue(userIterator.hasNext());
        assertEquals(userA, userIterator.next());
        Assert.assertFalse(userIterator.hasNext());

        // Checks
        Mockito.verify(query1, Mockito.atLeastOnce()).setLimit(50);
        Mockito.verify(query1, Mockito.atLeastOnce()).setOffset(0);

        Mockito.verify(query2, Mockito.atLeastOnce()).setLimit(50);
        Mockito.verify(query2, Mockito.atLeastOnce()).setOffset(50);

        Mockito.verify(query3, Mockito.atLeastOnce()).setLimit(50);
        Mockito.verify(query3, Mockito.atLeastOnce()).setOffset(100);
    }

}
