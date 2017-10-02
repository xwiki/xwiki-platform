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

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

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
    private ScopeNotificationFilterPreferencesGetter preferencesGetter;

    @Inject
    private ScopeNotificationFilterExpressionGenerator expressionGenerator;

    @Override
    public boolean filterEvent(Event event, DocumentReference user, NotificationFormat format)
    {
        ScopeNotificationFilterPreferencesHierarchy preferences
                = preferencesGetter.getScopeFilterPreferences(user, event.getType());

        if (preferences.isEmpty()) {
            // We won't filter anything if we have no filter preference
            return false;
        }

        final EntityReference eventEntity = getEventEntity(event);
        if (eventEntity == null) {
            // We don't handle events that are not related to a particular location
            return false;
        }

        Iterator<ScopeNotificationFilterPreference> it = preferences.getExclusiveFiltersThatHasNoParents();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            // If the exclusive filter match the event location...
            if (eventEntity.equals(pref.getScopeReference()) || eventEntity.hasParent(pref.getScopeReference())) {

                // then we dismiss the current event if there is no inclusive filter child matching the event
                return !pref.getChildren().stream().anyMatch(
                    child -> eventEntity.equals(child.getScopeReference())
                        || eventEntity.hasParent(child.getScopeReference())
                );
            }
        }

        it = preferences.getInclusiveFiltersThatHasNoParents();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            // If the inclusive filter match the event location...
            if (eventEntity.equals(pref.getScopeReference()) || eventEntity.hasParent(pref.getScopeReference())) {
                // Then we don't dismiss the event
                return false;
            }
        }

        // If we are here, we have filter preferences but no one is matching the current event location,
        // so we dismiss this event
        return true;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return preference.getCategory().equals(NotificationPreferenceCategory.DEFAULT)
                && preference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE);
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user, NotificationPreference preference)
    {
        return expressionGenerator.filterExpression(user,
                (String) preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE));
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user, NotificationFilterType type)
    {
        // We don't handle this use-case anymore
        return null;
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
