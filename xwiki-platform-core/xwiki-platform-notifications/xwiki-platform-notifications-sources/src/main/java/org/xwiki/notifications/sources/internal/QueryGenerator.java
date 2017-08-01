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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.xwiki.notifications.filters.NotificationProperty;
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
     * Class used to store the property of an event used in the query.
     *
     * @since 9.7RC1
     */
    private class EventProperty
    {
        private String eventType;

        private Date startDate;

        /**
         * Constructs a new {@link EventProperty}.
         *
         * @param eventType the type of the event
         * @param startDate the start date of the event
         */
        EventProperty(String eventType, Date startDate)
        {
            this.eventType = eventType;
            this.startDate = startDate;
        }
    }

    /**
     * Generate the query.
     *
     * @param user user interested in the notifications
     * @param format only match notifications enabled for that format
     * @param onlyUnread f only unread events should be returned
     * @param endDate do not return events happened after this date
     * @param startDate do not return events happened before this date. Note that since 9.7RC1, this start date is
     * completely optional, {@link NotificationPreference#startDate} should be used for more granular control on
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

        // First: get the preferences of the given user
        List<NotificationPreference> preferences = notificationPreferenceManager.getNotificationsPreferences(user);

        // Then: generate the HQL query
        StringBuilder hql = new StringBuilder();
        hql.append("where event.user <> :user AND ");
        if (startDate != null) {
            hql.append("event.date >= :startDate AND ");
        }
        hql.append(LEFT_PARENTHESIS);

        List<EventProperty> propertyList = handleEventPreferences(user, hql, preferences, format);
        Set<String> eventTypes = new HashSet<>();
        for (EventProperty property : propertyList) {
            eventTypes.add(property.eventType);
        }

        // No notification is returned if nothing is saved in the user settings
        // TODO: handle some defaults preferences that can be set in the administration
        if (preferences.isEmpty() || propertyList.isEmpty()) {
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
        handleEventPreferences(propertyList, query);
        handleBlackList(blackList, query);
        handleEndDate(endDate, query);
        handleWiki(user, query);

        handleFiltersParams(user, query, format, propertyList);

        // Return the query
        return query;
    }

    private void handleFiltersOR(DocumentReference user, StringBuilder hql, NotificationFormat format,
            String type)
            throws NotificationException
    {
        StringBuilder query = new StringBuilder();
        String separator = "";

        for (NotificationFilter filter : notificationFilterManager.getAllNotificationFilters(user)) {
            Map<NotificationProperty, String> properties =
                    Collections.singletonMap(NotificationProperty.EVENT_TYPE, type);
            String filterQuery = filter.queryFilterOR(user, format, properties);
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

    private void handleFiltersAND(DocumentReference user, StringBuilder hql, NotificationFormat format, String type)
            throws NotificationException
    {
        for (NotificationFilter filter : notificationFilterManager.getAllNotificationFilters(user)) {
            Map<NotificationProperty, String> properties =
                    Collections.singletonMap(NotificationProperty.EVENT_TYPE, type);
            String filterQuery = filter.queryFilterAND(user, format, properties);
            if (StringUtils.isNotBlank(filterQuery)) {
                hql.append(" AND ");
                hql.append(filterQuery);
            }
        }
    }

    private void handleFiltersParams(DocumentReference user, Query query, NotificationFormat format,
            List<EventProperty> propertyList) throws NotificationException
    {
        ArrayList<String> eventTypes = new ArrayList<>();
        for (EventProperty property : propertyList) {
            eventTypes.add(property.eventType);
        }
        for (NotificationFilter filter : notificationFilterManager.getAllNotificationFilters(user)) {
            List<Map<NotificationProperty, String>> propertiesList = new ArrayList<>();
            for (String eventType : eventTypes) {
                propertiesList.add(Collections.singletonMap(NotificationProperty.EVENT_TYPE, eventType));
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
     * Bind the event preferences parameters to the query. Those parameters are usually declared in
     * {@link #handleEventPreferences(DocumentReference, StringBuilder, List, NotificationFormat)}..
     *
     * @param propertyList A list of {@link EventProperty}
     * @param query the query
     */
    private void handleEventPreferences(List<EventProperty> propertyList, Query query)
    {
        int number = 0;
        for (EventProperty property : propertyList) {
            query.bindValue(String.format("type_%d", number), property.eventType);
            query.bindValue(String.format("date_%d", number), property.startDate);
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
     * @param format the format of event that we want to retrieve
     * @return a Map containing the event types in keys and their corresponding start dates as values
     * @throws NotificationException if an error occurred
     */
    private List<EventProperty> handleEventPreferences(DocumentReference user, StringBuilder hql,
            List<NotificationPreference> preferences, NotificationFormat format) throws NotificationException
    {
        List<EventProperty> propertyList = new ArrayList<>();
        for (NotificationPreference preference : preferences) {
            if (preference.isNotificationEnabled() && StringUtils.isNotBlank(preference.getEventType())
                    && format.equals(preference.getFormat())) {
                propertyList.add(new EventProperty(preference.getEventType(), preference.getStartDate()));
            }
        }
        if (!propertyList.isEmpty()) {
            hql.append(LEFT_PARENTHESIS);
            String separator = "";
            int number = 0;
            for (EventProperty property : propertyList) {
                hql.append(separator);
                hql.append(String.format("((event.type = :type_%s AND event.date >= :date_%d)", number, number));
                number++;
                handleFiltersOR(user, hql, format, property.eventType);
                handleFiltersAND(user, hql, format, property.eventType);
                hql.append(RIGHT_PARENTHESIS);
                separator = OR;
            }
            hql.append(RIGHT_PARENTHESIS);
        }
        return propertyList;
    }

    private void handleWiki(DocumentReference user, Query query)
    {
        // If the user is a local user
        if (!user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId())) {
            query.bindValue("userWiki", user.getWikiReference().getName());
        }
    }
}
