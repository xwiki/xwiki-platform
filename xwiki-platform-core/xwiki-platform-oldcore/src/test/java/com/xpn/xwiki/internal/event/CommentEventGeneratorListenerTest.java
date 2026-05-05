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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link CommentEventGeneratorListener}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
class CommentEventGeneratorListenerTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private CommentEventGeneratorListener listener;

    @MockComponent
    private ObservationManager observationManager;

    private XWikiDocument commentXClassDocument;

    private BaseClass commentXClass;

    private BaseObject commentXObject;

    private XWikiDocument document;

    private XWikiDocument documentOrigin;

    @BeforeEach
    void beforeEach()
    {
        this.commentXClassDocument = new XWikiDocument(new DocumentReference("wiki", "XWiki", "XWikiComments"));
        this.commentXClass = this.commentXClassDocument.getXClass();
        this.commentXClass.addTextAreaField("comment", "comment", 60, 20);

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.documentOrigin = new XWikiDocument(this.document.getDocumentReference());
        this.document.setOriginalDocument(this.documentOrigin);

        this.commentXObject = new BaseObject();
        this.commentXObject.setXClassReference(this.commentXClass.getDocumentReference());
    }

    @Test
    void addComment()
    {
        this.document.addXObject(this.commentXObject);

        final Event event = new CommentAddedEvent("wiki:space.page", "0");

        this.listener.onEvent(new XObjectAddedEvent(this.commentXObject.getReference()), this.document,
            this.oldcore.getXWikiContext());

        // Make sure the listener generated a comment added event
        verify(this.observationManager)
            .notify(any(event.getClass()), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    void deleteComment()
    {
        this.documentOrigin.addXObject(this.commentXObject);

        final Event event = new CommentDeletedEvent("wiki:space.page", "0");

        this.listener.onEvent(new XObjectDeletedEvent(this.commentXObject.getReference()), this.document,
            this.oldcore.getXWikiContext());

        // Make sure the listener generated a comment deleted event
        verify(this.observationManager)
            .notify(any(event.getClass()), same(this.document), same(this.oldcore.getXWikiContext()));
    }

    @Test
    void modifiedComment()
    {
        this.document.addXObject(this.commentXObject);
        this.documentOrigin.addXObject(this.commentXObject.clone());

        this.commentXObject.setStringValue("comment", "comment");

        final Event event = new CommentUpdatedEvent("wiki:space.page", "0");

        this.listener.onEvent(new XObjectUpdatedEvent(this.commentXObject.getReference()), this.document,
            this.oldcore.getXWikiContext());

        // Make sure the listener generated a comment updated event
        verify(this.observationManager)
            .notify(any(event.getClass()), same(this.document), same(this.oldcore.getXWikiContext()));
    }
}
