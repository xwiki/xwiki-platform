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
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.internal.LocationOperatorNodeGenerator;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;

/**
 * Generate an {@link ExpressionNode} to handle Scope Notification Filters for a given pair of user / event type.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component(roles = ScopeNotificationFilterExpressionGenerator.class)
@Singleton
public class ScopeNotificationFilterExpressionGenerator
{
    @Inject
    private ScopeNotificationFilterPreferencesGetter scopeNotificationFilterPreferencesGetter;

    @Inject
    private LocationOperatorNodeGenerator locationOperatorNodeGenerator;

    /**
     * Generate a filter expression for the given user and event type according to the scope notification filter
     * preferences.
     * @param filterPreferences the collection of all preferences
     * @param eventType type of the event on which we are filtering
     * @param format the format of the notification
     * @return the expression node corresponding to the filter
     */
    public AbstractOperatorNode filterExpression(Collection<NotificationFilterPreference> filterPreferences,
            String eventType, NotificationFormat format)
    {
        // The node we construct
        AbstractOperatorNode topNode = null;

        // Get the filters to handle
        ScopeNotificationFilterPreferencesHierarchy preferences
                 = scopeNotificationFilterPreferencesGetter.getScopeFilterPreferences(filterPreferences,
                    eventType, format);

        // The aim is to generate a black list with exceptions (handleExclusiveFilters) and a white
        // list (handleTopLevelInclusiveFilters).
        // It is a complex query, for more information see: https://jira.xwiki.org/browse/XWIKI-14713
        topNode = handleExclusiveFilters(topNode, preferences);
        topNode = handleTopLevelInclusiveFilters(topNode, preferences);

        // At this point, topNode looks like:
        //
        // (NOT (event.location = A) OR (event.location = A.B) OR (event.location = A.C))
        // AND
        // (NOT (event.location = X) OR (event.location = X.Y) OR (event.location = X.Z))
        // OR
        // event.location = D
        // OR
        // event.location = E
        // OR
        // event.location = F
        // etc...

        return topNode;
    }

    private AbstractOperatorNode handleExclusiveFilters(AbstractOperatorNode node,
            ScopeNotificationFilterPreferencesHierarchy preferences)
    {
        AbstractOperatorNode topNode = node;

        Iterator<ScopeNotificationFilterPreference> it = preferences.getExclusiveFiltersThatHasNoParents();

        // Handle exclusive filters
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            // For each exclusive filter, we want to generate a query to black list the location with a white list of
            // sub locations.
            // Ex:   "wiki1:Space1" is blacklisted but:
            //     - "wiki1:Space1.Space2" is white listed
            //     - "wiki1:Space1.Space3" is white listed too

            // The filterNode is something like "NOT (event.location = A)".
            AbstractOperatorNode filterNode = generateNode(pref);

            // Children are a list of inclusive filters located under the current one.
            for (ScopeNotificationFilterPreference childFilter : pref.getChildren()) {
                // child filter is something like "event.location = A.B"
                filterNode = filterNode.or(generateNode(childFilter));
            }

            // At this point, filter node looks like:
            // NOT (event.location = A) OR (event.location = A.B) or (event.location = A.C)

            // Chain this filter to the previous one
            if (topNode == null) {
                topNode = filterNode;
            } else {
                topNode = topNode.and(filterNode);
            }
        }

        // At this point, topNode looks like:
        // (NOT (event.location = A) OR (event.location = A.B) OR (event.location = A.C))
        // AND
        // (NOT (event.location = X) OR (event.location = X.Y) OR (event.location = X.Z))
        // AND ...

        return topNode;
    }

    private AbstractOperatorNode handleTopLevelInclusiveFilters(AbstractOperatorNode node,
            ScopeNotificationFilterPreferencesHierarchy preferences)
    {
        AbstractOperatorNode topNode = node;

        Iterator<ScopeNotificationFilterPreference> it = preferences.getInclusiveFiltersThatHasNoParents();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            if (topNode == null) {
                topNode = generateNode(pref);
            } else {
                topNode = topNode.or(generateNode(pref));
            }
        }

        // At this point, topNode looks like:
        // topNode OR event.location = D or event.location = E or event.location = F OR...

        return topNode;
    }

    private AbstractOperatorNode generateNode(ScopeNotificationFilterPreference scopeNotificationFilterPreference)
    {
        AbstractOperatorNode filterNode
                = locationOperatorNodeGenerator.generateNode(scopeNotificationFilterPreference.getScopeReference());

        // If we have an EXCLUSIVE filter, negate the filter node
        if (scopeNotificationFilterPreference.getFilterType().equals(NotificationFilterType.EXCLUSIVE)) {
            filterNode = not(filterNode);
        }

        return filterNode;
    }
}
