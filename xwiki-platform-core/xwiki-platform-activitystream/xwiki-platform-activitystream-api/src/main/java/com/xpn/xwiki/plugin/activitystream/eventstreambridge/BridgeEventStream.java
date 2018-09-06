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
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;
import com.xpn.xwiki.plugin.activitystream.impl.ActivityStreamConfiguration;
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
    /** Needed for accessing the current request context. */
    @Inject
    private Execution execution;

    /** Needed for running queries. */
    @Inject
    private QueryManager qm;

    @Inject
    private EventConverter eventConverter;

    @Inject
    private ActivityStreamConfiguration activityStreamConfiguration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public void addEvent(Event e)
    {
        try {
            XWikiContext context = getXWikiContext();
            ActivityStreamPlugin plugin = getPlugin(context);
            plugin.getActivityStream().addActivityEvent(eventConverter.convertEventToActivity(e), context);
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

        if (activityStreamConfiguration.useMainStore()) {
            q.setWiki(wikiDescriptorManager.getMainWikiId());
        }

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
