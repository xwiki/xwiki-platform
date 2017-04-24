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
package org.xwiki.notifications.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationManager;
import org.xwiki.notifications.NotificationPreference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.StringUtils;

/**
 * Default implementation of {@link NotificationManager}.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
public class DefaultNotificationManager implements NotificationManager
{
    @Inject
    private EventStream eventStream;

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("user")
    private ConfigurationSource userPreferencesSource;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private SimilarityCalculator similarityCalculator;

    @Override
    public List<CompositeEvent> getEvents(String userId, boolean onlyUnread, int expectedCount)
            throws NotificationException
    {
        return getEvents(
                new ArrayList<>(),
                documentReferenceResolver.resolve(userId),
                onlyUnread,
                expectedCount,
                null,
                new ArrayList<>()
        );
    }

    @Override
    public List<CompositeEvent> getEvents(String userId, boolean onlyUnread, int count, Date untilDate,
            List<String> blackList) throws NotificationException
    {
        return getEvents(new ArrayList<>(), documentReferenceResolver.resolve(userId), onlyUnread, count, untilDate,
                new ArrayList<>(blackList));
    }

    @Override
    public long getEventsCount(String userId, boolean onlyUnread, int maxCount) throws NotificationException
    {
        DocumentReference user = documentReferenceResolver.resolve(userId);

        List<CompositeEvent> events = getEvents(new ArrayList<>(), user, onlyUnread, maxCount,
                null, new ArrayList<>());

        return events.size();
    }

    private List<CompositeEvent> getEvents(List<CompositeEvent> results, DocumentReference userReference,
            boolean onlyUnread, int expectedCount, Date endDate, List<String> blackList) throws NotificationException
    {
        // Because the user might not be able to see all notifications because of the rights, we take from the database
        // more events than expected and we will filter afterwards.
        final int batchSize = expectedCount * 2;
        try {
            // Create the query
            Query query = generateQuery(userReference, onlyUnread, endDate, blackList);
            if (query == null) {
                return Collections.emptyList();
            }
            query.setLimit(batchSize);

            // Get a batch of events
            List<Event> batch = eventStream.searchEvents(query);

            // Add to the results the events the user has the right to see
            for (Event event : batch) {
                DocumentReference document = event.getDocument();
                // Don't record events concerning a doc the user cannot see
                if (document != null && !authorizationManager.hasAccess(Right.VIEW, userReference, document)) {
                    continue;
                }
                // Record this event
                recordEvent(results, event);
                // If the expected count is reached, stop now
                if (results.size() >= expectedCount) {
                    return results;
                }
            }

            // If we haven't get the expected number of events, perform a new batch
            if (results.size() < expectedCount && batch.size() == batchSize) {
                blackList.addAll(getEventsIds(batch));
                getEvents(results, userReference, onlyUnread, expectedCount - results.size(), endDate,
                        blackList);
            }

            return results;
        } catch (Exception e) {
            throw new NotificationException("Fail to get the list of notifications.", e);
        }
    }

    private class BestSimilarity
    {
        public int value;
        public CompositeEvent compositeEvent;
        public Event event;

        public boolean isCompositeEventCompatibleWith(Event event)
        {
            // Here we have a composite event made of A and B.
            // - if A is a "create" or an "update" event
            // - if A and B have the same groupId (which means A or B is a "create" or an "update" event basically)
            // - if B has the same type than E
            // (or vice versa)
            // It means the "update" event A has been triggered for technical reason, but the interesting event is
            // B, which we can group with the event E even if it lowers the similarity between the events.
            return compositeEvent.getSimilarityBetweenEvents() >= SimilarityCalculator.SAME_GROUP_ID
                    && compositeEvent.getType().equals(event.getType());
        }
    }

    private void recordEvent(List<CompositeEvent> results, Event event) throws NotificationException
    {
        BestSimilarity bestSimilarity = getBestSimilarity(results, event);

        if (bestSimilarity.compositeEvent != null) {
            if (bestSimilarity.value > bestSimilarity.compositeEvent.getSimilarityBetweenEvents()
                    && bestSimilarity.compositeEvent.getEvents().size() > 1) {
                // We have found an event A inside a composite event C1 that have a greater similarity with the event E
                // than the similarity between events (A, B, C) of that composite event (C1).
                //
                // It means we must remove the existing event A from that composite event C1 and create a new composite
                // event C2 made of A and E.
                bestSimilarity.compositeEvent.remove(bestSimilarity.event);

                // Instead of creating a new composite event with A and E, we first look if an other composite event can
                // match with A and E.
                BestSimilarity bestSecondChoice = getBestSimilarity(results, event);
                if (bestSecondChoice.compositeEvent != null
                        && bestSecondChoice.isCompositeEventCompatibleWith(event)) {
                    // We have found a composite event C2 made of events (X, Y) which have a greater similarity between
                    // themselves than between X and the event E.
                    // It means we cannot add E in C2.
                    // But there is actually an exception:
                    // - if X is a "create" or an "update" event
                    // - if X and Y have the same groupId
                    // - if Y has the same type than E
                    // (or vice versa)
                    // It means the "update" event X has been triggered for technical reason, but the interesting event
                    // is Y, which we can group with the event E.
                    bestSecondChoice.compositeEvent.add(bestSimilarity.event,
                            bestSecondChoice.compositeEvent.getSimilarityBetweenEvents());
                    bestSecondChoice.compositeEvent.add(event,
                            bestSecondChoice.compositeEvent.getSimilarityBetweenEvents());
                } else {
                    CompositeEvent newCompositeEvent = new CompositeEvent(event);
                    newCompositeEvent.add(bestSimilarity.event, bestSimilarity.value);
                    results.add(newCompositeEvent);
                }

                return;
            } else if (bestSimilarity.value >= bestSimilarity.compositeEvent.getSimilarityBetweenEvents()) {
                // We have found a composite event C1 made of events (A, B, C) which have the same similarity between
                // themselves than between A end E.
                // All we need to do it to add E to C1.
                bestSimilarity.compositeEvent.add(event, bestSimilarity.value);
                return;
            } else if (bestSimilarity.isCompositeEventCompatibleWith(event)) {
                // We have found a composite event C1 made of events (A, B) which have a greater similarity between
                // themselves than between A and the event E.
                // It means we cannot add E in C1.
                // But there is actually an exception:
                // - if A is a "create" or an "update" event
                // - if A and B have the same groupId
                // - if B has the same type than E
                // (or vice versa)
                // It means the "update" event A has been triggered for technical reason, but the interesting event is
                // B, which we can group with the event E.
                bestSimilarity.compositeEvent.add(event,
                        bestSimilarity.compositeEvent.getSimilarityBetweenEvents());
                return;
            }
        }
        // We haven't found an event that is similar to the current one, so we create a new composite event
        results.add(new CompositeEvent(event));
    }

    private BestSimilarity getBestSimilarity(List<CompositeEvent> results, Event event)
    {
        BestSimilarity bestSimilarity = new BestSimilarity();

        // Looking for the most similar event inside the existing composite events
        for (CompositeEvent existingCompositeEvent : results) {
            for (Event existingEvent : existingCompositeEvent.getEvents()) {
                int similarity = similarityCalculator.computeSimilarity(event, existingEvent);
                if (similarity > bestSimilarity.value) {
                    bestSimilarity.value = similarity;
                    bestSimilarity.event = existingEvent;
                    bestSimilarity.compositeEvent = existingCompositeEvent;
                }
            }
        }

        return bestSimilarity;
    }

    @Override
    public List<NotificationPreference> getPreferences() throws NotificationException
    {
        return getPreferences(documentAccessBridge.getCurrentUserReference());
    }

    @Override
    public List<NotificationPreference> getPreferences(String userId) throws NotificationException
    {
        return getPreferences(documentReferenceResolver.resolve(userId));
    }

    @Override
    public void setStartDate(String userId, Date startDate) throws NotificationException
    {
        modelBridge.setStartDateForUser(documentReferenceResolver.resolve(userId), startDate);
    }

    private Query generateQuery(DocumentReference user, boolean onlyUnread, Date endDate, List<String> blackList)
            throws NotificationException, QueryException
    {
        // TODO: create a role so extensions can inject their own complex query parts
        // TODO: create unit tests for all use-cases
        // TODO: idea: handle the items of the watchlist too

        // First: get the preferences of the given user
        List<NotificationPreference> preferences = getPreferences(user);

        // Then: generate the HQL query
        StringBuilder hql = new StringBuilder();
        hql.append("where event.date >= :startDate AND event.user <> :user AND (");

        List<String> types = handleEventTypes(hql, preferences);
        List<String> apps  = handleApplications(hql, preferences, types);

        // No notification is returned if nothing is saved in the user settings
        // TODO: handle some defaults preferences that can be set in the administration
        if (preferences.isEmpty() || (types.isEmpty() && apps.isEmpty())) {
            return null;
        }

        hql.append(")");

        handleBlackList(blackList, hql);
        handleEndDate(endDate, hql);
        handleHiddenEvents(hql);
        handleEventStatus(onlyUnread, hql);
        handleOrder(hql);

        // The, generate the query
        Query query = queryManager.createQuery(hql.toString(), Query.HQL);

        // Bind values
        query.bindValue("startDate", modelBridge.getUserStartDate(user));
        query.bindValue("user", serializer.serialize(user));
        handleEventTypes(types, query);
        handleApplications(apps, query);
        handleBlackList(blackList, query);
        handleEndDate(endDate, query);

        // Return the query
        return query;
    }

    private void handleEndDate(Date endDate, Query query)
    {
        if (endDate != null) {
            query.bindValue("endDate", endDate);
        }
    }

    private void handleBlackList(List<String> blackList, Query query)
    {
        if (blackList != null && !blackList.isEmpty()) {
            query.bindValue("blackList", blackList);
        }
    }

    private void handleEndDate(Date endDate, StringBuilder hql)
    {
        if (endDate != null) {
            hql.append(" AND event.date <= :endDate");
        }
    }

    private void handleBlackList(List<String> blackList, StringBuilder hql)
    {
        if (blackList != null && !blackList.isEmpty()) {
            hql.append(" AND event.id NOT IN (:blackList)");
        }
    }

    private List<String> getEventsIds(List<Event> events)
    {
        List<String> list = new ArrayList<>();
        for (Event event : events) {
            list.add(event.getId());
        }
        return list;
    }

    private List<NotificationPreference> getPreferences(DocumentReference user) throws NotificationException
    {
        return modelBridge.getNotificationsPreferences(user);
    }

    private void handleOrder(StringBuilder hql)
    {
        hql.append(" order by event.date DESC");
    }

    private void handleEventStatus(boolean onlyUnread, StringBuilder hql)
    {
        if (onlyUnread) {
            hql.append(" AND (event not in (select status.activityEvent from ActivityEventStatusImpl status "
                    + "where status.activityEvent = event and status.entityId = :user and status.read = true))");
        }
    }

    private void handleHiddenEvents(StringBuilder hql)
    {
        // Don't show hidden events unless the user want to display hidden pages
        if (userPreferencesSource.getProperty("displayHiddenDocuments", 0) == 0) {
            hql.append(" AND event.hidden <> true");
        }
    }

    private void handleApplications(List<String> apps, Query query)
    {
        if (!apps.isEmpty()) {
            query.bindValue("apps", apps);
        }
    }

    private void handleEventTypes(List<String> types, Query query)
    {
        if (!types.isEmpty()) {
            query.bindValue("types", types);
        }
    }

    private List<String> handleApplications(StringBuilder hql, List<NotificationPreference> preferences,
            List<String> types)
    {
        List<String> apps = new ArrayList<>();
        for (NotificationPreference preference : preferences) {
            if (preference.isNotificationEnabled() && StringUtils.isNotBlank(preference.getApplicationId())) {
                apps.add(preference.getApplicationId());
            }
        }
        if (!apps.isEmpty()) {
            hql.append((types.isEmpty() ? "" : " OR ") + "event.application IN (:apps)");
        }
        return apps;
    }

    private List<String> handleEventTypes(StringBuilder hql, List<NotificationPreference> preferences)
    {
        List<String> types = new ArrayList<>();
        for (NotificationPreference preference : preferences) {
            if (preference.isNotificationEnabled() && StringUtils.isNotBlank(preference.getEventType())) {
                types.add(preference.getEventType());
            }
        }
        if (!types.isEmpty()) {
            hql.append("event.type IN (:types)");
        }
        return types;
    }
}
