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
package org.xwiki.messagestream.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.messagestream.MessageStream;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Implementation of the {@link MessageStream} that stores messages as {@link Event events} in the {@link EventStream}.
 * 
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Singleton
public class DefaultMessageStream implements MessageStream
{
    /** Needed for altering queries. */
    @Inject
    private QueryManager qm;

    /** Needed for obtaining the current wiki name. */
    @Inject
    private ModelContext context;

    /** Entity serializer, used for converting references to strings suitable for storing in the "stream" field. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /** The event stream used for storing the messages. */
    @Inject
    private EventStream stream;

    @Inject
    private EventStore eventStore;

    /** The default factory for creating event objects. */
    @Inject
    private EventFactory factory;

    /** Needed for retrieving the current user. */
    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private Logger logger;

    @Override
    public void postPublicMessage(String message)
    {
        Event e = createMessageEvent(message, "publicMessage");
        DocumentReference userDoc = this.bridge.getCurrentUserReference();
        e.setRelatedEntity(userDoc);
        e.setImportance(Importance.MINOR);
        e.setStream(this.serializer.serialize(userDoc));
        saveEvent(e);
    }

    @Override
    public void postPersonalMessage(String message)
    {
        Event e = createMessageEvent(message, "personalMessage");
        DocumentReference userDoc = this.bridge.getCurrentUserReference();
        e.setRelatedEntity(userDoc);
        e.setStream(this.serializer.serialize(userDoc));
        saveEvent(e);
    }

    @Override
    public void postDirectMessageToUser(String message, DocumentReference user)
    {
        if (!this.bridge.exists(user)) {
            throw new IllegalArgumentException("Target user does not exist");
        }
        Event e = createMessageEvent(message, "directMessage");
        e.setRelatedEntity(new ObjectReference("XWiki.XWikiUsers", user));
        e.setStream(this.serializer.serialize(user));
        e.setImportance(Importance.CRITICAL);
        saveEvent(e);
    }

    @Override
    public void postMessageToGroup(String message, DocumentReference group) throws IllegalAccessError
    {
        if (!this.bridge.exists(group)) {
            throw new IllegalArgumentException("Target group does not exist");
        }
        Event e = createMessageEvent(message, "groupMessage");
        e.setRelatedEntity(new ObjectReference("XWiki.XWikiGroups", group));
        e.setStream(this.serializer.serialize(group));
        e.setImportance(Importance.MAJOR);
        saveEvent(e);
    }

    private void saveEvent(Event event)
    {
        try {
            this.eventStore.saveEvent(event);
        } catch (EventStreamException e) {
            this.logger.error("Failed to save the message", e);
        }
    }

    @Override
    public List<Event> getRecentPersonalMessages()
    {
        return getRecentPersonalMessages(this.bridge.getCurrentUserReference(), 30, 0);
    }

    @Override
    public List<Event> getRecentPersonalMessages(int limit, int offset)
    {
        return getRecentPersonalMessages(this.bridge.getCurrentUserReference(), limit, offset);
    }

    @Override
    public List<Event> getRecentPersonalMessages(DocumentReference author)
    {
        return getRecentPersonalMessages(author, 30, 0);
    }

    @Override
    public List<Event> getRecentPersonalMessages(DocumentReference author, int limit, int offset)
    {
        List<Event> result = new ArrayList<Event>();
        try {
            Query q = this.qm.createQuery(
                "where event.application = 'MessageStream' and event.type = 'personalMessage'"
                    + " and event.user = :user order by event.date desc",
                Query.XWQL);
            q.bindValue("user", this.serializer.serialize(author));
            q.setLimit(limit > 0 ? limit : 30).setOffset(offset >= 0 ? offset : 0);
            result = this.stream.searchEvents(q);
        } catch (QueryException ex) {
            this.logger.warn("Failed to search personal messages: {}", ex.getMessage());
        }
        return result;
    }

    @Override
    public List<Event> getRecentDirectMessages()
    {
        return getRecentDirectMessages(30, 0);
    }

    @Override
    public List<Event> getRecentDirectMessages(int limit, int offset)
    {
        List<Event> result = new ArrayList<Event>();
        try {
            Query q = this.qm.createQuery(
                "where event.application = 'MessageStream' and event.type = 'directMessage'"
                    + " and event.stream = :targetUser order by event.date desc",
                Query.XWQL);
            q.bindValue("targetUser", this.serializer.serialize(this.bridge.getCurrentUserReference()));
            q.setLimit(limit > 0 ? limit : 30).setOffset(offset >= 0 ? offset : 0);

            // TODO: refactor to use EventStore instead
            result = this.stream.searchEvents(q);
        } catch (QueryException ex) {
            this.logger.warn("Failed to search direct messages: {}", ex.getMessage());
        }
        return result;
    }

    @Override
    public List<Event> getRecentMessagesForGroup(DocumentReference group)
    {
        return getRecentMessagesForGroup(group, 30, 0);
    }

    @Override
    public List<Event> getRecentMessagesForGroup(DocumentReference group, int limit, int offset)
    {
        List<Event> result = new ArrayList<Event>();
        try {
            Query q = this.qm.createQuery(
                "where event.application = 'MessageStream' and event.type = 'groupMessage'"
                    + " and event.stream = :group order by event.date desc",
                Query.XWQL);
            q.bindValue("group", this.serializer.serialize(group));
            q.setLimit(limit > 0 ? limit : 30).setOffset(offset >= 0 ? offset : 0);

            // TODO: refactor to use EventStore instead
            result = this.stream.searchEvents(q);
        } catch (QueryException ex) {
            this.logger.warn("Failed to search group messages: {}", ex.getMessage());
        }
        return result;
    }

    @Override
    public void deleteMessage(String id)
    {
        Query q;
        try {
            q = this.qm.createQuery("where event.id = :id", Query.XWQL);
            q.bindValue("id", id);

            // TODO: refactor to use EventStore instead
            List<Event> events = this.stream.searchEvents(q);

            if (events == null || events.isEmpty()) {
                throw new IllegalArgumentException("This message does not exist");
            } else if (events.get(0).getUser().equals(this.bridge.getCurrentUserReference())) {
                this.eventStore.deleteEvent(events.get(0));
            } else {
                throw new IllegalArgumentException("You are not authorized to delete this message");
            }
        } catch (QueryException | EventStreamException ex) {
            this.logger.warn("Failed to delete message: {}", ex.getMessage());
        }
    }

    /**
     * Creates an {@link Event} object with the common fields filled in: event ID, target document, application, user...
     * It also fills in the provided message body and type.
     * 
     * @param message the message to store in the event; at most 2000 characters are stored, longer messages are
     *        automatically trimmed
     * @param messageType the type of message
     * @return the initialized event object
     */
    protected Event createMessageEvent(String message, String messageType)
    {
        Event e = this.factory.createEvent();
        e.setApplication("MessageStream");
        e.setDocument(new DocumentReference(this.context.getCurrentEntityReference().getRoot().getName(), "XWiki",
            e.getId()));
        e.setBody(StringUtils.left(message, 2000));
        e.setType(messageType);
        return e;
    }
}
