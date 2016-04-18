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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link XObjectEventGeneratorListener}.
 * 
 * @version $Id$
 */
@ComponentList({
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class,
    DefaultStringEntityReferenceSerializer.class,
    DefaultStringDocumentReferenceResolver.class,
    DefaultStringEntityReferenceResolver.class,
    DefaultEntityReferenceProvider.class,
    CurrentStringDocumentReferenceResolver.class,
    CurrentStringEntityReferenceResolver.class,
    LocalStringEntityReferenceSerializer.class,
    DefaultSymbolScheme.class
})
public class XObjectEventGeneratorListenerTest
{
    public MockitoComponentMockingRule<XObjectEventGeneratorListener> mocker =
        new MockitoComponentMockingRule<XObjectEventGeneratorListener>(XObjectEventGeneratorListener.class);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(mocker);

    private ObservationManager mockObservation;

    private XWikiDocument document;

    private XWikiDocument documentOrigin;

    private BaseObject xobject;

    @Before
    public void before() throws Exception
    {
        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.documentOrigin = new XWikiDocument(this.document.getDocumentReference());
        this.document.setOriginalDocument(this.documentOrigin);

        this.xobject = new BaseObject();
        this.xobject.setXClassReference(this.document.getDocumentReference());

        this.mockObservation = this.mocker.getInstance(ObservationManager.class);
    }

    @Test
    public void testAddDocument() throws ComponentLookupException
    {
        this.document.addXObject(this.xobject);

        final Event event = new XObjectAddedEvent(this.xobject.getReference());

        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a xobject added event
        verify(this.mockObservation).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testDeleteDocument() throws ComponentLookupException
    {
        this.documentOrigin.addXObject(this.xobject);

        final Event event = new XObjectDeletedEvent(this.xobject.getReference());

        this.mocker.getComponentUnderTest().onEvent(new DocumentDeletedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a xobject deleted event
        verify(this.mockObservation).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testModifiedDocumentXObjectAdded() throws ComponentLookupException
    {
        this.document.addXObject(this.xobject);

        final Event event = new XObjectAddedEvent(this.xobject.getReference());

        this.mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a xobject added event
        verify(this.mockObservation).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testModifiedDocumentXObjectDeleted() throws ComponentLookupException
    {
        this.documentOrigin.addXObject(this.xobject);

        final Event event = new XObjectDeletedEvent(this.xobject.getReference());

        this.mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a xobject deleted event
        verify(this.mockObservation).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testModifiedDocumentXObjectModified() throws ComponentLookupException
    {
        this.document.addXObject(this.xobject);
        this.documentOrigin.addXObject(this.xobject.clone());

        this.xobject.setStringValue("newproperty", "newvalue");

        final Event event = new XObjectUpdatedEvent(this.xobject.getReference());

        this.mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a xobject updated event
        verify(this.mockObservation).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testModifiedDocumentXObjectPropertyAdded() throws ComponentLookupException
    {
        this.document.addXObject(this.xobject);
        this.documentOrigin.addXObject(this.xobject.clone());

        this.xobject.setStringValue("newproperty", "newvalue");

        final Event event = new XObjectPropertyAddedEvent(this.xobject.getField("newproperty").getReference());

        this.mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a xobject property added event
        verify(this.mockObservation).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testModifiedDocumentXObjectPropertyDeleted() throws ComponentLookupException
    {
        this.document.addXObject(this.xobject.clone());
        this.documentOrigin.addXObject(this.xobject);

        this.xobject.setStringValue("deletedproperty", "deletedvalue");

        final Event event = new XObjectPropertyDeletedEvent(this.xobject.getField("deletedproperty").getReference());

        this.mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a xobject deleted event
        verify(this.mockObservation).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testModifiedDocumentXObjectPropertyModified() throws ComponentLookupException
    {
        this.xobject.setStringValue("updatedproperty", "propertyvalue");

        BaseObject xobjectModified = this.xobject.clone();
        xobjectModified.setStringValue("updatedproperty", "propertyvaluemodified");

        this.document.addXObject(this.xobject);
        this.documentOrigin.addXObject(xobjectModified);

        final Event event = new XObjectPropertyUpdatedEvent(this.xobject.getField("updatedproperty").getReference());

        this.mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(this.document.getDocumentReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a xobject property updated event
        verify(this.mockObservation).notify(eq(event), same(this.document), same(this.oldcore.getXWikiContext()));
    }
}
