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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventGroup;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Load events from the Legacy Event Store.
 *
 * @since 11.1RC1
 * @version $Id$
 */
@Component(roles = LegacyEventLoader.class)
@Singleton
public class LegacyEventLoader
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private LegacyEventConverter eventConverter;

    @Inject
    private LegacyEventStreamStoreConfiguration configuration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    /**
     * Retrieve the group that a given event is part of.
     *
     * @param event the event to search for
     * @return the event's group of related events
     */
    public EventGroup getRelatedEvents(Event event)
    {
        EventGroup result = new EventGroup();

        try {
            Query query = queryManager.createQuery("where event.requestId = :requestId", Query.HQL);
            query.bindValue("requestId", event.getGroupId());
            List<Event> events = searchEvents(query);
            result.addEvents(events.toArray(new Event[0]));
        } catch (Exception e) {
            logger.error("Failed to load related events for [%s].", event.getId(), e);
        }

        return result;
    }

    /**
     * Search stored events. The query will be prefixed with a hardcoded {@code select event from Event as event} or
     * equivalent stub which selects actual events from the storage, so it must start with further {@code from} or
     * {@code where} statements.
     *
     * @param query a query stub
     * @return the list of events matched by the query
     * @throws QueryException if the query is malformed or cannot be executed
     */
    public List<Event> searchEvents(Query query) throws QueryException
    {
        Query q = this.queryManager.createQuery("select event from LegacyEvent event " + query.getStatement(),
            query.getLanguage());
        for (Map.Entry<String, Object> entry : query.getNamedParameters().entrySet()) {
            q.bindValue(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Integer, Object> entry : query.getPositionalParameters().entrySet()) {
            q.bindValue(entry.getKey(), entry.getValue());
        }
        q.setLimit(query.getLimit());
        q.setOffset(query.getOffset());

        if (configuration.useMainStore()) {
            q.setWiki(wikiDescriptorManager.getMainWikiId());
        }

        List<LegacyEvent> events = q.execute();
        return convertActivitiesToEvents(events);
    }

    /**
     * Convert a list of old {@link LegacyEvent}s to a list of new {@link Event}s.
     *
     * @param events the activity events to convert
     * @return the equivalent events
     */
    private List<Event> convertActivitiesToEvents(List<LegacyEvent> events)
    {
        List<Event> result = new ArrayList<Event>(events.size());
        for (LegacyEvent e : events) {
            result.add(eventConverter.convertLegacyActivityToEvent(e));
        }
        return result;
    }

    /**
     * @param eventId the unique identifier of the event
     * @return the event stored in the database
     * @throws QueryException when failing to get the event
     * @since 12.2
     * @since 11.10.4
     */
    public LegacyEvent getLegacyEvent(String eventId) throws QueryException
    {
        Query query =
            this.queryManager.createQuery("select event from LegacyEvent event where eventId = :eventId", Query.HQL);
        query.bindValue("eventId", eventId);

        if (configuration.useMainStore()) {
            query.setWiki(wikiDescriptorManager.getMainWikiId());
        }

        List<LegacyEvent> events = query.execute();

        if (events.isEmpty()) {
            return null;
        }

        return events.get(0);
    }

    /**
     * @param eventId the event id
     * @return the event or null if none could be found
     * @throws QueryException  when failing to get the event
     * @since 12.2
     * @since 11.10.4
     */
    public Event getEvent(String eventId) throws QueryException
    {
        LegacyEvent legacyEvent = getLegacyEvent(eventId);

        if (legacyEvent != null) {
            return eventConverter.convertLegacyActivityToEvent(legacyEvent);
        }

        return null;
    }

    /**
     * @return the total number of events in the legacy store
     * @throws QueryException when failing to query the events
     * @since 12.6.1
     * @since 12.7RC1
     */
    public long countEvents() throws QueryException
    {
        Query query = this.queryManager.createQuery("select count(*) from LegacyEvent", Query.HQL);

        return (Long) query.execute().get(0);
    }
}
