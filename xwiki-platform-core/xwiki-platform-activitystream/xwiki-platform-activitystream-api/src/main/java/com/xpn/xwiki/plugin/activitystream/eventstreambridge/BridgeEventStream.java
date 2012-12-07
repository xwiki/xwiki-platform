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
package com.xpn.xwiki.plugin.activitystream.eventstreambridge;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventGroup;
import org.xwiki.eventstream.EventStream;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;
import com.xpn.xwiki.plugin.activitystream.impl.ActivityEventImpl;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityStreamPlugin;

/**
 * Implementation for the {@link EventStream} which actually uses the old {@link ActivityStreamPlugin} for storing and
 * searching events.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class BridgeEventStream implements EventStream
{
    /** Needed for creating raw events. */
    @Inject
    private EventFactory eventFactory;

    /** Needed for accessing the current request context. */
    @Inject
    private Execution execution;

    /** Needed for running queries. */
    @Inject
    private QueryManager qm;

    /** Needed for serializing document names. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /** Needed for serializing the wiki and space names. */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    /** Needed for deserializing document names. */
    @Inject
    private EntityReferenceResolver<String> resolver;

    /** Needed for deserializing related entities. */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> explicitResolver;

    @Override
    public void addEvent(Event e)
    {
        try {
            XWikiContext context = getXWikiContext();
            ActivityStreamPlugin plugin = getPlugin(context);
            plugin.getActivityStream().addActivityEvent(convertEventToActivity(e), context);
        } catch (ActivityStreamException ex) {
            // Unlikely; nothing we can do
        }
    }

    @Override
    public void deleteEvent(Event e)
    {
        try {
            XWikiContext context = getXWikiContext();
            ActivityStreamPlugin plugin = getPlugin(context);
            plugin.getActivityStream().deleteActivityEvent(convertEventToActivity(e), context);
        } catch (ActivityStreamException ex) {
            // Unlikely; nothing we can do
        }
    }

    @Override
    public EventGroup getRelatedEvents(Event e)
    {
        XWikiContext context = getXWikiContext();
        ActivityStreamPlugin plugin = getPlugin(context);
        EventGroup result = new EventGroup();
        try {
            result.addEvents(convertActivitiesToEvents(plugin.getActivityStream().getRelatedEvents(
                convertEventToActivity(e), context)).toArray(new Event[0]));
        } catch (ActivityStreamException ex) {
            // Should not happen, and the eventual error was already reported downstream
        }
        return result;
    }

    @Override
    public List<Event> searchEvents(Query query) throws QueryException
    {
        Query q = this.qm.createQuery("select event from ActivityEventImpl event "
            + query.getStatement(), query.getLanguage());
        for (Map.Entry<String, Object> entry : query.getNamedParameters().entrySet()) {
            q.bindValue(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Integer, Object> entry : query.getPositionalParameters().entrySet()) {
            q.bindValue(entry.getKey(), entry.getValue());
        }
        q.setLimit(query.getLimit());
        q.setOffset(query.getOffset());
        List<ActivityEvent> events = q.execute();
        return convertActivitiesToEvents(events);
    }

    /**
     * Retrieve the old {@link XWikiContext} from the {@link org.xwiki.context.ExecutionContext execution context}.
     * 
     * @return the current request context
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    /**
     * Retrieve the {@link ActivityStreamPlugin} instance.
     * 
     * @param context the current request context
     * @return the plugin instance
     */
    private ActivityStreamPlugin getPlugin(XWikiContext context)
    {
        return (ActivityStreamPlugin) context.getWiki().getPlugin("activitystream", context);
    }

    /**
     * Converts a new {@link Event} to the old {@link ActivityEvent}.
     * 
     * @param e the event to transform
     * @return the equivalent activity event
     */
    private ActivityEvent convertEventToActivity(Event e)
    {
        ActivityEvent result = new ActivityEventImpl();
        result.setApplication(e.getApplication());
        result.setBody(e.getBody());
        result.setDate(e.getDate());
        result.setEventId(e.getId());
        result.setPage(this.serializer.serialize(e.getDocument()));
        if (e.getDocumentTitle() != null) {
            result.setParam1(e.getDocumentTitle());
        }
        if (e.getRelatedEntity() != null) {
            result.setParam2(this.serializer.serialize(e.getRelatedEntity()));
        }
        result.setPriority((e.getImportance().ordinal() + 1) * 10);

        result.setRequestId(e.getGroupId());
        result.setSpace(this.localSerializer.serialize(e.getSpace()));
        result.setStream(e.getStream());
        result.setTitle(e.getTitle());
        result.setType(e.getType());
        if (e.getUrl() != null) {
            result.setUrl(e.getUrl().toString());
        }
        result.setUser(this.serializer.serialize(e.getUser()));
        result.setVersion(e.getDocumentVersion());
        result.setWiki(this.serializer.serialize(e.getWiki()));

        result.setParameters(e.getParameters());
        
        return result;
    }

    /**
     * Convert a list of old {@link ActivityEvent}s to a list of new {@link Event}s.
     * 
     * @param events the activity events to convert
     * @return the equivalent events
     */
    private List<Event> convertActivitiesToEvents(List<ActivityEvent> events)
    {
        List<Event> result = new ArrayList<Event>(events.size());
        for (ActivityEvent e : events) {
            result.add(convertActivityToEvent(e));
        }
        return result;
    }

    /**
     * Convert an old {@link ActivityEvent} to the new {@link Event}.
     * 
     * @param e the activity event to transform
     * @return the equivalent event
     */
    private Event convertActivityToEvent(ActivityEvent e)
    {
        Event result = this.eventFactory.createRawEvent();
        result.setApplication(e.getApplication());
        result.setBody(e.getBody());
        result.setDate(e.getDate());
        result.setDocument(new DocumentReference(this.resolver.resolve(e.getPage(), EntityType.DOCUMENT)));
        result.setId(e.getEventId());
        result.setDocumentTitle(e.getParam1());
        if (StringUtils.isNotEmpty(e.getParam2())) {
            if (StringUtils.endsWith(e.getType(), "Attachment")) {
                result.setRelatedEntity(this.explicitResolver.resolve(e.getParam2(), EntityType.ATTACHMENT,
                    result.getDocument()));
            } else if (StringUtils.endsWith(e.getType(), "Comment")
                || StringUtils.endsWith(e.getType(), "Annotation")) {
                result.setRelatedEntity(this.explicitResolver.resolve(e.getParam2(), EntityType.OBJECT,
                    result.getDocument()));
            }
        }
        result.setImportance(Event.Importance.MEDIUM);
        if (e.getPriority() > 0) {
            int priority = e.getPriority() / 10 - 1;
            if (priority >= 0 && priority < Event.Importance.values().length) {
                result.setImportance(Event.Importance.values()[priority]);
            }
        }

        result.setGroupId(e.getRequestId());
        result.setStream(e.getStream());
        result.setTitle(e.getTitle());
        result.setType(e.getType());
        if (StringUtils.isNotBlank(e.getUrl())) {
            try {
                result.setUrl(new URL(e.getUrl()));
            } catch (MalformedURLException ex) {
                // Should not happen
            }
        }
        result.setUser(new DocumentReference(this.resolver.resolve(e.getUser(), EntityType.DOCUMENT)));
        result.setDocumentVersion(e.getVersion());

        result.setParameters(e.getParameters());
        return result;
    }
}
