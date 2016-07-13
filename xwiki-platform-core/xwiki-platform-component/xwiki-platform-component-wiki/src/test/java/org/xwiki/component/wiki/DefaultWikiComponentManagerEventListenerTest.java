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
import org.xwiki.component.wiki.internal.DefaultWikiComponent;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManagerEventListener;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWikiComponentManagerEventListener}.
 *
 * @version $Id$
 */
public class DefaultWikiComponentManagerEventListenerTest implements WikiComponentConstants
{
    private static final String ROLE_HINT = "roleHint";

    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    @Rule
    public MockitoComponentMockingRule<EventListener> mocker = new MockitoComponentMockingRule<EventListener>(
        DefaultWikiComponentManagerEventListener.class);

    private WikiComponentBuilder wikiComponentBuilder;

    private WikiComponentManager wikiComponentManager;

    @Before
    public void setUp() throws Exception
    {
        wikiComponentBuilder = mocker.registerMockComponent(WikiComponentBuilder.class);
        wikiComponentManager = mocker.getInstance(WikiComponentManager.class);
    }

    @Test
    public void onEventWhenSourceIsNotAXWikiDocumentAndNoMatchingEvent() throws Exception
    {
        mocker.getComponentUnderTest().onEvent(null, null, null);

        verify(wikiComponentManager, never()).registerWikiComponent(any(WikiComponent.class));
        verify(wikiComponentManager, never()).unregisterWikiComponents(any(DocumentReference.class));
    }

    @Test
    public void onEventWhenSourceDocumentButNoMatchingEvent() throws Exception
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);

        mocker.getComponentUnderTest().onEvent(null, componentDocument, null);

        verify(wikiComponentManager, never()).registerWikiComponent(any(WikiComponent.class));
        verify(wikiComponentManager, never()).unregisterWikiComponents(any(DocumentReference.class));
    }

    @Test
    public void onDocumentCreated() throws Exception
    {
        onDocumentCreatedOrUpdated(new DocumentCreatedEvent(DOC_REFERENCE));
    }

    @Test
    public void onDocumentUpdated() throws Exception
    {
        onDocumentCreatedOrUpdated(new DocumentUpdatedEvent(DOC_REFERENCE));
    }

    @Test
    public void onDocumentUpdatedWhenTwoComponents() throws Exception
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);
        when(wikiComponentBuilder.getDocumentReferences()).thenReturn(Arrays.asList(DOC_REFERENCE));

        WikiComponent component =
            new DefaultWikiComponent(DOC_REFERENCE, AUTHOR_REFERENCE, TestRole.class, ROLE_HINT,
                WikiComponentScope.WIKI);
        when(wikiComponentBuilder.buildComponents(DOC_REFERENCE)).thenReturn(Arrays.asList(component, component));

        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(DOC_REFERENCE), componentDocument, null);

        verify(wikiComponentManager, times(1)).unregisterWikiComponents(DOC_REFERENCE);
        verify(wikiComponentManager, times(2)).registerWikiComponent(component);
    }

    @Test
    public void onDocumentDeleted() throws Exception
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);

        mocker.getComponentUnderTest().onEvent(new DocumentDeletedEvent(DOC_REFERENCE), componentDocument, null);

        verify(wikiComponentManager, times(1)).unregisterWikiComponents(DOC_REFERENCE);
    }

    @Test
    public void onApplicationReady() throws Exception
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);
        when(wikiComponentBuilder.getDocumentReferences()).thenReturn(Arrays.asList(DOC_REFERENCE));

        WikiComponent component =
            new DefaultWikiComponent(DOC_REFERENCE, AUTHOR_REFERENCE, TestRole.class, ROLE_HINT,
                WikiComponentScope.WIKI);
        when(wikiComponentBuilder.buildComponents(DOC_REFERENCE)).thenReturn(Arrays.asList(component));

        mocker.getComponentUnderTest().onEvent(new ApplicationReadyEvent(), null, null);

        verify(wikiComponentManager, times(1)).registerWikiComponent(component);
    }

    private void onDocumentCreatedOrUpdated(Event event) throws Exception
    {
        DocumentModelBridge componentDocument = mock(DocumentModelBridge.class);
        when(componentDocument.getDocumentReference()).thenReturn(DOC_REFERENCE);
        when(wikiComponentBuilder.getDocumentReferences()).thenReturn(Arrays.asList(DOC_REFERENCE));

        WikiComponent component =
            new DefaultWikiComponent(DOC_REFERENCE, AUTHOR_REFERENCE, TestRole.class, ROLE_HINT,
                WikiComponentScope.WIKI);
        when(wikiComponentBuilder.buildComponents(DOC_REFERENCE)).thenReturn(Arrays.asList(component));

        mocker.getComponentUnderTest().onEvent(event, componentDocument, null);

        verify(wikiComponentManager, times(1)).unregisterWikiComponents(DOC_REFERENCE);
        verify(wikiComponentManager, times(1)).registerWikiComponent(component);
    }

}
