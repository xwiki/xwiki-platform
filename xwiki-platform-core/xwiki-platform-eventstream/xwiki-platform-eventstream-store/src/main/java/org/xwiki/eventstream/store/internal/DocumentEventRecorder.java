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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AttachmentAddedEvent;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xpn.xwiki.internal.event.AttachmentUpdatedEvent;
import com.xpn.xwiki.internal.event.CommentAddedEvent;
import com.xpn.xwiki.internal.event.CommentDeletedEvent;
import com.xpn.xwiki.internal.event.CommentUpdatedEvent;
import org.xwiki.annotation.event.AnnotationAddedEvent;
import org.xwiki.annotation.event.AnnotationDeletedEvent;
import org.xwiki.annotation.event.AnnotationUpdatedEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Store the recordable event inside the event stream (except events that are already handled by the Activity Stream
 * implementation).
 *
 * @version $Id$
 * @since 11.0RC1
 */
@Component(roles = DocumentEventRecorder.class)
@Singleton
public class DocumentEventRecorder
{
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private EventStream eventStream;

    /**
     * Record the given event.
     * @param event the event to record
     * @param source the source that has triggered the event
     */
    public void recordEvent(Event event, Object source)
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
            eventType = EventType.ADD_ATTACHMENT;
        } else if (event instanceof DocumentUpdatedEvent) {
            eventType = EventType.UPDATE_ATTACHMENT;
        } else if (event instanceof DocumentDeletedEvent) {
            eventType = EventType.DELETE_ATTACHMENT;
            // When we receive a DELETE event, the given document is blank and does not have version & hidden tag
            // properly set.
            currentDoc.setVersion(originalDoc.getVersion());
            currentDoc.setHidden(originalDoc.isHidden());
        } else if (event instanceof CommentAddedEvent) {
            eventType = EventType.ADD_COMMENT;
        } else if (event instanceof CommentDeletedEvent) {
            eventType = EventType.DELETE_COMMENT;
        } else if (event instanceof CommentUpdatedEvent) {
            eventType = EventType.UPDATE_COMMENT;
        } else if (event instanceof AttachmentAddedEvent) {
            eventType = EventType.ADD_ATTACHMENT;
        } else if (event instanceof AttachmentDeletedEvent) {
            eventType = EventType.DELETE_ATTACHMENT;
        } else if (event instanceof AttachmentUpdatedEvent) {
            eventType = EventType.UPDATE_ATTACHMENT;
        } else if (event instanceof AnnotationAddedEvent) {
            eventType = EventType.ADD_ANNOTATION;
        } else if (event instanceof AnnotationDeletedEvent) {
            eventType = EventType.DELETE_ANNOTATION;
        } else if (event instanceof AnnotationUpdatedEvent){
            // update annotation
            eventType = EventType.UPDATE_ANNOTATION;
        } else {
            return;
        }

        recordEvent(streamName, currentDoc, eventType, eventType);
    }

    private void recordEvent(String streamName, XWikiDocument doc, String type, String title)
    {
        final String msgPrefix = "activitystream.event.";

        DefaultEvent event = new DefaultEvent();
        event.setStream(streamName);
        event.setDocument(doc.getDocumentReference());
        event.setDate(doc.getDate());
        event.setImportance(org.xwiki.eventstream.Event.Importance.MEDIUM);
        event.setType(type);
        event.setTitle(msgPrefix + title);
        event.setBody(msgPrefix + title);
        event.setDocumentVersion(doc.getVersion());
        // This might be wrong once non-altering events will be logged.
        event.setUser(doc.getAuthorReference());
        event.setHidden(doc.isHidden());
        eventStream.addEvent(event);
    }
}
