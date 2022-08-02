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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManagerEventListener;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.component.wiki.internal.WikiComponentManagerEventListenerHelper;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

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
public class DefaultWikiComponentManagerEventListenerTest implements WikiComponentConstants
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    @Rule
    public MockitoComponentMockingRule<EventListener> mocker = new MockitoComponentMockingRule<EventListener>(
        DefaultWikiComponentManagerEventListener.class);

    private WikiComponentManagerEventListenerHelper wikiComponentManagerEventListenerHelper;

    private ComponentManager componentManager;

    private WikiComponentBuilder wikiComponentBuilder;

    private WikiComponent wikiComponent;

    @Before
    public void setUp() throws Exception
    {
        this.wikiComponentManagerEventListenerHelper =
                this.mocker.registerMockComponent(WikiComponentManagerEventListenerHelper.class);

        this.wikiComponent = mock(WikiComponent.class);

        this.wikiComponentBuilder = mock(WikiComponentBuilder.class);
        when(this.wikiComponentBuilder.getDocumentReferences()).thenReturn(Arrays.asList(DOC_REFERENCE));
        when(this.wikiComponentBuilder.buildComponents(DOC_REFERENCE)).thenReturn(Arrays.asList(this.wikiComponent));

        this.componentManager = this.mocker.registerMockComponent(ComponentManager.class);
        when(this.componentManager.getInstanceList(WikiComponentBuilder.class))
                .thenReturn(Arrays.asList(this.wikiComponentBuilder));
    }

    @Test
    public void onEventWhenSourceIsNotAXWikiDocumentAndNoMatchingEvent() throws Exception
    {
        mocker.getComponentUnderTest().onEvent(null, null, null);

        verify(this.wikiComponentManagerEventListenerHelper, never()).registerComponentList(any());
        verify(this.wikiComponentManagerEventListenerHelper, never()).unregisterComponents(any(EntityReference.class));
    }

    @Test
    public void onEventWhenSourceDocumentButNoMatchingEvent() throws Exception
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);

        mocker.getComponentUnderTest().onEvent(null, componentDocument, null);

        verify(this.wikiComponentManagerEventListenerHelper, never()).registerComponentList(any());
        verify(this.wikiComponentManagerEventListenerHelper, never()).unregisterComponents(any(EntityReference.class));
    }

    @Test
    public void onDocumentCreated() throws Exception
    {
        onDocumentCreatedOrUpdated(new DocumentCreatedEvent(DOC_REFERENCE));

        verify(this.wikiComponentManagerEventListenerHelper, times(2))
                .registerComponentList(Arrays.asList(this.wikiComponent));
        verify(this.wikiComponentManagerEventListenerHelper, times(1))
                .unregisterComponents(any());
    }

    @Test
    public void onDocumentUpdated() throws Exception
    {
        onDocumentCreatedOrUpdated(new DocumentUpdatedEvent(DOC_REFERENCE));

        verify(this.wikiComponentManagerEventListenerHelper, times(2))
                .registerComponentList(Arrays.asList(this.wikiComponent));
        verify(this.wikiComponentManagerEventListenerHelper, times(1))
                .unregisterComponents(DOC_REFERENCE);
    }

    @Test
    public void onDocumentUpdatedWhenTwoComponents() throws Exception
    {
        // We create a second WikiComponent and register it against our standard WikiComponentBuilder
        WikiComponent secondWikiComponent = mock(WikiComponent.class);
        when(this.wikiComponentBuilder.buildComponents(DOC_REFERENCE))
                .thenReturn(Arrays.asList(this.wikiComponent, secondWikiComponent));

        this.onDocumentCreatedOrUpdated(new DocumentUpdatedEvent(DOC_REFERENCE));

        verify(this.wikiComponentManagerEventListenerHelper, times(2))
                .registerComponentList(Arrays.asList(this.wikiComponent, secondWikiComponent));
        verify(this.wikiComponentManagerEventListenerHelper, times(1))
                .unregisterComponents(DOC_REFERENCE);
    }

    @Test
    public void onDocumentDeleted() throws Exception
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);

        mocker.getComponentUnderTest().onEvent(new DocumentDeletedEvent(DOC_REFERENCE), componentDocument, null);

        verify(this.wikiComponentManagerEventListenerHelper, times(1))
                .unregisterComponents(DOC_REFERENCE);
    }

    @Test
    public void onApplicationReady() throws Exception
    {
        mocker.getComponentUnderTest().onEvent(new ApplicationReadyEvent(), null, null);

        verify(this.wikiComponentManagerEventListenerHelper, times(1))
                .registerComponentList(any());
    }

    private void onDocumentCreatedOrUpdated(Event event) throws Exception
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);

        /**
         * Here, {@link WikiComponentManagerEventListenerHelper#registerComponentList(List)} is called two times
         * because we have to "initialize" the tested event listener with an ApplicationReadyEvent() before sending
         * our custom event. Therefore, the tested WikiComponent will be registered two times.
         */
        mocker.getComponentUnderTest().onEvent(new ApplicationReadyEvent(), null, null);
        mocker.getComponentUnderTest().onEvent(event, componentDocument, null);
    }

}
