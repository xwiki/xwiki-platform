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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;
import org.xwiki.notifications.filters.internal.LocationOperatorNodeGenerator;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

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

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Generate a filter expression for the given user and event type according to the scope notification filter
     * preferences.
     * @param filterPreferences the collection of all preferences
     * @param eventType type of the event on which we are filtering
     * @param format the format of the notification
     * @param user the user for who we are making the query
     * @return the expression node corresponding to the filter
     */
    public AbstractOperatorNode filterExpression(Collection<NotificationFilterPreference> filterPreferences,
            String eventType, NotificationFormat format, DocumentReference user)
    {
        // The node we construct
        AbstractOperatorNode topNode = null;

        // Get the filters to handle
        ScopeNotificationFilterPreferencesHierarchy preferences
                = scopeNotificationFilterPreferencesGetter.getScopeFilterPreferences(filterPreferences,
                    eventType, format, false, false);

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

            // We will handle "page only" filters afterwards (but only for pref that are stored in the database
            // and loaded by the default user profile)
            if (isPageOnly(pref)) {
                continue;
            }

            // For each exclusive filter, we want to generate a query to black list the location with a white list of
            // sub locations.
            // Ex:   "wiki1:Space1" is blacklisted but:
            //     - "wiki1:Space1.Space2" is white listed
            //     - "wiki1:Space1.Space3" is white listed too

            // The filterNode is something like "NOT (event.location = A)".
            AbstractOperatorNode filterNode = generateNode(pref);

            // Children are a list of inclusive filters located under the current one.
            for (ScopeNotificationFilterPreference childFilter : pref.getChildren()) {
                // We will handle "page only" filters afterwards
                if (isPageOnly(childFilter)) {
                    continue;
                }
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
        AbstractOperatorNode topNode = null;

        Iterator<ScopeNotificationFilterPreference> it = preferences.getInclusiveFiltersThatHasNoParents();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            // We will handle "page only" filters afterwards
            if (isPageOnly(pref)) {
                continue;
            }

            if (topNode == null) {
                topNode = generateNode(pref);
            } else {
                topNode = topNode.or(generateNode(pref));
            }
        }

        // At this point, topNode looks like:
        // topNode AND (event.location = D or event.location = E or event.location = F OR...)
        if (node != null && topNode != null) {
            topNode = node.and(topNode);
        } else if (topNode == null && node != null) {
            topNode = node;
        }

        return topNode;
    }

    private boolean isPageOnly(ScopeNotificationFilterPreference pref)
    {
        // We make sure we only handle preferences that come from "userProfile" that are actually saved in the database
        // as NotificationFilterPreferences.
        // For example, a preference that comes from the watchlist bridge is not stored in the database, so we have to
        // handle it without using the subquery mechanism that we can see in
        // filterExpression(Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format,
        //    NotificationFilterType type, DocumentReference user).
        return StringUtils.isNotBlank(pref.getPageOnly())
            && pref.getEventTypes().isEmpty();
    }

    private AbstractOperatorNode generateNode(ScopeNotificationFilterPreference scopeNotificationFilterPreference)
    {
        AbstractOperatorNode filterNode
                = locationOperatorNodeGenerator.generateNode(scopeNotificationFilterPreference.getScopeReference());

        // If we have an EXCLUSIVE filter, negate the filter node
        if (scopeNotificationFilterPreference.getFilterType().equals(NotificationFilterType.EXCLUSIVE)) {
            filterNode = not(filterNode);
        } else if (scopeNotificationFilterPreference.getStartingDate() != null) {
            filterNode = filterNode.and(value(EventProperty.DATE).greaterThan(
                    value(scopeNotificationFilterPreference.getStartingDate())));
        }

        return filterNode;
    }

    /**
     * Global filtering on the query.
     *
     * This method is designed to handle one of the main use case of XWiki notifications that used to scale badly.
     * Because of the auto watch mechanism, users could end-up with hundred of notification filter preferences
     * to watch given pages.
     * The corresponding HQL query used to contains hundred of "OR event.page = 'somePage'" and was so big that
     * Stack Overflows were happening.
     * So for this very problematic use-case, we have decided not to inject a lot of statements in the HQL query,
     * but instead to write a sub query so that the database would load the notification filter preferences itself
     * and do the filtering based on this.
     * To limit the number of sub queries, we also limit this mechanism to filter preferences that concern all event
     * types.
     *
     * NOTE: this work only with user's preferences stocked in the database. It does not support the use-case of
     * the notifications macro, when the preferences are created on the fly based on the macro parameters!
     *
     * @param filterPreferences all filter preferences
     * @param format format of the notifications
     * @param type type of filtering
     * @param user the user for who we compute the notifications
     *
     * @return the expression node to inject in the query
     *
     * @since 10.8RC1
     * @since 9.11.8
     */
    public AbstractOperatorNode filterExpression(Collection<NotificationFilterPreference> filterPreferences,
            NotificationFormat format, NotificationFilterType type, DocumentReference user)
    {
        // Of course, we don't inject this sub query if there is no preference at all that match the criteria.
        if (!filterPreferences.stream().anyMatch(
                isAPageOnlyFilterPreferenceThatConcernAllEvents(format, type))) {
            return null;
        }

        String subQuery = "SELECT nfp.pageOnly FROM DefaultNotificationFilterPreference nfp WHERE nfp.owner = :owner "
                + "AND nfp.filterType = %d AND nfp.filterName = 'scopeNotificationFilter' "
                + "AND nfp.pageOnly IS NOT NULL AND nfp.pageOnly <> '' "
                + "AND (nfp.allEventTypes = '' OR nfp.allEventTypes IS NULL) "
                + "AND nfp.%s = true AND nfp.enabled = true";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("owner", serializer.serialize(user));

        String formatParameter = (format == NotificationFormat.ALERT ? "alertEnabled" : "emailEnabled");

        // We need to concat event.wiki with event.page because event.page contains a local serialized value
        // meanwhile DefaultNotificationFilterPreference stores full reference values
        AbstractValueNode expression = value(EventProperty.WIKI).concat(value(":")).concat(value(EventProperty.PAGE));

        if (type == NotificationFilterType.EXCLUSIVE) {
            String exclusion = String.format(subQuery, NotificationFilterType.EXCLUSIVE.ordinal(), formatParameter);
            return not(expression.inSubQuery(exclusion, parameters));
        } else {
            subQuery += " AND nfp.startingDate <= event.date";
            String inclusion = String.format(subQuery, NotificationFilterType.INCLUSIVE.ordinal(), formatParameter);
            return expression.inSubQuery(inclusion, parameters);
        }
    }

    private Predicate<NotificationFilterPreference> isAPageOnlyFilterPreferenceThatConcernAllEvents(
            NotificationFormat format, NotificationFilterType type)
    {
        return nfp -> isEnabledAndStoredScopeNotificationFilterPreference(nfp)
                && doesFilterTypeAndFormatMatch(nfp, format, type)
                && StringUtils.isNotBlank(nfp.getPageOnly());
    }

    private boolean isEnabledAndStoredScopeNotificationFilterPreference(NotificationFilterPreference nfp)
    {
        // This optimization can only works on preferences stored by the user, that's why we add the final condition
        return nfp.isEnabled() && ScopeNotificationFilter.FILTER_NAME.equals(nfp.getFilterName())
            && StringUtils.startsWith(nfp.getId(), NotificationFilterPreference.DB_ID_FILTER_PREFIX);
    }

    private boolean doesFilterTypeAndFormatMatch(NotificationFilterPreference nfp, NotificationFormat format,
            NotificationFilterType type)
    {
        return nfp.getFilterType() == type && nfp.getNotificationFormats().contains(format);
    }
}
