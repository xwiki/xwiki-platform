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
package org.xwiki.notifications.sources.internal;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.internal.EventStreamConfiguration;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Validate {@link EventSearcher}.
 *
 * @version $Id$
 */
@ComponentTest
class EventSearcherTest
{
    @InjectMockComponents
    private EventSearcher searcher;

    @MockComponent
    private EventStore eventStore;

    @MockComponent
    private EventQueryGenerator eventQueryGenerator;

    @MockComponent
    private EventStreamConfiguration configuration;

    @Mock
    private NotificationParameters parameters;

    @BeforeEach
    void beforeEach()
    {
        when(this.configuration.isEventStoreEnabled()).thenReturn(true);
    }

    @Test
    void searchEvents() throws Exception
    {
        SimpleEventQuery query = new SimpleEventQuery();
        when(this.eventQueryGenerator.generateQuery(this.parameters)).thenReturn(query);
        Event event = mock(Event.class);
        EventSearchResult result = mock(EventSearchResult.class);
        when(result.stream()).thenReturn(Stream.of(event));
        when(this.eventStore.search(query)).thenReturn(result);

        assertEquals(List.of(event), this.searcher.searchEvents(10, 20, this.parameters));

        assertEquals(10, query.getOffset());
        assertEquals(20, query.getLimit());
        verify(result).close();
    }

    @Test
    void searchEventsWhenNoEventCanMatch() throws Exception
    {
        // No query is generated when there is no notification preference (e.g. for the guest user).
        when(this.eventQueryGenerator.generateQuery(this.parameters)).thenReturn(null);

        assertEquals(List.of(), this.searcher.searchEvents(0, 10, this.parameters));

        verifyNoInteractions(this.eventStore);
    }

    @Test
    void searchEventsWhenEventStoreIsDisabled() throws Exception
    {
        when(this.configuration.isEventStoreEnabled()).thenReturn(false);

        assertEquals(List.of(), this.searcher.searchEvents(0, 10, this.parameters));

        verifyNoInteractions(this.eventQueryGenerator, this.eventStore);
    }
}
