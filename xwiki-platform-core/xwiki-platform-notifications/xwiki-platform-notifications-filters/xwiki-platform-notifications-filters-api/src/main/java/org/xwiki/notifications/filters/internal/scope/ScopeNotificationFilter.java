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

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Define a notification filter based on a scope in the wiki.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component
@Named(ScopeNotificationFilter.FILTER_NAME)
@Singleton
public class ScopeNotificationFilter implements NotificationFilter
{
    /**
     * Name of the filter.
     */
    public static final String FILTER_NAME = "scopeNotificationFilter";

    @Inject
    private ScopeNotificationFilterLocationStateComputer stateComputer;

    @Inject
    private ScopeNotificationFilterExpressionGenerator expressionGenerator;

    @Inject
    private Logger logger;

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences,
            NotificationFormat format)
    {
        final EntityReference eventEntity = getEventEntity(event);
        FilterPolicy result = FilterPolicy.NO_EFFECT;
        if (eventEntity != null) {

            // We don't check the inclusive filters if the target is specified since it means the notification already
            // targets an user. We still check the exclusive one in case the user would want to avoid spam.
            boolean checkInclusiveFilters = event.getTarget() == null || event.getTarget().isEmpty();

            // Note: the filtering on the date is not handled on the HQL-side because the request used to be too long
            // and used to generate stack overflows. So we won't make it worse by adding a date condition on each
            // different scope preference.
            WatchedLocationState state = stateComputer.isLocationWatched(filterPreferences, eventEntity,
                event.getType(), format, false, checkInclusiveFilters, false);

            switch (state.getState()) {
                case BLOCKED:
                case BLOCKED_BY_ANCESTOR:
                case BLOCKED_WITH_CHILDREN:
                    if (state.getStartingDate().before(event.getDate())) {
                        result = FilterPolicy.FILTER;
                    }
                    break;

                case WATCHED:
                case WATCHED_BY_ANCESTOR:
                case WATCHED_WITH_CHILDREN:
                    if (state.getStartingDate().after(event.getDate())) {
                        result = FilterPolicy.FILTER;
                    }
                    break;

                case CUSTOM:
                    this.logger.error("Filtering of event should never return custom. Event: [{}]. User: [{}] ",
                        event, user);
                    break;

                case NOT_SET:
                default:
                    if (checkInclusiveFilters) {
                        result = FilterPolicy.FILTER;
                    }
            }
        }
        return result;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return preference.getCategory().equals(NotificationPreferenceCategory.DEFAULT)
                && preference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE);
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences,
            NotificationPreference preference)
    {
        return expressionGenerator.filterExpression(filterPreferences,
                (String) preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE),
                preference.getFormat(), user);
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences,
            NotificationFilterType type, NotificationFormat format)
    {
        return filterExpression(user, filterPreferences, type, format, Collections.emptyList());
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilterType type,
            NotificationFormat format, Collection<NotificationPreference> preferences)
    {
        // Generate the node that we may (or not) return afterwards
        AbstractOperatorNode node = expressionGenerator.filterExpression(filterPreferences, format, type, user);
        if (node == null) {
            return null;
        }

        if (type == NotificationFilterType.INCLUSIVE) {
            // In order not to include all watched pages without consideration to the event types, we first collect
            // the enabled even types.
            AbstractOperatorNode enabledEventTypes = getEnabledEventTypes(preferences);
            if (enabledEventTypes != null) {
                return enabledEventTypes.and(node);
            }
        } else {
            return node;
        }

        return null;
    }

    private AbstractOperatorNode getEnabledEventTypes(Collection<NotificationPreference> preferences)
    {
        AbstractOperatorNode topNode = null;

        for (NotificationPreference preference : preferences) {
            Object eventType = preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE);
            if (preference.isNotificationEnabled() && eventType != null && eventType instanceof String) {
                AbstractOperatorNode node = value(EventProperty.TYPE).eq(value((String) eventType))
                        .and(value(EventProperty.DATE).greaterThan(value(preference.getStartDate())));
                if (topNode == null) {
                    topNode = node;
                } else {
                    topNode = topNode.or(node);
                }
            }
        }

        return topNode;
    }

    @Override
    public String getName()
    {
        return FILTER_NAME;
    }

    private EntityReference getEventEntity(Event event)
    {
        if (event.getDocument() != null) {
            return event.getDocument();
        }
        if (event.getSpace() != null) {
            return event.getSpace();
        }
        return event.getWiki();
    }
}
