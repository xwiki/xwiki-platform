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

import java.util.Arrays;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Validate {@link XObjectEventGeneratorListener}.
 * 
 * @version $Id$
 */
public class XObjectEventGeneratorListenerTest extends AbstractBridgedComponentTestCase
{
    private ObservationManager observation;

    private XWiki xwiki;

    private XWikiDocument document;

    private XWikiDocument documentOrigin;
    
    private EventListener listener;
    
    private BaseObject xobject;
    
    private BaseClass xclass;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.observation = getComponentManager().getInstance(ObservationManager.class);

        // Remove wiki macro listener which is useless and try to load documents from database
        this.observation.removeListener("wikimacrolistener");

        this.xwiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.xwiki);
        
        this.listener = getMockery().mock(EventListener.class);

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.documentOrigin = new XWikiDocument(this.document.getDocumentReference());
        this.document.setOriginalDocument(this.documentOrigin);
        
        this.xclass = this.document.getXClass();
        
        this.xobject = new BaseObject();
        this.xobject.setXClassReference(this.document.getDocumentReference());
        
        getMockery().checking(new Expectations() {{
            allowing(listener).getName(); will(returnValue("mylistener"));
            allowing(xwiki).getXClass(xclass.getDocumentReference(), getContext()); will(returnValue(xclass));
        }});
    }

    @Test
    public void testAddDocument()
    {
        this.document.addXObject(this.xobject);

        final Event event = new XObjectAddedEvent(this.xobject.getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentCreatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testDeleteDocument()
    {
        this.documentOrigin.addXObject(this.xobject);

        final Event event = new XObjectDeletedEvent(this.xobject.getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);
 
        this.observation.notify(new DocumentDeletedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedDocumentXObjectAdded()
    {
        this.document.addXObject(this.xobject);

        final Event event = new XObjectAddedEvent(this.xobject.getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedDocumentXObjectDeleted()
    {
        this.documentOrigin.addXObject(this.xobject);

        final Event event = new XObjectDeletedEvent(this.xobject.getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);
 
        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedDocumentXObjectModified()
    {
        this.document.addXObject(this.xobject);
        this.documentOrigin.addXObject((BaseObject) this.xobject.clone());
        
        this.xobject.setStringValue("newproperty", "newvalue");

        final Event event = new XObjectUpdatedEvent(this.xobject.getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedDocumentXObjectPropertyAdded()
    {
        this.document.addXObject(this.xobject);
        this.documentOrigin.addXObject((BaseObject) this.xobject.clone());
        
        this.xobject.setStringValue("newproperty", "newvalue");

        final Event event = new XObjectPropertyAddedEvent(this.xobject.getField("newproperty").getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedDocumentXObjectPropertyDeleted()
    {
        this.document.addXObject((BaseObject) this.xobject.clone());
        this.documentOrigin.addXObject(this.xobject);
        
        this.xobject.setStringValue("deletedproperty", "deletedvalue");

        final Event event = new XObjectPropertyDeletedEvent(this.xobject.getField("deletedproperty").getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedDocumentXObjectPropertyModified()
    {
        this.xobject.setStringValue("updatedproperty", "propertyvalue");
     
        BaseObject xobjectModified = (BaseObject) this.xobject.clone();
        xobjectModified.setStringValue("updatedproperty", "propertyvaluemodified");
        
        this.document.addXObject(this.xobject);
        this.documentOrigin.addXObject(xobjectModified);

        final Event event = new XObjectPropertyUpdatedEvent(this.xobject.getField("updatedproperty").getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }
}
