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
package com.xpn.xwiki.plugin.activitystream.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStatusManager;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.eventstream.internal.events.EventStatusAddOrUpdatedEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventStatus;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;
import com.xpn.xwiki.plugin.activitystream.eventstreambridge.EventConverter;
import com.xpn.xwiki.plugin.activitystream.impl.ActivityStreamConfiguration;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Default implementation of {@link EventStatusManager} which use the Activity Stream storage.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
public class DefaultEventStatusManager implements EventStatusManager
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private EventConverter eventConverter;

    @Inject
    private ActivityStreamConfiguration configuration;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ObservationManager observation;

    @Override
    public List<EventStatus> getEventStatus(List<Event> events, List<String> entityIds) throws Exception
    {
        List<EventStatus> results = new ArrayList<>();

        // Don't perform any query if the list of events or entities is actually empty
        if (events.isEmpty() || entityIds.isEmpty()) {
            return results;
        }

        // Get the ActivityEventStatus from the database and convert them
        Query query = queryManager.createQuery("select eventStatus from ActivityEventStatusImpl eventStatus "
            + "where eventStatus.activityEvent.id in :eventIds and eventStatus.entityId in :entityIds", Query.HQL);
        query.bindValue("eventIds", getEventIds(events));
        query.bindValue("entityIds", entityIds);
        for (ActivityEventStatus activityEventStatus : query.<ActivityEventStatus>execute()) {
            results.add(
                new DefaultEventStatus(eventConverter.convertActivityToEvent(activityEventStatus.getActivityEvent()),
                    activityEventStatus.getEntityId(), activityEventStatus.isRead()));
        }

        // For status that are not present in the database, we create objects with read = false
        for (Event event : events) {
            for (String entityId : entityIds) {
                if (!isPresent(event, entityId, results)) {
                    results.add(new DefaultEventStatus(event, entityId, false));
                }
            }
        }

        // Sort statuses by date, in the descending order, like notifications, otherwise the order is lost
        Collections.sort(results,
            (status1, status2) -> status2.getEvent().getDate().compareTo(status1.getEvent().getDate()));

        return results;
    }

    /**
     * @param events a list of events
     * @return the list of the events' id
     */
    private List<String> getEventIds(List<Event> events)
    {
        List<String> eventIds = new ArrayList<>();
        for (Event event : events) {
            eventIds.add(event.getId());
        }
        return eventIds;
    }

    /**
     * @return if there is a status concerning the given event and the given entity in a list of statuses
     */
    private boolean isPresent(Event event, String entityId, List<EventStatus> list)
    {
        for (EventStatus status : list) {
            if (StringUtils.equals(status.getEvent().getId(), event.getId())
                && StringUtils.equals(status.getEntityId(), entityId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveEventStatus(EventStatus eventStatus) throws Exception
    {
        ActivityEventStatus status = eventConverter.convertEventStatusToActivityStatus(eventStatus);

        if (configuration.useLocalStore()) {
            saveEventStatusInStore(status);

            this.observation.notify(new EventStatusAddOrUpdatedEvent(), eventStatus);
        }

        if (configuration.useMainStore()) {
            XWikiContext context = contextProvider.get();
            // store event in the main database
            String oriDatabase = context.getWikiId();
            context.setWikiId(context.getMainXWiki());
            try {
                saveEventStatusInStore(status);
            } finally {
                context.setWikiId(oriDatabase);
            }

            this.observation.notify(new EventStatusAddOrUpdatedEvent(), eventStatus);
        }
    }

    private void saveEventStatusInStore(ActivityEventStatus eventStatus) throws ActivityStreamException
    {
        XWikiContext context = contextProvider.get();
        XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
        try {
            hibernateStore.beginTransaction(context);
            Session session = hibernateStore.getSession(context);
            session.save(eventStatus);
            hibernateStore.endTransaction(context, true);
        } catch (XWikiException e) {
            hibernateStore.endTransaction(context, false);
            throw new ActivityStreamException(e);
        }
    }
}
