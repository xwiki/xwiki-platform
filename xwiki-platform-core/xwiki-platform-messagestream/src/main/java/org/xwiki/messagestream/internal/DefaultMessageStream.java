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
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.messagestream.MessageStream;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
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
    /** Logging helper object. */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMessageStream.class);

    /** Needed for altering queries. */
    @Inject
    private QueryManager qm;

    /** Needed for obtaining the current wiki name. */
    @Inject
    private ModelContext context;

    /** Entity parser used for converting the current user name into a proper reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<String> currentResolver;

    /** Entity serializer, used for converting references to strings suitable for storing in the "stream" field. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /** The event stream used for storing the messages. */
    @Inject
    private EventStream stream;

    /** The default factory for creating event objects. */
    @Inject
    private EventFactory factory;

    /** Needed for retrieving the current user. */
    @Inject
    private DocumentAccessBridge bridge;

    @Override
    public void postPublicMessage(String message)
    {
        Event e = createMessageEvent(message, "publicMessage");
        // FIXME This shouldn't be needed if the current user were already available as a reference.
        DocumentReference userDoc =
            new DocumentReference(this.currentResolver.resolve(this.bridge.getCurrentUser(), EntityType.DOCUMENT));
        e.setRelatedEntity(userDoc);
        e.setImportance(Importance.MINOR);
        e.setStream(this.serializer.serialize(userDoc));
        this.stream.addEvent(e);
    }

    @Override
    public void postPersonalMessage(String message)
    {
        Event e = createMessageEvent(message, "personalMessage");
        // FIXME This shouldn't be needed if the current user were already available as a reference.
        DocumentReference userDoc =
            new DocumentReference(this.currentResolver.resolve(this.bridge.getCurrentUser(), EntityType.DOCUMENT));
        e.setRelatedEntity(userDoc);
        e.setStream(this.serializer.serialize(userDoc));
        this.stream.addEvent(e);
    }

    @Override
    public void postDirectMessageToUser(String message, DocumentReference user)
    {
        Event e = createMessageEvent(message, "directMessage");
        e.setRelatedEntity(new ObjectReference("XWiki.XWikiUsers", user));
        e.setStream(this.serializer.serialize(user));
        e.setImportance(Importance.CRITICAL);
        this.stream.addEvent(e);
    }

    @Override
    public void postMessageToGroup(String message, DocumentReference group) throws IllegalAccessError
    {
        Event e = createMessageEvent(message, "groupMessage");
        e.setRelatedEntity(new ObjectReference("XWiki.XWikiGroups", group));
        e.setStream(this.serializer.serialize(group));
        e.setImportance(Importance.MAJOR);
        this.stream.addEvent(e);
    }

    @Override
    public List<Event> getRecentPersonalMessages()
    {
        DocumentReference currentUser = new DocumentReference(
            this.currentResolver.resolve(this.bridge.getCurrentUser(), EntityType.DOCUMENT));
        return getRecentPersonalMessages(currentUser, 30, 0);
    }

    @Override
    public List<Event> getRecentPersonalMessages(int limit, int offset)
    {
        DocumentReference currentUser = new DocumentReference(
            this.currentResolver.resolve(this.bridge.getCurrentUser(), EntityType.DOCUMENT));
        return getRecentPersonalMessages(currentUser, limit, offset);
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
            LOG.warn("Failed to search personal messages: {}", ex.getMessage());
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
            DocumentReference currentUser = new DocumentReference(
                this.currentResolver.resolve(this.bridge.getCurrentUser(), EntityType.DOCUMENT));
            Query q = this.qm.createQuery(
                "where event.application = 'MessageStream' and event.type = 'directMessage'"
                + " and event.stream = :targetUser order by event.date desc",
                Query.XWQL);
            q.bindValue("targetUser", this.serializer.serialize(currentUser));
            q.setLimit(limit > 0 ? limit : 30).setOffset(offset >= 0 ? offset : 0);
            result = this.stream.searchEvents(q);
        } catch (QueryException ex) {
            LOG.warn("Failed to search direct messages: {}", ex.getMessage());
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
            result = this.stream.searchEvents(q);
        } catch (QueryException ex) {
            LOG.warn("Failed to search group messages: {}", ex.getMessage());
        }
        return result;
    }

    @Override
    public void deleteMessage(String id)
    {
        Query q;
        try {
            DocumentReference currentUser = new DocumentReference(
                this.currentResolver.resolve(this.bridge.getCurrentUser(), EntityType.DOCUMENT));
            q = this.qm.createQuery("where event.id = :id", Query.XWQL);
            q.bindValue("id", id);
            List<Event> events = this.stream.searchEvents(q);
            if (events == null || events.isEmpty()) {
                throw new IllegalArgumentException("This message does not exist");
            } else if (events.get(0).getUser().equals(currentUser)) {
                this.stream.deleteEvent(events.get(0));
            } else {
                throw new IllegalArgumentException("You are not authorized to delete this message");
            }
        } catch (QueryException ex) {
            LOG.warn("Failed to delete message: {}", ex.getMessage());
        }
    }

    /**
     * Creates an {@link Event} object with the common fields filled in: event ID, target document, application, user...
     * It also fills in the provided message body and type.
     * 
     * @param message the message to store in the event; at most 2000 characters are stored, longer messages are
     *            automatically trimmed
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
