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
package com.xpn.xwiki.internal.event;

import com.xpn.xwiki.objects.PropertyInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link XClassPropertyEventGeneratorListener}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
class XClassPropertyEventGeneratorListenerTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private XClassPropertyEventGeneratorListener listener;

    @MockComponent
    private ObservationManager observationManager;

    private XWikiDocument document;

    private XWikiDocument documentOrigin;

    private BaseClass xclass;

    private BaseClass xclassOrigin;

    @BeforeEach
    void beforeEach()
    {
        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.documentOrigin = new XWikiDocument(this.document.getDocumentReference());
        this.document.setOriginalDocument(this.documentOrigin);

        this.xclass = this.document.getXClass();
        this.xclassOrigin = this.documentOrigin.getXClass();
    }

    @Test
    void addDocument()
    {
        this.xclass.addTextField("property", "Property", 30);

        PropertyInterface newProp = this.xclass.getField("property");
        final Event propertyEvent = new XClassPropertyAddedEvent(newProp.getReference());

        this.listener.onEvent(new DocumentCreatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // We verify in order because it makes it easier to debug the test, but I suppose triggering XClassUpdatedEvent
        // before the PropertyAddedEvent would be fine.
        // Although a behavior change could theoretically be a subtle API breakage I suppose.
        InOrder inOrder = inOrder(this.observationManager);

        // Make sure the listener generated a property added event
        inOrder.verify(this.observationManager).notify(eq(propertyEvent), same(this.document), same(this.oldcore.getXWikiContext()));

        // Make sure the listener generated a class updated event
        final Event classEvent = new XClassUpdatedEvent(this.document.getDocumentReference());
        Collection<XClassUpdatedEvent.PropertyUpdate> propertyUpdates = List.of(
                new XClassUpdatedEvent.PropertyUpdate(null, newProp)
        );
        inOrder.verify(this.observationManager).notify(eq(classEvent), same(this.document), eq(propertyUpdates));
    }

    @Test
    void deleteDocument()
    {
        this.xclassOrigin.addTextField("property", "Property", 30);

        PropertyInterface oldProp = this.xclassOrigin.getField("property");
        final Event event = new XClassPropertyDeletedEvent(oldProp.getReference());

        this.listener.onEvent(new DocumentDeletedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // See the previous comment about verifying in order
        InOrder inOrder = inOrder(this.observationManager);

        // Make sure the listener generated a property deleted event
        inOrder.verify(this.observationManager).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));

        // Make sure the listener generated a class updated event
        final Event classEvent = new XClassUpdatedEvent(this.document.getDocumentReference());
        Collection<XClassUpdatedEvent.PropertyUpdate> propertyUpdates = List.of(
                new XClassUpdatedEvent.PropertyUpdate(oldProp, null)
        );
        inOrder.verify(this.observationManager).notify(eq(classEvent), same(this.document), eq(propertyUpdates));
    }

    @Test
    void modifiedDocumentXClassPropertyAdded()
    {
        this.xclass.addTextField("property", "Property", 30);

        PropertyInterface newProp = this.xclass.getField("property");
        final Event event = new XClassPropertyAddedEvent(newProp.getReference());

        this.listener.onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // See the previous comment about verifying in order
        InOrder inOrder = inOrder(this.observationManager);

        // Make sure the listener generated a property deleted event
        inOrder.verify(this.observationManager).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));

        // Make sure the listener generated a class updated event
        final Event classEvent = new XClassUpdatedEvent(this.document.getDocumentReference());
        Collection<XClassUpdatedEvent.PropertyUpdate> propertyUpdates = List.of(
                new XClassUpdatedEvent.PropertyUpdate(null, newProp)
        );
        inOrder.verify(this.observationManager).notify(eq(classEvent), same(this.document), eq(propertyUpdates));
    }

    @Test
    void modifiedDocumentXClassPropertyDeleted()
    {
        this.xclassOrigin.addTextField("property", "Property", 30);

        PropertyInterface oldProp = this.xclassOrigin.getField("property");
        final Event event = new XClassPropertyDeletedEvent(oldProp.getReference());

        this.listener.onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // See the previous comment about verifying in order
        InOrder inOrder = inOrder(this.observationManager);

        // Make sure the listener generated a property deleted event
        inOrder.verify(this.observationManager).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));

        // Make sure the listener generated a class updated event
        final Event classEvent = new XClassUpdatedEvent(this.document.getDocumentReference());
        Collection<XClassUpdatedEvent.PropertyUpdate> propertyUpdates = List.of(
                new XClassUpdatedEvent.PropertyUpdate(oldProp, null)
        );
        inOrder.verify(this.observationManager).notify(eq(classEvent), same(this.document), eq(propertyUpdates));
    }

    @Test
    void modifiedDocumentXClassPropertyModified()
    {
        this.xclassOrigin.addTextField("property", "Property", 30);
        PropertyInterface oldProp = this.xclassOrigin.getField("property");
        this.xclass.addTextField("property", "New Property", 30);
        PropertyInterface newProp = this.xclass.getField("property");

        final Event event = new XClassPropertyUpdatedEvent(this.xclassOrigin.getField("property").getReference());

        this.listener.onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // See the previous comment about verifying in order
        InOrder inOrder = inOrder(this.observationManager);

        // Make sure the listener generated a property updated event
        inOrder.verify(this.observationManager).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));

        // Make sure the listener generated a class updated event
        final Event classEvent = new XClassUpdatedEvent(this.document.getDocumentReference());
        Collection<XClassUpdatedEvent.PropertyUpdate> propertyUpdates = List.of(
                new XClassUpdatedEvent.PropertyUpdate(oldProp, newProp)
        );
        inOrder.verify(this.observationManager).notify(eq(classEvent), same(this.document), eq(propertyUpdates));
    }
}
