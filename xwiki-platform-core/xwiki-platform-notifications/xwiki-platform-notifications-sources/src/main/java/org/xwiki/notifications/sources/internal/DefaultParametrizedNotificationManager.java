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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.GroupingEventManager;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.internal.status.EventReadAlertFilter;
import org.xwiki.notifications.filters.internal.status.EventReadEmailFilter;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

/**
 * Default implementation of {@link ParametrizedNotificationManager}.
 *
 * @version $Id$
 * @since 10.4RC1
 */
@Component
@Singleton
public class DefaultParametrizedNotificationManager implements ParametrizedNotificationManager
{
    /**
     * the maximal size of events to fetch in a single batch.
     */
    private static final int MAX_BATCH_SIZE = 1280;

    @Inject
    private EventSearcher eventSearcher;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private GroupManager groupManager;

    @Inject
    @Named(EventReadAlertFilter.FILTER_NAME)
    private NotificationFilter eventReadAlertFilter;

    @Inject
    @Named(EventReadEmailFilter.FILTER_NAME)
    private NotificationFilter eventReadEmailFilter;

    @Inject
    private RecordableEventDescriptorHelper recordableEventDescriptorHelper;

    @Inject
    private PreferenceDateNotificationFilter preferenceDateNotificationFilter;

    @Inject
    private GroupingEventManager groupingEventManager;

    @Inject
    private Logger logger;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Override
    public List<CompositeEvent> getEvents(NotificationParameters parameters) throws NotificationException
    {
        List<CompositeEvent> compositeEvents = new ArrayList<>();
        getRawEvents(parameters, compositeEvents);
        return compositeEvents;
    }

    @Override
    public List<Event> getRawEvents(NotificationParameters parameters) throws NotificationException
    {
        return getRawEvents(parameters, null);
    }

    private List<Event> getRawEvents(NotificationParameters parameters, List<CompositeEvent> compositeEvents)
        throws NotificationException
    {
        if (Boolean.TRUE.equals(parameters.onlyUnread) && !parameters.filters.contains(eventReadAlertFilter)) {
            parameters.filters.add(eventReadAlertFilter);
        }
        if (Boolean.TRUE.equals(parameters.onlyUnread) && !parameters.filters.contains(eventReadEmailFilter)) {
            parameters.filters.add(eventReadEmailFilter);
        }
        List<Event> results = new ArrayList<>();

        // Because the user might not be able to see all notifications because of the rights, we take from the database
        // more events than expected and we will filter afterwards.
        int batchSize = parameters.expectedCount * 2;
        int offset = 0;
        try {
            boolean done = false;
            while (!done) {
                // Get a batch of events
                List<Event> batch = this.eventSearcher.searchEvents(offset, batchSize, parameters);

                done = addMatchingEventsToResults(batch, parameters, results, compositeEvents);
                if (!done) {
                    if (batch.size() < batchSize) {
                        // there are no more results to expect. stop.
                        done = true;
                    } else {
                        // grab a larger batch size next time to get more possible results
                        offset += batchSize;
                        if (batchSize < MAX_BATCH_SIZE) {
                            batchSize <<= 1;
                        }
                    }
                }
            }

            return results;
        } catch (Exception e) {
            throw new NotificationException("Fail to get the list of notifications.", e);
        }
    }

    private boolean addMatchingEventsToResults(List<Event> batch, NotificationParameters parameters,
        List<Event> results, List<CompositeEvent> compositeEvents) throws EventStreamException, NotificationException
    {
        boolean done = false;
        // Add to the results the events the user has the right to see
        for (Event event : batch) {
            DocumentReference document = event.getDocument();
            // 1) Don't include events concerning a doc the passed user cannot see
            // 2) If the current user is not the passed user, also make sure that the current user has view permissions
            // on the document as otherwise any user could forget a REST request and see other user's notifications
            if (document != null && !isAllowed(parameters.user, document)) {
                continue;
            }

            if (filterEvent(event, parameters)) {
                continue;
            }

            // Record this event
            results.add(event);

            int reachedSize = results.size();

            // if what's requested is the composite events, then we only stop when the number of composite events
            // is reached
            if (compositeEvents != null) {
                UserReference userReference = null;
                if (parameters.user != null) {
                    userReference = this.userReferenceResolver.resolve(parameters.user);
                }
                this.groupingEventManager.augmentCompositeEvents(compositeEvents, List.of(event), userReference,
                    parameters.groupingEventTarget);
                reachedSize = compositeEvents.size();
            }
            // If the expected count is reached, stop now
            if (reachedSize >= parameters.expectedCount) {
                done = true;
                break;
            }
        }
        return done;
    }

    private boolean isAllowed(DocumentReference passedUser, DocumentReference document)
    {
        boolean allowed = this.authorizationManager.hasAccess(Right.VIEW, passedUser, document);
        if (allowed) {
            allowed = this.contextualAuthorizationManager.hasAccess(Right.VIEW, document);
        }
        return allowed;
    }

    /**
     * Determine if the given user reference is targeted by the event targets.
     * 
     * @param event the event that is tested
     * @param userReference the user reference given by the parameters
     * @return {@code true} iff the user is explicitely target, or through a group.
     */
    private boolean eventTargetUser(Event event, DocumentReference userReference)
    {
        boolean result = false;
        if (event.getTarget() != null && !event.getTarget().isEmpty() && userReference != null) {
            String serializedUserReference = this.serializer.serialize(userReference);

            // if the target explicitely contains the user reference we're good
            if (event.getTarget().contains(serializedUserReference)) {
                result = true;
                // else we need to check that the targets does not contain any group the user belongs to
                // note that we do the check by starting from the user groups for better scalability
                // compared to loading each targets individually
            } else {
                try {
                    Collection<DocumentReference> groups = this.groupManager.getGroups(userReference, null, true);
                    for (DocumentReference group : groups) {
                        if (event.getTarget().contains(this.serializer.serialize(group))) {
                            result = true;
                            break;
                        }
                    }
                } catch (GroupException e) {
                    logger.error("Error while checking groups for user [{}]", userReference, e);
                }
            }
        }
        return result;
    }

    private boolean filterEvent(Event event, NotificationParameters parameters) throws EventStreamException
    {
        // Don't record events that have a target that don't include the current user
        if (!event.getTarget().isEmpty()
            && (parameters.user == null || !this.eventTargetUser(event, parameters.user))) {
            return true;
        }

        // Don't record events that concern an event type for which we don't have a descriptor and
        // don't record events that are before the starting date of the corresponding preference (the query do not
        // guarantee that)
        if (!recordableEventDescriptorHelper.hasDescriptor(event.getType(), parameters.user)
            || preferenceDateNotificationFilter.shouldFilter(event, parameters.preferences)) {
            return true;
        }

        List<NotificationFilter> filters = new ArrayList<>(parameters.filters);
        Collections.sort(filters);
        for (NotificationFilter filter : filters) {
            NotificationFilter.FilterPolicy policy =
                filter.filterEvent(event, parameters.user, parameters.filterPreferences, parameters.format);
            switch (policy) {
                case FILTER:
                    return true;
                case KEEP:
                    return false;
                default:
                    // Do nothing
            }
        }

        return false;
    }
}
