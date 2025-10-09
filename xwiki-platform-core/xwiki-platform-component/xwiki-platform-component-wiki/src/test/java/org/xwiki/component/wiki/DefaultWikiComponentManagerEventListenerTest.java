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
package org.xwiki.component.wiki;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManagerEventListener;
import org.xwiki.component.wiki.internal.WikiComponentManagerEventListenerHelper;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWikiComponentManagerEventListener}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultWikiComponentManagerEventListenerTest
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    @InjectMockComponents
    private DefaultWikiComponentManagerEventListener listener;

    @MockComponent
    private WikiComponentManagerEventListenerHelper wikiComponentManagerEventListenerHelper;

    @MockComponent
    private WikiComponentBuilder wikiComponentBuilder;

    @MockComponent
    private WikiComponent wikiComponent;

    @BeforeEach
    void setUp(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        ComponentManager componentManager = mockitoComponentManager.registerMockComponent(ComponentManager.class);
        when(this.wikiComponentBuilder.getDocumentReferences()).thenReturn(List.of(DOC_REFERENCE));
        when(this.wikiComponentBuilder.buildComponents(DOC_REFERENCE)).thenReturn(List.of(this.wikiComponent));
        when(componentManager.getInstanceList(WikiComponentBuilder.class))
            .thenReturn(List.of(this.wikiComponentBuilder));
    }

    @Test
    void onEventWhenSourceIsNotAXWikiDocumentAndNoMatchingEvent()
    {
        this.listener.onEvent(null, null, null);

        verify(this.wikiComponentManagerEventListenerHelper, never()).registerComponentList(any());
        verify(this.wikiComponentManagerEventListenerHelper, never()).unregisterComponents(any(EntityReference.class));
    }

    @Test
    void onEventWhenSourceDocumentButNoMatchingEvent()
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);

        this.listener.onEvent(null, componentDocument, null);

        verify(this.wikiComponentManagerEventListenerHelper, never()).registerComponentList(any());
        verify(this.wikiComponentManagerEventListenerHelper, never()).unregisterComponents(any(EntityReference.class));
    }

    @Test
    void onDocumentCreated()
    {
        onDocumentCreatedOrUpdated(new DocumentCreatedEvent(DOC_REFERENCE));

        verify(this.wikiComponentManagerEventListenerHelper, times(2))
            .registerComponentList(List.of(this.wikiComponent));
        verify(this.wikiComponentManagerEventListenerHelper).unregisterComponents(any());
    }

    @Test
    void onDocumentUpdated()
    {
        onDocumentCreatedOrUpdated(new DocumentUpdatedEvent(DOC_REFERENCE));

        verify(this.wikiComponentManagerEventListenerHelper, times(2))
            .registerComponentList(List.of(this.wikiComponent));
        verify(this.wikiComponentManagerEventListenerHelper).unregisterComponents(DOC_REFERENCE);
    }

    @Test
    void onDocumentUpdatedWhenTwoComponents() throws Exception
    {
        // We create a second WikiComponent and register it against our standard WikiComponentBuilder
        WikiComponent secondWikiComponent = mock(WikiComponent.class);
        when(this.wikiComponentBuilder.buildComponents(DOC_REFERENCE))
            .thenReturn(List.of(this.wikiComponent, secondWikiComponent));

        this.onDocumentCreatedOrUpdated(new DocumentUpdatedEvent(DOC_REFERENCE));

        verify(this.wikiComponentManagerEventListenerHelper, times(2))
            .registerComponentList(List.of(this.wikiComponent, secondWikiComponent));
        verify(this.wikiComponentManagerEventListenerHelper).unregisterComponents(DOC_REFERENCE);
    }

    @Test
    void onDocumentDeleted()
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);

        this.listener.onEvent(new DocumentDeletedEvent(DOC_REFERENCE), componentDocument, null);

        verify(this.wikiComponentManagerEventListenerHelper).unregisterComponents(DOC_REFERENCE);
    }

    @Test
    void onApplicationReady()
    {
        this.listener.onEvent(new ApplicationReadyEvent(), null, null);

        verify(this.wikiComponentManagerEventListenerHelper).registerComponentList(any());
    }

    private void onDocumentCreatedOrUpdated(Event event)
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);

        /**
         * Here, {@link WikiComponentManagerEventListenerHelper#registerComponentList(List)} is called two times
         * because we have to "initialize" the tested event listener with an ApplicationReadyEvent() before sending
         * our custom event. Therefore, the tested WikiComponent will be registered two times.
         */
        this.listener.onEvent(new ApplicationReadyEvent(), null, null);
        this.listener.onEvent(event, componentDocument, null);
    }
}
