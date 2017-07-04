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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventGroup;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.events.AbstractEventStreamEvent;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.eventstream.events.EventStreamDeletedEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;
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
    /**
     * Used to provide a key to a property in the current execution context that avoids stepping into a loop when
     * triggering new events.
     */
    private static final String EVENT_CONTEXT_LOCK_PROPERTY = "eventContextProperty";

    /** Needed for accessing the current request context. */
    @Inject
    private Execution execution;

    /** Needed for running queries. */
    @Inject
    private QueryManager qm;

    @Inject
    private EventConverter eventConverter;

    @Inject
    private ObservationManager observationManager;

    @Override
    public void addEvent(Event e)
    {
        try {
            XWikiContext context = getXWikiContext();
            ActivityStreamPlugin plugin = getPlugin(context);
            plugin.getActivityStream().addActivityEvent(eventConverter.convertEventToActivity(e), context);
            this.sendEventStreamEvent(new EventStreamAddedEvent(e));
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
            plugin.getActivityStream().deleteActivityEvent(eventConverter.convertEventToActivity(e), context);
            this.sendEventStreamEvent(new EventStreamDeletedEvent(e));
        } catch (ActivityStreamException ex) {
            // Unlikely; nothing we can do
        }
    }

    private void sendEventStreamEvent(AbstractEventStreamEvent eventStreamEvent)
    {
        // In order to avoid infinite loop caused by observation events triggering event stream events that are
        // themselves triggering new observation events …, we add a new property to the execution context. Therefore,
        // an observation event declared out of an event stream event can only be triggered once.
        if (!this.execution.getContext().hasProperty(EVENT_CONTEXT_LOCK_PROPERTY)) {
            this.execution.getContext().newProperty(EVENT_CONTEXT_LOCK_PROPERTY).declare();
            this.observationManager.notify(eventStreamEvent, eventStreamEvent.getEvent());
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
                eventConverter.convertEventToActivity(e), context)).toArray(new Event[0]));
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
     * Convert a list of old {@link ActivityEvent}s to a list of new {@link Event}s.
     *
     * @param events the activity events to convert
     * @return the equivalent events
     */
    private List<Event> convertActivitiesToEvents(List<ActivityEvent> events)
    {
        List<Event> result = new ArrayList<Event>(events.size());
        for (ActivityEvent e : events) {
            result.add(eventConverter.convertActivityToEvent(e));
        }
        return result;
    }
}
