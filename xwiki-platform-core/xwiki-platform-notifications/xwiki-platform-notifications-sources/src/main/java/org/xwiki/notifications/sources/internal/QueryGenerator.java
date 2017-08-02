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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.NotificationProperty;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.text.StringUtils;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Generate a query to retrieve notifications events according to the preferences of the user.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Component(roles = QueryGenerator.class)
@Singleton
public class QueryGenerator
{
    private static final String OR = " OR ";

    private static final String LEFT_PARENTHESIS = "(";

    private static final String RIGHT_PARENTHESIS = ")";

    @Inject
    private QueryManager queryManager;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("user")
    private ConfigurationSource userPreferencesSource;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    /**
     * Generate the query.
     *
     * @param user user interested in the notifications
     * @param format only match notifications enabled for that format
     * @param onlyUnread if only unread events should be returned
     * @param endDate do not return events happened after this date
     * @param startDate do not return events happened before this date. Note that since 9.7RC1, this start date is
     * completely optional, {@link NotificationPreference#getStartDate()} should be used for more granular control on
     * notifications
     * @param blackList list of ids of blacklisted events to not return (to not get already known events again)
     * @return the query to execute
     *
     * @throws NotificationException if error happens
     * @throws QueryException if error happens
     */
    public Query generateQuery(DocumentReference user, NotificationFormat format, boolean onlyUnread, Date endDate,
            Date startDate, List<String> blackList) throws NotificationException, QueryException
    {
        // TODO: create a role so extensions can inject their own complex query parts
        // TODO: create unit tests for all use-cases
        // TODO: idea: handle the items of the watchlist too

        // First: get the active preferences of the given user
        List<NotificationPreference> preferences = notificationPreferenceManager.getNotificationsPreferences(
                user, true, format);

        // Then: generate the HQL query
        StringBuilder hql = new StringBuilder();
        hql.append("where event.user <> :user AND ");
        if (startDate != null) {
            hql.append("event.date >= :startDate AND ");
        }
        hql.append(LEFT_PARENTHESIS);

        handleEventPreferences(user, hql, preferences);

        // No notification is returned if nothing is saved in the user settings
        // TODO: handle some defaults preferences that can be set in the administration
        if (preferences.isEmpty() || preferences.isEmpty()) {
            return null;
        }

        hql.append(RIGHT_PARENTHESIS);

        handleBlackList(blackList, hql);
        handleEndDate(endDate, hql);
        handleHiddenEvents(hql);
        handleEventStatus(onlyUnread, hql);
        handleWiki(user, hql);
        handleOrder(hql);

        // The, generate the query
        Query query = queryManager.createQuery(hql.toString(), Query.HQL);

        // Bind values
        if (startDate != null) {
            query.bindValue("startDate", startDate);
        }
        query.bindValue("user", serializer.serialize(user));
        handleEventPreferences(preferences, query);
        handleBlackList(blackList, query);
        handleEndDate(endDate, query);
        handleWiki(user, query);

        handleFiltersParams(user, query, format, preferences);

        // Return the query
        return query;
    }

    private void handleFiltersOR(DocumentReference user, StringBuilder hql, NotificationPreference preference)
            throws NotificationException
    {
        StringBuilder query = new StringBuilder();
        String separator = "";

        for (NotificationFilter filter : notificationFilterManager.getAllNotificationFilters(user)) {
            String filterQuery = filter.queryFilterOR(user, preference.getFormat(), preference.getProperties());

            if (StringUtils.isNotBlank(filterQuery)) {
                query.append(separator);
                query.append(filterQuery);
                separator = OR;
            }
        }

        if (StringUtils.isNotBlank(query.toString())) {
            hql.append(String.format(" AND (%s)", query.toString()));
        }
    }

    private void handleFiltersAND(DocumentReference user, StringBuilder hql, NotificationPreference preference)
            throws NotificationException
    {
        for (NotificationFilter filter : notificationFilterManager.getAllNotificationFilters(user)) {
            String filterQuery = filter.queryFilterAND(user, preference.getFormat(), preference.getProperties());

            if (StringUtils.isNotBlank(filterQuery)) {
                hql.append(" AND ");
                hql.append(filterQuery);
            }
        }
    }

    private void handleFiltersParams(DocumentReference user, Query query, NotificationFormat format,
            List<NotificationPreference> preferences) throws NotificationException
    {
        for (NotificationFilter filter : notificationFilterManager.getAllNotificationFilters(user)) {

            List<Map<NotificationProperty, Object>> propertiesList = new ArrayList<>();
            for (NotificationPreference preference : preferences) {
                propertiesList.add(preference.getProperties());
            }

            Map<String, Object> params = filter.queryFilterParams(user, format, propertiesList);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.bindValue(entry.getKey(), entry.getValue());
            }
        }
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

    private void handleWiki(DocumentReference user, StringBuilder hql)
    {
        // If the user is a local user
        if (!user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId())) {
            hql.append(" AND event.wiki = :userWiki");
        }
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

    /**
     * Bind the notification preferences parameters to the query. Those parameters are usually declared in
     * {@link #handleEventPreferences(DocumentReference, StringBuilder, List)}..
     *
     * @param preferences A list of {@link NotificationPreference}
     * @param query the query
     */
    private void handleEventPreferences(List<NotificationPreference> preferences, Query query)
    {
        int number = 0;
        for (NotificationPreference preference : preferences) {
            if (preference.getProperties().containsKey(NotificationProperty.APPLICATION_ID)) {
                query.bindValue(String.format("application_%d", number),
                        preference.getProperties().get(NotificationProperty.APPLICATION_ID));
            } else if (preference.getProperties().containsKey(NotificationProperty.EVENT_TYPE)) {
                query.bindValue(String.format("type_%d", number),
                        preference.getProperties().get(NotificationProperty.EVENT_TYPE));
            }
            query.bindValue(String.format("date_%d", number), preference.getStartDate());
            number++;
        }
    }

    /**
     * For each notification preference of the given user, add a constraint on the events to
     * - have one of the notification types that have been subscribed by the user;
     * - have a date superior to the start date corresponding to this type;
     * - match the custom defined user filters.
     *
     * @param user the current user
     * @param hql the query
     * @param preferences a list of the user preferences
     * @return a Map containing the event types in keys and their corresponding start dates as values
     * @throws NotificationException if an error occurred
     */
    private void handleEventPreferences(DocumentReference user, StringBuilder hql,
            List<NotificationPreference> preferences) throws NotificationException
    {
        // Filter the notification preferences that are not bound to a specific EVENT_TYPE
        // or APPLICATION_ID as those are the only parameters supported in the queries
        Iterator<NotificationPreference> it = preferences.iterator();
        while (it.hasNext()) {
            NotificationPreference preference = it.next();

            if (!preference.getProperties().containsKey(NotificationProperty.EVENT_TYPE)
                && !preference.getProperties().containsKey(NotificationProperty.APPLICATION_ID)) {
                preferences.remove(preference);
            }
        }

        if (!preferences.isEmpty()) {
            hql.append(LEFT_PARENTHESIS);
            String separator = "";
            int number = 0;
            for (NotificationPreference preference : preferences) {
                hql.append(separator);

                if (preference.getProperties().containsKey(NotificationProperty.APPLICATION_ID)) {
                    hql.append(String.format("((event.application = :application_%s", number));
                } else if (preference.getProperties().containsKey(NotificationProperty.EVENT_TYPE)) {
                    hql.append(String.format("((event.type = :type_%s", number));
                }

                hql.append(String.format(" AND event.date >= :date_%d)", number));

                number++;
                handleFiltersOR(user, hql, preference);
                handleFiltersAND(user, hql, preference);
                hql.append(RIGHT_PARENTHESIS);
                separator = OR;
            }
            hql.append(RIGHT_PARENTHESIS);
        }
    }

    private void handleWiki(DocumentReference user, Query query)
    {
        // If the user is a local user
        if (!user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId())) {
            query.bindValue("userWiki", user.getWikiReference().getName());
        }
    }
}
