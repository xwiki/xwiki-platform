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
 * Validate {@link CommentEventGeneratorListener}.
 * 
 * @version $Id$
 */
public class CommentEventGeneratorListenerTest extends AbstractBridgedComponentTestCase
{
    private ObservationManager observation;

    private XWiki xwiki;

    private XWikiDocument commentXClassDocument;
    
    private BaseClass commentXClass;
    
    private BaseObject commentXObject;
    
    private XWikiDocument document;

    private XWikiDocument documentOrigin;
    
    private EventListener listener;
    

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

        this.commentXClassDocument = new XWikiDocument(new DocumentReference("wiki", "XWiki", "XWikiComments"));
        this.commentXClass = this.commentXClassDocument.getXClass();
        this.commentXClass.addTextAreaField("comment", "comment", 60, 20);

        this.commentXObject = new BaseObject();
        this.commentXObject.setXClassReference(this.commentXClass.getDocumentReference());

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.documentOrigin = new XWikiDocument(this.document.getDocumentReference());
        this.document.setOriginalDocument(this.documentOrigin);
        
        getMockery().checking(new Expectations() {{
            allowing(listener).getName(); will(returnValue("mylistener"));
            allowing(xwiki).getXClass(commentXClass.getDocumentReference(), getContext()); will(returnValue(commentXClass));
        }});
    }

    @Test
    public void testAddComment()
    {
        this.document.addXObject(this.commentXObject);

        final Event event = new CommentAddedEvent("wiki:space.page", "0");

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(any(event.getClass())), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentCreatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testDeleteComment()
    {
        this.documentOrigin.addXObject(this.commentXObject);

        final Event event = new CommentDeletedEvent("wiki:space.page", "0");

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(any(event.getClass())), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);
 
        this.observation.notify(new DocumentDeletedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedComment()
    {
        this.document.addXObject(this.commentXObject);
        this.documentOrigin.addXObject((BaseObject) this.commentXObject.clone());
        
        this.commentXObject.setStringValue("comment", "comment");

        final Event event = new CommentUpdatedEvent("wiki:space.page", "0");

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(any(event.getClass())), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testNotComment()
    {
        this.commentXObject = new BaseObject();
        this.commentXObject.setXClassReference(new DocumentReference("wiki", "XWiki", "XWikkiComments2"));
        
        this.document.addXObject(this.commentXObject);

        final Event event = new CommentAddedEvent("wiki:space.page", "0");

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentCreatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }
}
