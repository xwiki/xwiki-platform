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
package org.xwiki.mentions.internal.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.mentions.MentionsEventExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;

import ch.qos.logback.classic.Level;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.AWM_FIELD;
import static org.xwiki.mentions.MentionLocation.COMMENT;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;
import static org.xwiki.test.LogLevel.DEBUG;

/**
 * Test of {@link MentionsUpdatedEventListener}.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@ComponentTest
public class MentionsUpdatedEventListenerTest
{
    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(DEBUG);

    @InjectMockComponents
    private MentionsUpdatedEventListener listener;

    @Mock
    private XWikiDocument newDoc;

    @Mock
    private XWikiDocument oldDoc;

    @Mock
    private XWikiContext context;

    @MockComponent
    private MentionsEventExecutor executor;

    @Test
    void onEventNewObjects()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Author");
        DocumentUpdatedEvent event = new DocumentUpdatedEvent(documentReference);
        XDOM oldXDOM = new XDOM(emptyList());
        XDOM newXDOM = new XDOM(emptyList());
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        BaseObject newComment = mock(BaseObject.class);
        BaseObject newAnnotation = mock(BaseObject.class);
        BaseObject newField = mock(BaseObject.class);

        newXObjects.put(documentReference, Arrays.asList(newComment, newAnnotation, newField));

        when(this.newDoc.getOriginalDocument()).thenReturn(this.oldDoc);
        when(this.context.getUserReference()).thenReturn(authorReference);
        when(this.oldDoc.getXDOM()).thenReturn(oldXDOM);
        when(this.oldDoc.getXObjects()).thenReturn(oldXObjects);
        when(this.newDoc.getXDOM()).thenReturn(newXDOM);
        when(this.newDoc.getDocumentReference()).thenReturn(documentReference);
        when(this.newDoc.getXObjects()).thenReturn(newXObjects);
        when(newComment.getXClassReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
        when(newAnnotation.getXClassReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
        when(newField.getXClassReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "Books"));
        LargeStringProperty lsp1 = new LargeStringProperty();
        lsp1.setValue("comment content");
        lsp1.setObject(newComment);
        when(newComment.getField("comment")).thenReturn(lsp1);

        LargeStringProperty lsp2 = new LargeStringProperty();
        lsp2.setValue("annotation content");
        lsp2.setObject(newAnnotation);
        when(newAnnotation.getField("comment")).thenReturn(lsp2);
        StringProperty noSelection = new StringProperty();
        noSelection.setValue("");
        when(newComment.getField("selection")).thenReturn(noSelection);
        StringProperty withSelection = new StringProperty();
        withSelection.setValue("abcd");
        when(newAnnotation.getField("selection")).thenReturn(withSelection);

        LargeStringProperty lsp3 = new LargeStringProperty();
        lsp3.setValue("awm field content");
        when(newField.getProperties()).thenReturn(new Object[]{
            lsp3
        });

        this.listener.onEvent(event, this.newDoc, this.context);

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.DEBUG, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            "Event [org.xwiki.bridge.event.DocumentUpdatedEvent] received from [newDoc] with data [context].",
            this.logCapture.getMessage(0));
        verify(this.executor).executeUpdate(oldXDOM, newXDOM, authorReference, documentReference, DOCUMENT);
        verify(this.executor).executeUpdate(null, "comment content", authorReference, documentReference, COMMENT);
        verify(this.executor).executeUpdate(null, "annotation content", authorReference, documentReference, ANNOTATION);
        verify(this.executor).executeUpdate(null, "awm field content", authorReference, documentReference, AWM_FIELD);
    }

    @Test
    void onEventExistingObjects()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Author");
        DocumentUpdatedEvent event = new DocumentUpdatedEvent(documentReference);
        XDOM oldXDOM = new XDOM(emptyList());
        XDOM newXDOM = new XDOM(emptyList());
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        BaseObject newComment = mock(BaseObject.class);
        BaseObject newAnnotation = mock(BaseObject.class);
        BaseObject newField = mock(BaseObject.class);
        when(newComment.getId()).thenReturn(1L);
        when(newAnnotation.getId()).thenReturn(2L);
        when(newField.getId()).thenReturn(3L);
        BaseObject oldComment = mock(BaseObject.class);
        BaseObject oldAnnotation = mock(BaseObject.class);
        BaseObject oldField = mock(BaseObject.class);
        when(oldComment.getId()).thenReturn(1L);
        when(oldAnnotation.getId()).thenReturn(2L);
        when(oldField.getId()).thenReturn(3L);

        newXObjects.put(documentReference, Arrays.asList(newComment, newAnnotation, newField));
        oldXObjects.put(documentReference, Arrays.asList(oldComment, oldAnnotation, oldField));

        when(this.newDoc.getOriginalDocument()).thenReturn(this.oldDoc);
        when(this.context.getUserReference()).thenReturn(authorReference);
        when(this.oldDoc.getXDOM()).thenReturn(oldXDOM);
        when(this.oldDoc.getXObjects()).thenReturn(oldXObjects);
        when(this.newDoc.getXDOM()).thenReturn(newXDOM);
        when(this.newDoc.getDocumentReference()).thenReturn(documentReference);
        when(this.newDoc.getXObjects()).thenReturn(newXObjects);
        when(newComment.getXClassReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
        when(newAnnotation.getXClassReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
        when(newField.getXClassReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "Books"));
        LargeStringProperty lsp1 = new LargeStringProperty();
        lsp1.setValue("comment content");
        lsp1.setName("comment");
        lsp1.setObject(newComment);
        LargeStringProperty lsp1old = new LargeStringProperty();
        lsp1old.setValue("old comment content");
        when(newComment.getField("comment")).thenReturn(lsp1);
        when(oldComment.getField("comment")).thenReturn(lsp1old);

        LargeStringProperty lsp2 = new LargeStringProperty();
        lsp2.setValue("annotation content");
        lsp2.setName("comment");
        lsp2.setObject(newAnnotation);
        LargeStringProperty lsp2old = new LargeStringProperty();
        lsp2old.setValue("old annotation content");
        when(newAnnotation.getField("comment")).thenReturn(lsp2);
        when(oldAnnotation.getField("comment")).thenReturn(lsp2old);
        StringProperty noSelection = new StringProperty();
        noSelection.setValue("");
        when(newComment.getField("selection")).thenReturn(noSelection);
        StringProperty withSelection = new StringProperty();
        withSelection.setValue("abcd");
        when(newAnnotation.getField("selection")).thenReturn(withSelection);

        LargeStringProperty lsp3 = new LargeStringProperty();
        lsp3.setValue("awm field content");
        lsp3.setName("field");
        LargeStringProperty lsp3old = new LargeStringProperty();
        lsp3old.setValue("old awm field content");
        when(newField.getProperties()).thenReturn(new Object[]{ lsp3 });
        when(oldField.getProperties()).thenReturn(new Object[]{ lsp3old });
        when(oldField.getField("field")).thenReturn(lsp3old);

        this.listener.onEvent(event, this.newDoc, this.context);

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.DEBUG, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            "Event [org.xwiki.bridge.event.DocumentUpdatedEvent] received from [newDoc] with data [context].",
            this.logCapture.getMessage(0));
        verify(this.executor).executeUpdate(oldXDOM, newXDOM, authorReference, documentReference, DOCUMENT);
        verify(this.executor)
            .executeUpdate("old comment content", "comment content", authorReference, documentReference, COMMENT);
        verify(this.executor)
            .executeUpdate("old annotation content", "annotation content", authorReference, documentReference,
                ANNOTATION);
        verify(this.executor)
            .executeUpdate("old awm field content", "awm field content", authorReference, documentReference, AWM_FIELD);
    }

   
}