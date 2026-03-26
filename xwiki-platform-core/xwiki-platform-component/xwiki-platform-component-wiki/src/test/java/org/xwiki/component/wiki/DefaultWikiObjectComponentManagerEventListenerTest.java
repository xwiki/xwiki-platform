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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.component.wiki.internal.bridge.WikiObjectComponentManagerEventListenerProxy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWikiObjectComponentManagerEventListener}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@ComponentTest
class DefaultWikiObjectComponentManagerEventListenerTest
{
    @InjectMockComponents
    private DefaultWikiObjectComponentManagerEventListener listener;

    @MockComponent
    private WikiObjectComponentManagerEventListenerProxy wikiObjectComponentManagerEventListenerProxy;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    private final List<EntityReference> xClassReferences = List.of(
        new DocumentReference("wiki1", "space1", "xClass1"),
        new DocumentReference("wiki1", "space1", "xClass2"));

    private ComponentManager componentManager;

    @BeforeEach
    void setUp(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        when(this.wikiObjectComponentManagerEventListenerProxy.getWikiObjectsList())
            .thenReturn(List.of(
                new LocalDocumentReference(this.xClassReferences.get(0)),
                this.xClassReferences.get(1)));
        this.componentManager = mockitoComponentManager.getInstance(ComponentManager.class, "context");
    }

    @Test
    void supportedEvents()
    {
        List<Event> events = this.listener.getEvents();

        assertEquals(5, events.size());
    }

    @Test
    void correctListenerName()
    {
        assertEquals("defaultWikiObjectComponentManagerEventListener",
            this.listener.getName());
    }

    @Test
    void testComponentInitializationOnApplicationReady()
    {
        this.listener.onEvent(new WikiReadyEvent(), null, null);

        verify(this.wikiObjectComponentManagerEventListenerProxy).registerAllObjectComponents();
    }

    @Test
    void testComponentInitializationOnWikiReady()
    {
        this.listener.onEvent(new WikiReadyEvent(), null, null);

        verify(this.wikiObjectComponentManagerEventListenerProxy).registerAllObjectComponents();
    }

    @Test
    void onEventWithIncompatibleEvent()
    {
        this.listener.onEvent(new XObjectAddedEvent(), null, null);

        verify(this.wikiObjectComponentManagerEventListenerProxy, never()).registerAllObjectComponents();
        verify(this.wikiObjectComponentManagerEventListenerProxy, never()).unregisterObjectComponents(any());
        verify(this.wikiObjectComponentManagerEventListenerProxy, never())
            .registerObjectComponents(any(), any(), any());
    }

    @Test
    void onDocumentCreatedEvent() throws Exception
    {
        this.verifyXObjectAddOrUpdate(mock(DocumentCreatedEvent.class));
    }

    @Test
    void onDocumentUpdatedEvent() throws Exception
    {
        this.verifyXObjectAddOrUpdate(mock(DocumentUpdatedEvent.class));
    }

    @Test
    void onDocumentDeletedEvent() throws Exception
    {
        DocumentDeletedEvent event = mock(DocumentDeletedEvent.class);
        XWikiDocument source = mock(XWikiDocument.class);
        XWikiDocument oldXObjectDocument = mock(XWikiDocument.class);

        BaseObject xObject = mock(BaseObject.class);
        BaseObjectReference xObjectReference = mock(BaseObjectReference.class);

        when(source.getOriginalDocument()).thenReturn(oldXObjectDocument);
        when(xObject.getReference()).thenReturn(xObjectReference);

        DocumentReference xObjectClassReference = (DocumentReference) this.xClassReferences.get(0);
        Map<DocumentReference, List<BaseObject>> fakeDocumentXObjects = new HashMap<>();
        fakeDocumentXObjects.put(xObjectClassReference, Collections.singletonList(xObject));

        when(oldXObjectDocument.getXObjects()).thenReturn(fakeDocumentXObjects);
        mockAssociatedComponentBuilderMethod(xObject);

        this.listener.onEvent(event, source, null);

        verify(this.wikiObjectComponentManagerEventListenerProxy)
            .unregisterObjectComponents(xObjectReference);
    }

    private WikiObjectComponentBuilder mockAssociatedComponentBuilderMethod(BaseObject xObject) throws Exception
    {
        WikiObjectComponentBuilder builder = mock(WikiObjectComponentBuilder.class);

        DocumentReference xClassReference = (DocumentReference) this.xClassReferences.get(0);
        when(xObject.getXClassReference()).thenReturn(xClassReference);

        when(this.defaultEntityReferenceSerializer.serialize(xClassReference)).thenReturn("xwiki1:space1.xClass1");
        when(this.localEntityReferenceSerializer.serialize(xClassReference)).thenReturn("space1.xClass1");

        when(this.componentManager.hasComponent(WikiObjectComponentBuilder.class, "xwiki1:space1.xClass1"))
            .thenReturn(false);
        when(this.componentManager.hasComponent(WikiObjectComponentBuilder.class, "space1.xClass1"))
            .thenReturn(true);
        when(this.componentManager.getInstance(WikiObjectComponentBuilder.class, "space1.xClass1"))
            .thenReturn(builder);

        return builder;
    }

    private void verifyXObjectAddOrUpdate(AbstractDocumentEvent event) throws Exception
    {
        XWikiDocument fakeSource = mock(XWikiDocument.class);
        XWikiDocument fakeOldSource = mock(XWikiDocument.class);

        BaseObject xObject = mock(BaseObject.class);
        BaseObjectReference xObjectReference = mock(BaseObjectReference.class);
        DocumentReference xObjectClassReference = (DocumentReference) this.xClassReferences.get(0);

        when(xObject.getReference()).thenReturn(xObjectReference);

        Map<DocumentReference, List<BaseObject>> fakeDocumentXObjects = new HashMap<>();
        fakeDocumentXObjects.put(xObjectClassReference, Collections.singletonList(xObject));

        when(fakeSource.getXObjects()).thenReturn(fakeDocumentXObjects);
        when(fakeSource.getOriginalDocument()).thenReturn(fakeOldSource);

        WikiObjectComponentBuilder builder = mockAssociatedComponentBuilderMethod(xObject);

        this.listener.onEvent(event, fakeSource, null);

        verify(this.wikiObjectComponentManagerEventListenerProxy)
            .registerObjectComponents(xObjectReference, xObject, builder);
    }
}
