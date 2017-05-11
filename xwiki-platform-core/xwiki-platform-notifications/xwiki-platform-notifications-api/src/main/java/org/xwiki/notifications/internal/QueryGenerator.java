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
package org.xwiki.notifications.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationPreference;
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
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("cached")
    private ModelBridge modelBridge;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("user")
    private ConfigurationSource userPreferencesSource;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;


    /**
     * Generate the query.
     *
     * @param user user interested in the notifications
     * @param onlyUnread f only unread events should be returned
     * @param endDate do not return events happened after this date
     * @param blackList list of ids of blacklisted events to not return (to not get already known events again)
     * @return the query to execute
     *
     * @throws NotificationException if error happens
     * @throws QueryException if error happens
     */
    public Query generateQuery(DocumentReference user, boolean onlyUnread, Date endDate, List<String> blackList)
            throws NotificationException, QueryException
    {
        // TODO: create a role so extensions can inject their own complex query parts
        // TODO: create unit tests for all use-cases
        // TODO: idea: handle the items of the watchlist too

        // First: get the preferences of the given user
        List<NotificationPreference> preferences = modelBridge.getNotificationsPreferences(user);

        // Then: generate the HQL query
        StringBuilder hql = new StringBuilder();
        hql.append("where event.date >= :startDate AND event.user <> :user AND (");

        List<String> types = handleEventTypes(hql, preferences);
        List<String> apps  = handleApplications(hql, preferences, types);

        // No notification is returned if nothing is saved in the user settings
        // TODO: handle some defaults preferences that can be set in the administration
        if (preferences.isEmpty() || (types.isEmpty() && apps.isEmpty())) {
            return null;
        }

        hql.append(")");

        handleBlackList(blackList, hql);
        handleEndDate(endDate, hql);
        handleHiddenEvents(hql);
        handleEventStatus(onlyUnread, hql);
        handleWiki(user, hql);
        handleOrder(hql);

        // The, generate the query
        Query query = queryManager.createQuery(hql.toString(), Query.HQL);

        // Bind values
        query.bindValue("startDate", modelBridge.getUserStartDate(user));
        query.bindValue("user", serializer.serialize(user));
        handleEventTypes(types, query);
        handleApplications(apps, query);
        handleBlackList(blackList, query);
        handleEndDate(endDate, query);
        handleWiki(user, query);

        // Return the query
        return query;
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

    private void handleApplications(List<String> apps, Query query)
    {
        if (!apps.isEmpty()) {
            query.bindValue("apps", apps);
        }
    }

    private void handleEventTypes(List<String> types, Query query)
    {
        if (!types.isEmpty()) {
            query.bindValue("types", types);
        }
    }

    private List<String> handleApplications(StringBuilder hql, List<NotificationPreference> preferences,
            List<String> types)
    {
        List<String> apps = new ArrayList<>();
        for (NotificationPreference preference : preferences) {
            if (preference.isNotificationEnabled() && StringUtils.isNotBlank(preference.getApplicationId())) {
                apps.add(preference.getApplicationId());
            }
        }
        if (!apps.isEmpty()) {
            hql.append((types.isEmpty() ? "" : " OR ") + "event.application IN (:apps)");
        }
        return apps;
    }

    private List<String> handleEventTypes(StringBuilder hql, List<NotificationPreference> preferences)
    {
        List<String> types = new ArrayList<>();
        for (NotificationPreference preference : preferences) {
            if (preference.isNotificationEnabled() && StringUtils.isNotBlank(preference.getEventType())) {
                types.add(preference.getEventType());
            }
        }
        if (!types.isEmpty()) {
            hql.append("event.type IN (:types)");
        }
        return types;
    }

    private void handleWiki(DocumentReference user, Query query)
    {
        // If the user is a local user
        if (!user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId())) {
            query.bindValue("userWiki", user.getWikiReference().getName());
        }
    }
}
