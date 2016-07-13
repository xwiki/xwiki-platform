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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link CommentEventGeneratorListener}.
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
    DefaultSymbolScheme.class
})
public class CommentEventGeneratorListenerTest
{
    public MockitoComponentMockingRule<CommentEventGeneratorListener> mocker =
        new MockitoComponentMockingRule<CommentEventGeneratorListener>(CommentEventGeneratorListener.class);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(mocker);

    private ObservationManager mockObservation;

    private XWikiDocument commentXClassDocument;

    private BaseClass commentXClass;

    private BaseObject commentXObject;

    private XWikiDocument document;

    private XWikiDocument documentOrigin;

    @Before
    public void before() throws Exception
    {
        this.commentXClassDocument = new XWikiDocument(new DocumentReference("wiki", "XWiki", "XWikiComments"));
        this.commentXClass = this.commentXClassDocument.getXClass();
        this.commentXClass.addTextAreaField("comment", "comment", 60, 20);

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.documentOrigin = new XWikiDocument(this.document.getDocumentReference());
        this.document.setOriginalDocument(this.documentOrigin);

        this.commentXObject = new BaseObject();
        this.commentXObject.setXClassReference(this.commentXClass.getDocumentReference());

        this.mockObservation = this.mocker.getInstance(ObservationManager.class);
    }

    @Test
    public void testAddComment() throws ComponentLookupException
    {
        this.document.addXObject(this.commentXObject);

        final Event event = new CommentAddedEvent("wiki:space.page", "0");

        this.mocker.getComponentUnderTest().onEvent(new XObjectAddedEvent(this.commentXObject.getReference()), this.document,
            this.oldcore.getXWikiContext());

        // Make sure the listener generated a comment added event
        verify(this.mockObservation)
            .notify(any(event.getClass()), same(document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testDeleteComment() throws ComponentLookupException
    {
        this.documentOrigin.addXObject(this.commentXObject);

        final Event event = new CommentDeletedEvent("wiki:space.page", "0");

        this.mocker.getComponentUnderTest().onEvent(new XObjectDeletedEvent(this.commentXObject.getReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a comment deleted event
        verify(this.mockObservation)
            .notify(any(event.getClass()), same(document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    public void testModifiedComment() throws ComponentLookupException
    {
        this.document.addXObject(this.commentXObject);
        this.documentOrigin.addXObject(this.commentXObject.clone());

        this.commentXObject.setStringValue("comment", "comment");

        final Event event = new CommentUpdatedEvent("wiki:space.page", "0");

        this.mocker.getComponentUnderTest().onEvent(new XObjectUpdatedEvent(this.commentXObject.getReference()),
            this.document, this.oldcore.getXWikiContext());

        // Make sure the listener generated a comment updated event
        verify(this.mockObservation)
            .notify(any(event.getClass()), same(document), same(this.oldcore.getXWikiContext()));
    }
}
