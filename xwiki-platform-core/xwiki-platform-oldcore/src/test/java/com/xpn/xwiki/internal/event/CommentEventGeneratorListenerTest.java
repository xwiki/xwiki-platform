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

        this.observation = getComponentManager().lookup(ObservationManager.class);

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
