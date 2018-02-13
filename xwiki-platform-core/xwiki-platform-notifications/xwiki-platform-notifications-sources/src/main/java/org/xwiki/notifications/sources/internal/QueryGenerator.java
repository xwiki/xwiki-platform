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
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.BooleanValueNode;
import org.xwiki.notifications.filters.expression.DateValueNode;
import org.xwiki.notifications.filters.expression.EntityReferenceNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.GreaterThanNode;
import org.xwiki.notifications.filters.expression.InNode;
import org.xwiki.notifications.filters.expression.LesserThanNode;
import org.xwiki.notifications.filters.expression.NotEqualsNode;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

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
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private ExpressionNodeToHQLConverter hqlConverter;

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
     * @param endDate do not return events happened after this date
     * @param startDate do not return events happened before this date. Note that since 9.7RC1, this start date is
     * completely optional, {@link NotificationPreference#getStartDate()} should be used for more granular control on
     * notifications
     * @param blackList list of ids of blacklisted events to not return (to not get already known events again)
     * @return the query to execute
     * @throws NotificationException if error happens
     * @throws QueryException if error happens
     */
    public Query generateQuery(DocumentReference user, NotificationFormat format, Date endDate, Date startDate,
            List<String> blackList) throws NotificationException, QueryException
    {
        ExpressionNodeToHQLConverter.HQLQuery result = hqlConverter.parse(
                generateQueryExpression(user, format, endDate, startDate, blackList)
        );
        if (result.getQuery().isEmpty()) {
            return null;
        }

        Query query = queryManager.createQuery(String.format("where %s", result.getQuery()), Query.HQL);
        for (Map.Entry<String, Object> queryParameter : result.getQueryParameters().entrySet()) {
            query.bindValue(queryParameter.getKey(), queryParameter.getValue());
        }

        return query;
    }

    /**
     * Generate the query.
     *
     * @param user user interested in the notifications
     * @param format only match notifications enabled for that format
     * @param endDate do not return events happened after this date
     * @param startDate do not return events happened before this date. Note that since 9.7RC1, this start date is
     * completely optional, {@link NotificationPreference#getStartDate()} should be used for more granular control on
     * notifications
     * @param blackList list of ids of blacklisted events to not return (to not get already known events again)
     * @return the query to execute
     *
     * @throws NotificationException if error happens
     * @throws QueryException if error happens
     *
     * @since 9.8RC1
     */
    public ExpressionNode generateQueryExpression(DocumentReference user, NotificationFormat format, Date endDate,
            Date startDate, List<String> blackList) throws NotificationException, QueryException
    {
        // First: get the active preferences of the given user
        List<NotificationPreference> preferences = notificationPreferenceManager.getPreferences(user, true,
                format);

        // Ensure that we have at least one filter preference that is active
        if (preferences.isEmpty()
            && notificationFilterManager.getFilterPreferences(user).stream().noneMatch(
                NotificationFilterPreference::isActive)) {
            return null;
        }

        AbstractOperatorNode topNode = null;

        // Condition 1: (maybe) events have happened after the given start date
        if (startDate != null) {
            topNode = new GreaterThanNode(
                            new PropertyValueNode(EventProperty.DATE),
                            new DateValueNode(startDate)
            );
        }

        // Condition 2: handle other preferences
        AbstractOperatorNode preferencesNode = handleEventPreferences(user, preferences);

        // Condition 3: handle exclusive global notification filters
        AbstractOperatorNode globalExclusiveFiltersNode = handleExclusiveGlobalFilters(user, format);
        if (globalExclusiveFiltersNode != null) {
            if (preferencesNode == null) {
                preferencesNode = globalExclusiveFiltersNode;
            } else {
                preferencesNode = preferencesNode.and(globalExclusiveFiltersNode);
            }
        }

        // Condition 4: handle inclusive global notification filters
        AbstractOperatorNode globalInclusiveFiltersNode = handleInclusiveGlobalFilters(user, format);
        if (globalInclusiveFiltersNode != null) {
            if (preferencesNode == null) {
                preferencesNode = globalInclusiveFiltersNode;
            } else {
                preferencesNode = preferencesNode.or(globalInclusiveFiltersNode);
            }
        }

        // Mix all these conditions
        if (preferencesNode != null) {
            if (topNode != null) {
                topNode = topNode.and(preferencesNode);
            } else  {
                topNode = preferencesNode;
            }
        }

        // Other basic filters
        topNode = handleBlackList(blackList, topNode);
        topNode = handleEndDate(endDate, topNode);
        topNode = handleHiddenEvents(topNode);
        topNode = handleWiki(user, topNode);
        topNode = handleOrder(topNode);

        return topNode;
    }

    /**
     * For each notification preference of the given user, add a constraint on the events to
     * - have one of the notification types that have been subscribed by the user;
     * - have a date superior to the start date corresponding to this type;
     * - match the custom defined user filters.
     *
     * @param user the current user
     * @param preferences a list of the user preferences
     * @return a list of maps that contains query parameters
     * @throws NotificationException if an error occurred
     */
    private AbstractOperatorNode handleEventPreferences(DocumentReference user,
            List<NotificationPreference> preferences) throws NotificationException
    {
        AbstractOperatorNode preferencesNode = null;

        // Filter the notification preferences that are not bound to a specific EVENT_TYPE
        Iterator<NotificationPreference> it = preferences.stream()
                .filter(pref -> pref.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)).iterator();

        while (it.hasNext()) {
            NotificationPreference preference = it.next();

            AbstractOperatorNode preferenceTypeNode = new AndNode(
                    new EqualsNode(
                            value(EventProperty.TYPE),
                            value((String) preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE))
                    ),
                    new GreaterThanNode(
                            value(EventProperty.DATE),
                            value(preference.getStartDate())
                    )
            );

            // Get the notification filters that can be applied to the current preference
            Collection<NotificationFilter> filters = notificationFilterManager.getFilters(user, preference);
            for (NotificationFilter filter : filters) {
                ExpressionNode node = filter.filterExpression(user, preference);
                if (node != null && node instanceof AbstractOperatorNode) {
                    preferenceTypeNode = preferenceTypeNode.and(
                            (AbstractOperatorNode) node
                    );
                }
            }

            if (preferencesNode == null) {
                preferencesNode = preferenceTypeNode;
            } else {
                preferencesNode = preferencesNode.or(
                        preferenceTypeNode
                );
            }
        }

        return preferencesNode;
    }

    /**
     * Generate a part of the query using each of the {@link NotificationFilter} retrieved from the
     * {@link NotificationFilterManager}. Each {@link NotificationFilter} is called without any associated
     * {@link NotificationPreference}.
     *
     * @param user the user used to retrieve the {@link NotificationFilter}
     * @param format format of the notification
     * @return a list of maps of parameters that should be used for the query
     * @throws NotificationException
     */
    private AbstractOperatorNode handleExclusiveGlobalFilters(DocumentReference user, NotificationFormat format)
            throws NotificationException
    {
        AbstractOperatorNode globalFiltersNode = null;

        for (NotificationFilter filter : notificationFilterManager.getAllFilters(user)) {
            ExpressionNode node = filter.filterExpression(user, NotificationFilterType.EXCLUSIVE, format);
            if (node != null && node instanceof AbstractOperatorNode) {
                if (globalFiltersNode == null) {
                    globalFiltersNode = (AbstractOperatorNode) node;
                } else {
                    globalFiltersNode = globalFiltersNode.and(
                            (AbstractOperatorNode) node
                    );
                }
            }
        }

        return globalFiltersNode;
    }

    private AbstractOperatorNode handleInclusiveGlobalFilters(DocumentReference user, NotificationFormat format)
            throws NotificationException
    {
        AbstractOperatorNode globalFiltersNode = null;

        for (NotificationFilter filter : notificationFilterManager.getAllFilters(user)) {
            ExpressionNode node = filter.filterExpression(user, NotificationFilterType.INCLUSIVE, format);
            if (node != null && node instanceof AbstractOperatorNode) {
                if (globalFiltersNode == null) {
                    globalFiltersNode = (AbstractOperatorNode) node;
                } else {
                    globalFiltersNode = globalFiltersNode.or(
                            (AbstractOperatorNode) node
                    );
                }
            }
        }

        return globalFiltersNode;
    }

    private AbstractOperatorNode handleEndDate(Date endDate, AbstractOperatorNode topNode)
    {
        if (endDate != null) {
            return topNode.and(
                    new LesserThanNode(
                        new PropertyValueNode(EventProperty.DATE),
                        new DateValueNode(endDate)
                    )
            );
        }
        return topNode;
    }

    private AbstractOperatorNode handleBlackList(List<String> blackList, AbstractOperatorNode topNode)
    {
        if (blackList != null && !blackList.isEmpty()) {
            Collection<AbstractValueNode> values = new ArrayList<>();
            for (String value : blackList) {
                values.add(new StringValueNode(value));
            }

            return topNode.and(
                    new NotNode(
                            new InNode(
                                    new PropertyValueNode(EventProperty.ID),
                                    values
                            )
                    )
            );
        }
        return topNode;
    }

    private AbstractOperatorNode handleWiki(DocumentReference user, AbstractOperatorNode topNode)
    {
        // If the user is a local user
        if (!user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId())) {
            return topNode.and(
                    new EqualsNode(
                            new PropertyValueNode(EventProperty.WIKI),
                            new EntityReferenceNode(user.getWikiReference())
                    )
            );
        }
        return topNode;
    }

    private AbstractOperatorNode handleOrder(AbstractOperatorNode topNode)
    {
        return new OrderByNode(
                topNode,
                new PropertyValueNode(EventProperty.DATE),
                OrderByNode.Order.DESC
        );
    }

    private AbstractOperatorNode handleHiddenEvents(AbstractOperatorNode topNode)
    {
        // Don't show hidden events unless the user want to display hidden pages
        if (userPreferencesSource.getProperty("displayHiddenDocuments", 0) == 0) {
            return topNode.and(
                    new NotEqualsNode(
                            new PropertyValueNode(EventProperty.HIDDEN),
                            new BooleanValueNode(true)
                    )
            );
        }
        return topNode;
    }
}
