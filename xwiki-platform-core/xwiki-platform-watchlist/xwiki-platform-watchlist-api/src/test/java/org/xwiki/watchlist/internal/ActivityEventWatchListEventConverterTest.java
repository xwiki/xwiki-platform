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
package org.xwiki.watchlist.internal;

import java.util.Arrays;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.watchlist.internal.api.WatchListEvent;

import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.impl.ActivityEventImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test the conversion from {@link ActivityEvent} to {@link WatchListEvent} done by
 * {@link ActivityEventWatchListEventConverter}.
 *
 * @version $Id$
 * @since 7.2M1
 */
public class ActivityEventWatchListEventConverterTest
{
    @Rule
    public final MockitoComponentMockingRule<WatchListEventConverter<ActivityEvent>> mocker =
        new MockitoComponentMockingRule<WatchListEventConverter<ActivityEvent>>(
            ActivityEventWatchListEventConverter.class);

    @Test
    public void testConversion() throws Exception
    {
        // Input.
        ActivityEvent activityEvent = new ActivityEventImpl();
        activityEvent.setWiki("xwiki");
        activityEvent.setPage("Space1.Space2.Page");
        activityEvent.setType("update");
        activityEvent.setUser("xwiki:XWiki.SomeUser");
        activityEvent.setVersion("1.3");
        activityEvent.setDate(new Date());

        // Mocks
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Space1", "Space2"), "Page");
        WikiReference wikiReference = new WikiReference(activityEvent.getWiki());
        EntityReferenceResolver<String> resolver = mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "explicit");
        // Note: cheating a bit, it should return an entityReference instead of documentReference, but we are fine.
        when(resolver.resolve(activityEvent.getPage(), EntityType.DOCUMENT, wikiReference)).thenReturn(
            documentReference);

        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "SomeUser");
        when(resolver.resolve(activityEvent.getUser(), EntityType.DOCUMENT, wikiReference)).thenReturn(userReference);

        // Convert.
        WatchListEvent watchListEvent = mocker.getComponentUnderTest().convert(activityEvent);

        // Test the output.
        assertEquals(documentReference, watchListEvent.getDocumentReference());

        assertEquals(activityEvent.getWiki(), watchListEvent.getDocumentReference().getWikiReference().getName());

        assertEquals(activityEvent.getType(), watchListEvent.getType());

        assertEquals(userReference, watchListEvent.getAuthorReference());
        assertEquals(1, watchListEvent.getAuthorReferences().size());

        assertEquals(activityEvent.getVersion(), watchListEvent.getVersion());
        assertEquals(1, watchListEvent.getVersions().size());

        assertEquals(activityEvent.getDate(), watchListEvent.getDate());
        assertEquals(1, watchListEvent.getDates().size());
    }
}
