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
package org.xwiki.eventstream.store.internal;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.annotation.event.AnnotationAddedEvent;
import org.xwiki.annotation.event.AnnotationDeletedEvent;
import org.xwiki.annotation.event.AnnotationUpdatedEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.DocumentEventType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AttachmentAddedEvent;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xpn.xwiki.internal.event.AttachmentUpdatedEvent;
import com.xpn.xwiki.internal.event.CommentAddedEvent;
import com.xpn.xwiki.internal.event.CommentDeletedEvent;
import com.xpn.xwiki.internal.event.CommentUpdatedEvent;

/**
 * Store the recordable event inside the event stream (except events that are already handled by the Activity Stream
 * implementation).
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component(roles = DocumentEventRecorder.class)
@Singleton
public class DocumentEventRecorder
{
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private EventStore eventStore;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Inject
    private EventFactory eventFactory;

    /**
     * Record the given event.
     * 
     * @param event the event to record
     * @param source the source that has triggered the event
     * @throws EventStreamException when failing to record the passed event
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     */
    public void recordEvent(Event event, Object source) throws EventStreamException, InterruptedException, ExecutionException
    {
        XWikiDocument currentDoc = (XWikiDocument) source;
        XWikiDocument originalDoc = currentDoc.getOriginalDocument();

        final String streamName = serializer.serialize(currentDoc.getDocumentReference().getLastSpaceReference());

        // If we haven't found a stream to store the event or if both currentDoc and originalDoc are null: exit
        if (streamName == null) {
            return;
        }

        String eventType;

        if (event instanceof DocumentCreatedEvent) {
            eventType = DocumentEventType.CREATE;
        } else if (event instanceof DocumentUpdatedEvent) {
            eventType = DocumentEventType.UPDATE;
        } else if (event instanceof DocumentDeletedEvent) {
            eventType = DocumentEventType.DELETE;
            // When we receive a DELETE event, the given document is blank and does not have version & hidden tag
            // properly set.
            currentDoc.setVersion(originalDoc.getVersion());
            currentDoc.setHidden(originalDoc.isHidden());
        } else if (event instanceof CommentAddedEvent) {
            eventType = DocumentEventType.ADD_COMMENT;
        } else if (event instanceof CommentDeletedEvent) {
            eventType = DocumentEventType.DELETE_COMMENT;
        } else if (event instanceof CommentUpdatedEvent) {
            eventType = DocumentEventType.UPDATE_COMMENT;
        } else if (event instanceof AttachmentAddedEvent) {
            eventType = DocumentEventType.ADD_ATTACHMENT;
        } else if (event instanceof AttachmentDeletedEvent) {
            eventType = DocumentEventType.DELETE_ATTACHMENT;
        } else if (event instanceof AttachmentUpdatedEvent) {
            eventType = DocumentEventType.UPDATE_ATTACHMENT;
        } else if (event instanceof AnnotationAddedEvent) {
            eventType = DocumentEventType.ADD_ANNOTATION;
        } else if (event instanceof AnnotationDeletedEvent) {
            eventType = DocumentEventType.DELETE_ANNOTATION;
        } else if (event instanceof AnnotationUpdatedEvent) {
            // update annotation
            eventType = DocumentEventType.UPDATE_ANNOTATION;
        } else {
            return;
        }

        recordEvent(streamName, currentDoc, eventType, eventType);
    }

    private void recordEvent(String streamName, XWikiDocument doc, String type, String title)
    {
        final String msgPrefix = "activitystream.event.";

        org.xwiki.eventstream.Event event = this.eventFactory.createRawEvent();
        event.setStream(streamName);
        event.setDocument(doc.getDocumentReference());
        event.setDate(doc.getDate());
        event.setImportance(org.xwiki.eventstream.Event.Importance.MEDIUM);
        event.setType(type);
        event.setTitle(msgPrefix + title);
        event.setBody(msgPrefix + title);
        event.setDocumentVersion(doc.getVersion());
        // This might be wrong once non-altering events will be logged.
        DocumentReference originalAuthor =
            this.userReferenceSerializer.serialize(doc.getAuthors().getOriginalMetadataAuthor());
        event.setUser(originalAuthor);
        event.setHidden(doc.isHidden());
        event.setDocumentTitle(doc.getRenderedTitle(Syntax.PLAIN_1_0, contextProvider.get()));

        this.eventStore.saveEvent(event);
    }
}
