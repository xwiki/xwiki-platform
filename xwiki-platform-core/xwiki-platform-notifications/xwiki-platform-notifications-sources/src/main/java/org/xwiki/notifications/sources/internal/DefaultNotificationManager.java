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
package org.xwiki.notifications.sources.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.internal.SimilarityCalculator;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.sources.NotificationManager;
import org.xwiki.query.Query;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

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
    private QueryGenerator queryGenerator;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private SimilarityCalculator similarityCalculator;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * For internal use, avoid to give more than 7 parameters to methods.
     */
    private class Parameters
    {
        public DocumentReference userReference;
        public NotificationFormat format;
        public int expectedCount;
        public Date endDate;
        public Date fromDate;
        public List<String> blackList;
        private String userId;

        Parameters(DocumentReference userReference, NotificationFormat format, int expectedCount,
                Date endDate, Date fromDate, List<String> blackList)
        {
            this.userReference = userReference;
            this.format = format;
            this.expectedCount = expectedCount;
            this.endDate = endDate;
            this.fromDate = fromDate;
            this.blackList = blackList;
            this.userId = entityReferenceSerializer.serialize(userReference);
        }
    }

    @Override
    public List<CompositeEvent> getEvents(String userId, int expectedCount)
            throws NotificationException
    {
        return getEvents(new ArrayList<>(),
                new Parameters(
                    documentReferenceResolver.resolve(userId),
                    NotificationFormat.ALERT,
                    expectedCount,
                    null,
                    null,
                    new ArrayList<>()
                )
        );
    }

    @Override
    public List<CompositeEvent> getEvents(String userId, int count, Date untilDate, List<String> blackList)
            throws NotificationException
    {
        return getEvents(new ArrayList<>(),
                new Parameters(
                        documentReferenceResolver.resolve(userId),
                        NotificationFormat.ALERT,
                        count,
                        untilDate,
                        null,
                        new ArrayList<>(blackList)
                )
        );
    }

    @Override
    public List<CompositeEvent> getEvents(String userId, int expectedCount, Date untilDate, Date fromDate,
            List<String> blackList) throws NotificationException
    {
        return getEvents(new ArrayList<>(),
                new Parameters(
                        documentReferenceResolver.resolve(userId),
                        NotificationFormat.ALERT,
                        expectedCount,
                        untilDate,
                        fromDate,
                        new ArrayList<>(blackList)
                )
        );
    }

    @Override
    public List<CompositeEvent> getEvents(String userId, NotificationFormat format, int expectedCount, Date untilDate,
            Date fromDate, List<String> blackList) throws NotificationException
    {
        return getEvents(new ArrayList<>(),
                new Parameters(
                        documentReferenceResolver.resolve(userId),
                        format,
                        expectedCount,
                        untilDate,
                        fromDate,
                        new ArrayList<>(blackList)
                )
        );
    }

    @Override
    public long getEventsCount(String userId, int maxCount) throws NotificationException
    {
        DocumentReference user = documentReferenceResolver.resolve(userId);

        List<CompositeEvent> events = getEvents(new ArrayList<>(),
                new Parameters(
                        user,
                        NotificationFormat.ALERT,
                        maxCount,
                        null,
                        null,
                        new ArrayList<>()
                )
        );

        return events.size();
    }

    private List<CompositeEvent> getEvents(List<CompositeEvent> results, Parameters parameters)
            throws NotificationException
    {
        // Because the user might not be able to see all notifications because of the rights, we take from the database
        // more events than expected and we will filter afterwards.
        final int batchSize = parameters.expectedCount * 2;
        try {
            // Create the query
            Query query = queryGenerator.generateQuery(parameters.userReference, parameters.format, parameters.endDate,
                    parameters.fromDate, parameters.blackList);
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
                if (document != null && !authorizationManager.hasAccess(Right.VIEW, parameters.userReference,
                        document)) {
                    continue;
                }

                if (filterEvent(event, parameters)) {
                    continue;
                }

                // Record this event
                recordEvent(results, event);
                // If the expected count is reached, stop now
                if (results.size() >= parameters.expectedCount) {
                    return results;
                }
            }

            // If we haven't get the expected number of events, perform a new batch
            if (results.size() < parameters.expectedCount && batch.size() == batchSize) {
                parameters.blackList.addAll(getEventsIds(batch));
                getEvents(results, parameters);
            }

            return results;
        } catch (Exception e) {
            throw new NotificationException("Fail to get the list of notifications.", e);
        }
    }

    private boolean filterEvent(Event event, Parameters parameters) throws NotificationException
    {
        // Don't record events that have a target that don't include the current user
        if (!event.getTarget().isEmpty() && !event.getTarget().contains(parameters.userId)) {
            return true;
        }

        for (NotificationFilter filter : notificationFilterManager.getAllFilters(parameters.userReference)) {
            if (filter.filterEvent(event, parameters.userReference, parameters.format)) {
                return true;
            }
        }

        return false;
    }

    private List<String> getEventsIds(List<Event> events)
    {
        return events.stream().map(Event::getId).collect(Collectors.toList());
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
            return compositeEvent.getSimilarityBetweenEvents()
                    >= SimilarityCalculator.SAME_GROUP_ID_AND_DOCUMENT_BUT_DIFFERENT_TYPES
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
                if (similarity < existingCompositeEvent.getSimilarityBetweenEvents()) {
                    // Penality
                    similarity -= 5;
                }
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
        return notificationPreferenceManager.getAllPreferences(
                documentAccessBridge.getCurrentUserReference());
    }

    @Override
    public List<NotificationPreference> getPreferences(String userId) throws NotificationException
    {
        return notificationPreferenceManager.getAllPreferences(documentReferenceResolver.resolve(userId));
    }

    @Override
    public void setStartDate(String userId, Date startDate) throws NotificationException
    {
        notificationPreferenceManager.setStartDateForUser(documentReferenceResolver.resolve(userId), startDate);
    }
}
