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
package org.xwiki.eventstream.store.solr.internal;

import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.LazySolrCore;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrEventStore}, with a mocked {@link LazySolrCore} (the strict and tolerant initializations
 * and the retry rules are tested in {@code LazySolrCoreTest}).
 *
 * @version $Id$
 */
@ComponentTest
class SolrEventStoreTest
{
    @InjectMockComponents
    private SolrEventStore store;

    @MockComponent
    private LazySolrCore eventsCore;

    private SolrClient client = mock(SolrClient.class);

    private XWikiSolrCore core = mock(XWikiSolrCore.class);

    @BeforeComponent("eventsCoreRequestedAgainAfterAFailure")
    void beforeEventsCoreRequestedAgainAfterAFailure() throws Exception
    {
        when(this.core.getClient()).thenReturn(this.client);

        // The events core is not available when the store is initialized (a remote Solr server which cannot be
        // reached), nor the first time it's needed, then it's available
        when(this.eventsCore.initialize(EventsSolrCoreInitializer.NAME)).thenReturn(null);
        when(this.eventsCore.get()).thenThrow(new SolrException("Solr is not reachable")).thenReturn(this.core);
    }

    @Test
    void eventsCoreRequestedAgainAfterAFailure() throws Exception
    {
        // The initialization does not fail when the events core is not available
        verify(this.eventsCore).initialize(EventsSolrCoreInitializer.NAME);
        verify(this.eventsCore, never()).get();

        DefaultEvent event = new DefaultEvent();
        event.setId("id");

        // The save fails while the events core is not available, and nothing is committed
        ExecutionException exception = assertThrows(ExecutionException.class, () -> this.store.saveEvent(event).get());
        assertInstanceOf(EventStreamException.class, exception.getCause());
        assertEquals("SolrException: Solr is not reachable", ExceptionUtils.getRootCauseMessage(exception));
        verifyNoInteractions(this.client);

        // The events core is available
        Event savedEvent = this.store.saveEvent(event).get();
        assertSame(event, savedEvent);
        verify(this.client).add(any(SolrInputDocument.class));
        verify(this.client).commit();

        // The client of the events core is kept once it was found
        this.store.saveEvent(event).get();
        verify(this.eventsCore, times(2)).get();
        verify(this.client, times(2)).add(any(SolrInputDocument.class));
        verify(this.client, times(2)).commit();
    }

    @Test
    void initializationFailure() throws Exception
    {
        // The events core cannot be loaded (with the embedded Solr for example): the initialization fails.
        SolrException failure = new SolrException("Failed to load the core");
        LazySolrCore failingEventsCore = mock();
        when(failingEventsCore.initialize(EventsSolrCoreInitializer.NAME)).thenThrow(failure);
        // Built by hand because the component manager would fail to inject it. Only the events core is set: any later
        // step of the initialization would fail on the missing fields, so this also checks that no store thread is
        // started when the events core cannot be loaded.
        SolrEventStore failingStore = new SolrEventStore();
        ReflectionUtils.setFieldValue(failingStore, "eventsCore", failingEventsCore);

        InitializationException exception = assertThrows(InitializationException.class, failingStore::initialize);
        assertEquals("Failed to get the events Solr core", exception.getMessage());
        assertSame(failure, exception.getCause());
    }
}
