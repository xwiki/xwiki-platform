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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStatusManager;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Legacy implementation of {@link EventStatusManager} which use the Activity Stream storage.
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Singleton
public class LegacyEventStatusManager implements EventStatusManager
{
    @Inject
    private QueryManager queryManager;

    @Inject
    private LegacyEventConverter eventConverter;

    @Inject
    private LegacyEventStreamStoreConfiguration configuration;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NamespaceContextExecutor namespaceContextExecutor;

    @Override
    public List<EventStatus> getEventStatus(List<Event> events, List<String> entityIds) throws Exception
    {
        List<EventStatus> results = new ArrayList<>();

        // Don't perform any query if the list of events or entities is actually empty
        if (events.isEmpty() || entityIds.isEmpty()) {
            return results;
        }

        // Get the ActivityEventStatus from the database and convert them
        Query query = queryManager.createQuery("select eventStatus from LegacyEventStatus eventStatus "
            + "where eventStatus.activityEvent.id in :eventIds and eventStatus.entityId in :entityIds", Query.HQL);
        query.bindValue("eventIds", getEventIds(events));
        query.bindValue("entityIds", entityIds);
        for (LegacyEventStatus legacyEventStatus : query.<LegacyEventStatus>execute()) {
            results.add(new DefaultEventStatus(
                eventConverter.convertLegacyActivityToEvent(legacyEventStatus.getActivityEvent()),
                legacyEventStatus.getEntityId(), legacyEventStatus.isRead()));
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
        LegacyEventStatus status = eventConverter.convertEventStatusToLegacyActivityStatus(eventStatus);

        boolean isSavedOnMainStore = false;

        if (configuration.useLocalStore()) {
            String currentWiki = wikiDescriptorManager.getCurrentWikiId();
            saveEventStatusInStore(status, currentWiki);
            isSavedOnMainStore = wikiDescriptorManager.isMainWiki(currentWiki);
        }

        if (configuration.useMainStore() && !isSavedOnMainStore) {
            // save event into the main database (if the event was not already be recorded on the main store,
            // otherwise we would duplicate the event)
            saveEventStatusInStore(status, wikiDescriptorManager.getMainWikiId());
        }
    }

    @Override
    @Deprecated
    public void deleteEventStatus(EventStatus eventStatus) throws Exception
    {
        LegacyEventStatus status = eventConverter.convertEventStatusToLegacyActivityStatus(eventStatus);

        boolean isSavedOnMainStore = false;

        if (configuration.useLocalStore()) {
            String currentWiki = wikiDescriptorManager.getCurrentWikiId();
            deleteEventStatusFromStore(status, currentWiki);
            isSavedOnMainStore = wikiDescriptorManager.isMainWiki(currentWiki);
        }

        if (configuration.useMainStore() && !isSavedOnMainStore) {
            // save event into the main database (if the event was not already be recorded on the main store,
            // otherwise we would duplicate the event)
            deleteEventStatusFromStore(status, wikiDescriptorManager.getMainWikiId());
        }
    }

    private void saveEventStatusInStore(LegacyEventStatus eventStatus, String wikiId) throws Exception
    {
        namespaceContextExecutor.execute(new WikiNamespace(wikiId), () -> {
            XWikiContext context = contextProvider.get();
            XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
            try {
                hibernateStore.executeWrite(context, session -> {
                    // The event status may already exists, so we use saveOrUpdate
                    session.saveOrUpdate(eventStatus);

                    return null;
                });
            } catch (XWikiException e) {
                throw new EventStreamException(e);
            }

            return null;
        });
    }

    private void deleteEventStatusFromStore(LegacyEventStatus eventStatus, String wikiId) throws Exception
    {
        namespaceContextExecutor.execute(new WikiNamespace(wikiId), () -> {
            XWikiContext context = contextProvider.get();
            XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
            try {
                hibernateStore.executeWrite(context, session -> {
                    // The event status may already exists, so we use saveOrUpdate
                    session.delete(eventStatus);

                    return null;
                });
            } catch (XWikiException e) {
                throw new EventStreamException(e);
            }

            return null;
        });
    }

    @Override
    @Deprecated
    public void deleteAllForEntity(Date startDate, String entityId) throws Exception
    {
        boolean isSavedOnMainStore = false;

        if (configuration.useLocalStore()) {
            String currentWiki = wikiDescriptorManager.getCurrentWikiId();
            deleteAllForEntityInStore(startDate, entityId, currentWiki);
            isSavedOnMainStore = wikiDescriptorManager.isMainWiki(currentWiki);
        }

        if (configuration.useMainStore() && !isSavedOnMainStore) {
            // save event into the main database (if the event was not already be recorded on the main store,
            // otherwise we would duplicate the event)
            deleteAllForEntityInStore(startDate, entityId, wikiDescriptorManager.getMainWikiId());
        }
    }

    private void deleteAllForEntityInStore(Date startDate, String entityId, String wikiId) throws Exception
    {
        namespaceContextExecutor.execute(new WikiNamespace(wikiId), () -> {
            XWikiContext context = contextProvider.get();
            XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();

            try {
                hibernateStore.executeWrite(context,
                    session -> deleteAllForEntityInStore(session, startDate, entityId));
            } catch (XWikiException e) {
                throw new EventStreamException(e);
            }

            return null;
        });
    }

    private Object deleteAllForEntityInStore(Session session, Date startDate, String entityId)
    {
        StringBuilder statement =
            new StringBuilder("delete from LegacyEventStatus status where status.entityId = :entityId");

        if (startDate != null) {
            statement.append(" and status.activityEvent in ");
            statement.append("(select event from LegacyEvent event where event.date < :startDate)");
        }

        org.hibernate.query.Query query = session.createQuery(statement.toString());
        query.setParameter("entityId", entityId);
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }

        query.executeUpdate();

        return null;
    }

    /**
     * @param session the Hibernate session
     * @param eventId the identifier of the event associated with the statuses to remove
     * @since 13.1RC1
     * @since 12.10.5
     * @since 12.6.8
     */
    public void deleteAllForEventInStore(Session session, String eventId)
    {
        StringBuilder statement =
            new StringBuilder("delete from LegacyEventStatus status where status.activityEvent.id = :eventId");

        org.hibernate.query.Query query = session.createQuery(statement.toString());
        query.setParameter("eventId", eventId);

        query.executeUpdate();
    }
}
