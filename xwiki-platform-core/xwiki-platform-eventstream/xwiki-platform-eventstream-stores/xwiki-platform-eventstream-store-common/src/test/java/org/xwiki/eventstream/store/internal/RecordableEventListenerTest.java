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
package org.xwiki.eventstream.store.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RecordableEventListener}
 */
@ComponentTest
class RecordableEventListenerTest
{
    @InjectMockComponents
    private RecordableEventListener listener;

    @MockComponent
    private EventStore eventStore;

    @MockComponent
    private RecordableEventConverter defaultConverter;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @MockComponent
    private ObservationContext observationContext;

    private ComponentManager contextComponentManager;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        this.contextComponentManager = componentManager.registerMockComponent(ComponentManager.class, "context");
    }

    @Test
    void onEvent() throws Exception
    {
        Event event = mock(Event.class);
        this.listener.onEvent(event, null, null);

        verify(this.eventStore, never()).saveEvent(any());
        verify(this.contextComponentManager, never()).getInstanceList(any());

        event = mock(RecordableEvent.class);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);

        this.listener.onEvent(event, null, null);

        verify(this.eventStore, never()).saveEvent(any());
        verify(this.contextComponentManager, never()).getInstanceList(any());

        event = mock(BeginFoldEvent.class);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);

        this.listener.onEvent(event, null, null);
        verify(this.eventStore, never()).saveEvent(any());
        verify(this.contextComponentManager, never()).getInstanceList(any());

        event = mock(DocumentCreatedEvent.class);

        this.listener.onEvent(event, null, null);
        verify(this.eventStore, never()).saveEvent(any());
        verify(this.contextComponentManager, never()).getInstanceList(any());

        RecordableEvent recordableEvent = mock(RecordableEvent.class);
        org.xwiki.eventstream.Event convertedEvent = mock(org.xwiki.eventstream.Event.class);

        RecordableEventConverter converter1 = mock(RecordableEventConverter.class);
        RecordableEventConverter converter2 = mock(RecordableEventConverter.class);

        when(this.contextComponentManager.getInstanceList(RecordableEventConverter.class))
            .thenReturn(Arrays.asList(this.defaultConverter, converter1, converter2));
        RecordableEvent recordableEvent1 = otherEvent -> false;
        when(converter1.getSupportedEvents()).thenReturn(Collections.singletonList(recordableEvent1));
        RecordableEvent recordableEvent2 = otherEvent -> true;
        when(converter2.getSupportedEvents()).thenReturn(Collections.singletonList(recordableEvent2));

        String source = "something";
        String data = "another thing";
        when(converter2.convert(recordableEvent, source, data)).thenReturn(convertedEvent);
        CompletableFuture<org.xwiki.eventstream.Event> future = new CompletableFuture<>();
        future.complete(convertedEvent);
        when(eventStore.saveEvent(convertedEvent)).thenReturn(future);

        this.listener.onEvent(recordableEvent, source, data);
        verify(eventStore).saveEvent(convertedEvent);

        when(converter2.getSupportedEvents()).thenReturn(Collections.singletonList(recordableEvent1));
        org.xwiki.eventstream.Event convertedEvent2 = mock(org.xwiki.eventstream.Event.class);
        when(this.defaultConverter.convert(recordableEvent, source, data)).thenReturn(convertedEvent2);
        when(eventStore.saveEvent(convertedEvent2)).thenReturn(future);

        this.listener.onEvent(recordableEvent, source, data);
        verify(eventStore).saveEvent(convertedEvent2);
    }
}
