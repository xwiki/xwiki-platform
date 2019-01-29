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

import com.xpn.xwiki.internal.event.AttachmentAddedEvent;
import com.xpn.xwiki.internal.event.AttachmentDeletedEvent;
import com.xpn.xwiki.internal.event.AttachmentUpdatedEvent;
import com.xpn.xwiki.internal.event.CommentAddedEvent;
import com.xpn.xwiki.internal.event.CommentDeletedEvent;
import com.xpn.xwiki.internal.event.CommentUpdatedEvent;
import org.slf4j.Logger;
import org.xwiki.annotation.event.AnnotationAddedEvent;
import org.xwiki.annotation.event.AnnotationDeletedEvent;
import org.xwiki.annotation.event.AnnotationUpdatedEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.events.AbstractEventStreamEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Store the recordable event inside the event stream (except events that are already handled by the Activity Stream
 * implementation).
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Singleton
@Named("EventStreamStoreListener")
public class DocumentEventListener extends AbstractEventListener
{
    /**
     * The events to match.
     */
    public static final List<Event> LISTENER_EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentCreatedEvent());
            add(new DocumentUpdatedEvent());
            add(new DocumentDeletedEvent());
            add(new CommentAddedEvent());
            add(new CommentDeletedEvent());
            add(new CommentUpdatedEvent());
            add(new AttachmentAddedEvent());
            add(new AttachmentDeletedEvent());
            add(new AttachmentUpdatedEvent());
            add(new AnnotationAddedEvent());
            add(new AnnotationDeletedEvent());
            add(new AnnotationUpdatedEvent());
        }
    };

    private static final BeginFoldEvent IGNORED_EVENTS = otherEvent ->  otherEvent instanceof BeginFoldEvent;

    @Inject
    private EventStream eventStream;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private ObservationContext observationContext;

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private DocumentEventRecorder documentEventRecorder;

    /**
     * Construct a NotificationEventListener.
     */
    public DocumentEventListener()
    {
        super("EventStreamStoreListener", LISTENER_EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (remoteObservationManagerContext.isRemoteState() || observationContext.isIn(IGNORED_EVENTS)
            || execution.getContext().hasProperty(AbstractEventStreamEvent.EVENT_LOOP_CONTEXT_LOCK_PROPERTY)) {
            return;
        }

        try {
            this.execution.getContext()
                    .setProperty(AbstractEventStreamEvent.EVENT_LOOP_CONTEXT_LOCK_PROPERTY, true);

            // Handle separately some basic events
            for (Event ignoredEvent : LISTENER_EVENTS) {
                if (ignoredEvent.matches(event)) {
                    documentEventRecorder.recordEvent(event, source);
                    return;
                }
            }

        } catch (Exception e) {
            logger.warn("Failed to save the event [{}].", event.getClass().getCanonicalName(), e);
        } finally {
            this.execution.getContext()
                    .removeProperty(AbstractEventStreamEvent.EVENT_LOOP_CONTEXT_LOCK_PROPERTY);
        }
    }
}
