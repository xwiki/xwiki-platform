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
package org.xwiki.notifications.filters.internal.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.internal.LocationOperatorNodeGenerator;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;

/**
 * @version $Id$
 * @since 9.9RC1
 */
@Component(roles = ScopeNotificationFilterExpressionGenerator.class)
@Singleton
public class ScopeNotificationFilterExpressionGenerator
{
    protected static final String ERROR = "Failed to filter the notifications.";

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private LocationOperatorNodeGenerator locationOperatorNodeGenerator;

    @Inject
    protected Logger logger;

    public AbstractNode filterExpression(DocumentReference user, NotificationPreference preference)
    {
        // The node we construct
        AbstractOperatorNode topNode = null;

        // The filters
        Iterator<LocationFilter> it = getExclusiveFiltersThatHasNoParents(
                computeParentFilters(generateLocationFilters(user, preference)));

        while (it.hasNext()) {
            LocationFilter filter = it.next();

            AbstractOperatorNode filterNode = filter.getNode();
            // Add children
            for (LocationFilter childFilter : filter.getChildren()) {
                filterNode = filterNode.or(childFilter.getNode());
            }

            if (topNode == null) {
                topNode = filterNode;
            } else {
                topNode = topNode.and(filterNode);
            }
        }

        return topNode;
    }

    private Iterator<LocationFilter> getExclusiveFiltersThatHasNoParents(List<LocationFilter> filters)
    {
        return filters.stream().filter(
                filter -> !filter.hasParent() && filter.getType() == NotificationFilterType.EXCLUSIVE
        ).iterator();
    }

    private List<LocationFilter> computeParentFilters(List<LocationFilter> filters)
    {
        // Compare filters 2 by 2 to see if some are children of the other
        for (LocationFilter filter : filters) {
            for (LocationFilter otherFilter : filters) {
                if (filter == otherFilter) {
                    continue;
                }
                if (otherFilter.isParentOf(filter)) {
                    otherFilter.getChildren().add(filter);
                    filter.setHasParent(true);
                }
            }
        }

        return filters;
    }

    /**
     * @param filterPreference a filter preference
     * @return either or not the preference should be applied to all events
     */
    private boolean matchAllEvents(NotificationFilterPreference filterPreference)
    {
        // When the list of event types concerned by the filter is empty, we consider that the filter concerns
        // all events.
        return filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE).isEmpty();
    }

    /**
     * @param filterPreference a filter preference
     * @param preference a notification preference
     * @return if the filter preference concerns the event of the notification preference
     */
    private boolean matchEventType(NotificationFilterPreference filterPreference, NotificationPreference preference)
    {
        // The event types concerned by the filter
        List<String> filterEventTypes = filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE);

        // The event type concerned by the notification preference
        Object preferenceEventType = preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE);

        // There is a match of the preference event type is not blank (it should not...) and if the filter concerns it
        // (or all events)
        return preferenceEventType != null && StringUtils.isNotBlank((String) preferenceEventType)
                && (filterEventTypes.contains(preferenceEventType) || filterEventTypes.isEmpty());
    }

    private List<LocationFilter> generateLocationFilters(DocumentReference user, NotificationPreference eventTypePref)
    {
        List<LocationFilter> filters = new ArrayList<>();

        try {
            // Get every filterPreference linked to the current filter
            Set<NotificationFilterPreference> filterPreferences = notificationFilterManager.getFilterPreferences(user);

            Stream<NotificationFilterPreference> filterPreferenceStream = filterPreferences.stream().filter(
                    pref -> ScopeNotificationFilter.FILTER_NAME.equals(pref.getFilterName())
                    && ( matchAllEvents(pref) || matchEventType(pref, eventTypePref) )
            );

            Iterator<NotificationFilterPreference> iterator = filterPreferenceStream.iterator();
            while (iterator.hasNext()) {

                ScopeNotificationFilterPreference filterPref = new ScopeNotificationFilterPreference(iterator.next(),
                        entityReferenceResolver);

                AbstractOperatorNode filterNode
                        = locationOperatorNodeGenerator.generateNode(filterPref.getScopeReference());

                // If we have an EXCLUSIVE filter, negate the filter node
                if (filterPref.getFilterType().equals(NotificationFilterType.EXCLUSIVE)) {
                    filterNode = not(filterNode);
                }

                LocationFilter filter = new LocationFilter(filterNode, filterPref.getScopeReference(),
                        filterPref.getFilterType());
                filters.add(filter);
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        return filters;
    }
}
