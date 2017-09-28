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
package org.xwiki.notifications.filters.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;

/**
 * Helper to implement {@link ScopeNotificationFilter} and {@link UsersNotificationFilter}.
 *
 * @param <T> the specialized type of the notification filter preference
 * @version $Id$
 * @since 9.8RC1
 */
public abstract class AbstractScopeOrUserNotificationFilter<T extends NotificationFilterPreference>
        extends AbstractNotificationFilter
{
    protected static final String ERROR = "Failed to filter the notifications.";

    @Inject
    protected NotificationFilterManager notificationFilterManager;

    @Inject
    protected Logger logger;

    protected String filterName;

    /**
     * Construct a AbstractScopeOrUserNotificationFilter.
     * @param filterName name of the filter
     */
    public AbstractScopeOrUserNotificationFilter(String filterName)
    {
        this.filterName = filterName;
    }

    @Override
    public boolean filterEventByFilterType(Event event, DocumentReference user, NotificationFormat format,
            NotificationFilterType filterType)
    {
        // Indicate if a restriction exist concerning this type of event
        boolean hasRestriction = false;
        // Indicate if we should keep the event when a restriction exists
        boolean keepTheEvent = false;

        try {
            int maxDeepLevel = 0;
            Iterator<NotificationFilterPreference> iterator = getFilterPreferences(user, format, filterType);
            while (iterator.hasNext()) {

                T preference = convertPreferences(iterator.next());

                List<String> concernedEventTypes = preference.getProperties(NotificationFilterProperty.EVENT_TYPE);
                if (concernedEventTypes.isEmpty() || concernedEventTypes.contains(event.getType())) {
                    hasRestriction = true;

                    if (matchRestriction(event, preference)) {
                        int deepLevel = getDeepLevel(preference);
                        if (deepLevel > maxDeepLevel) {
                            maxDeepLevel = deepLevel;
                            keepTheEvent = filterType.equals(NotificationFilterType.INCLUSIVE);
                        } else if (deepLevel == maxDeepLevel && filterType.equals(NotificationFilterType.EXCLUSIVE)) {
                            keepTheEvent = false;
                        }
                    }
                }
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        return !hasRestriction || keepTheEvent;
    }

    protected abstract T convertPreferences(NotificationFilterPreference pref);

    protected abstract boolean matchRestriction(Event event, T preference)
            throws NotificationException;

    protected Iterator<NotificationFilterPreference> getFilterPreferences(DocumentReference user,
            NotificationFormat format, NotificationFilterType filterType) throws NotificationException
    {
        return getFilterPreferencesIterator(
                notificationFilterManager.getFilterPreferences(user, this, filterType, format));
    }

    protected Iterator<NotificationFilterPreference> getFilterPreferencesIterator(
            Collection<NotificationFilterPreference> preferences)
    {
        return preferences.stream().filter(filter()).iterator();
    }

    protected Predicate<NotificationFilterPreference> filter()
    {
        return preference -> filterName.equals(preference.getFilterName());
    }

    /**
     * Given a {@link NotificationFilterPreference} and the current filtering context (defined by a
     * {@link NotificationPreference}), determine if a the current filter should apply with the given scope.
     *
     * Note that we allow the {@link NotificationPreference} to be null and the
     * {@link NotificationFilterPreference} to have an empty {@link NotificationPreferenceProperty#EVENT_TYPE} property.
     * This is done to allow the filter to be applied globally and match all events.
     *
     * @param filterPreference the reference user
     * @param preference the related notification preference, can be null
     * @return true if the filter should be applied to the given scope.
     */
    protected boolean preferencesMatchesFilteringContext(NotificationFilterPreference filterPreference,
            NotificationPreference preference)
    {
        if (preference == null) {
            return matchAllEvents(filterPreference) && filterPreference.isActive();
        } else {
            return matchEventType(filterPreference, preference);
        }
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

    @Override
    public String getName()
    {
        return filterName;
    }

    @Override
    public AbstractOperatorNode generateFilterExpression(DocumentReference user, NotificationPreference eventTypePref,
            NotificationFilterType filterType)
    {
        AbstractOperatorNode syntaxNode = null;

        try {
            // Get every filterPreference linked to the current filter
            Set<NotificationFilterPreference> notificationFilterPreferences;
            if (eventTypePref != null) {
                notificationFilterPreferences = notificationFilterManager.getFilterPreferences(
                        user, this, filterType, eventTypePref.getFormat());
            } else {
                notificationFilterPreferences = notificationFilterManager.getFilterPreferences(
                        user, this, filterType);
            }

            Iterator<NotificationFilterPreference> iterator
                    = getFilterPreferencesIterator(notificationFilterPreferences);
            while (iterator.hasNext()) {

                T filterPref = convertPreferences(iterator.next());

                if (!preferencesMatchesFilteringContext(filterPref, eventTypePref)) {
                    continue;
                }

                AbstractOperatorNode tmpNode = generateNode(filterPref);

                // If we have an EXCLUSIVE filter, negate the filter node
                if (filterType.equals(NotificationFilterType.EXCLUSIVE)) {
                    tmpNode = not(tmpNode);
                }

                // Wrap the freshly created node in a AndNode or a OrNode depending on the filter type
                if (syntaxNode == null) {
                    syntaxNode = tmpNode;
                } else if (filterType.equals(NotificationFilterType.INCLUSIVE)) {
                    syntaxNode = syntaxNode.or(tmpNode);
                } else {
                    syntaxNode = syntaxNode.and(tmpNode);
                }
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        return syntaxNode;
    }

    /**
     * Given {@link NotificationFilterPreference}, generate the associated restriction.
     *
     * @param filterPreference the preference to use
     * @return the generated node
     */
    protected abstract AbstractOperatorNode generateNode(T filterPreference);

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return preference.getCategory().equals(NotificationPreferenceCategory.DEFAULT)
                && preference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE);
    }

    protected abstract int getDeepLevel(NotificationFilterPreference pref);
}
