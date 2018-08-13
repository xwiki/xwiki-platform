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
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
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
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.sources.NotificationParameters;
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
    private static final LocalDocumentReference USER_CLASS
            = new LocalDocumentReference("XWiki", "XWikiUsers");

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ExpressionNodeToHQLConverter hqlConverter;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private RecordableEventDescriptorHelper recordableEventDescriptorHelper;

    /**
     * Generate the query.
     *
     * @param parameters parameters to use
     * @return the query to execute
     * @throws QueryException if error happens
     * @throws EventStreamException if error happens
     */
    public Query generateQuery(NotificationParameters parameters) throws QueryException, EventStreamException
    {
        ExpressionNodeToHQLConverter.HQLQuery result = hqlConverter.parse(generateQueryExpression(parameters));
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
     * @param parameters parameters to use
     * @return the query to execute
     *
     * @since 9.8RC12x
     */
    public ExpressionNode generateQueryExpression(NotificationParameters parameters) throws EventStreamException
    {
        // First: get the active preferences of the given user
        Collection<NotificationPreference> preferences = parameters.preferences;

        // Ensure that we have at least one filter preference that is active
        if (preferences.isEmpty() && parameters.filterPreferences.stream().noneMatch(
                NotificationFilterPreference::isActive)) {
            return null;
        }

        AbstractOperatorNode topNode = null;

        // Condition 1: (maybe) events have happened after the given start date
        if (parameters.fromDate != null) {
            topNode = new GreaterThanNode(
                            new PropertyValueNode(EventProperty.DATE),
                            new DateValueNode(parameters.fromDate)
            );
        }

        // Condition 2: handle other preferences
        AbstractOperatorNode preferencesNode = handleEventPreferences(parameters);

        // Condition 3: handle inclusive global notification filters
        AbstractOperatorNode globalInclusiveFiltersNode = handleInclusiveGlobalFilters(parameters);
        if (globalInclusiveFiltersNode != null) {
            if (preferencesNode == null) {
                preferencesNode = globalInclusiveFiltersNode;
            } else {
                preferencesNode = preferencesNode.or(globalInclusiveFiltersNode);
            }
        }

        // Condition 4: handle exclusive global notification filters (only if some preferences were enabled)
        if (preferencesNode != null) {
            AbstractOperatorNode globalExclusiveFiltersNode = handleExclusiveGlobalFilters(parameters);
            if (globalExclusiveFiltersNode != null) {
                preferencesNode = preferencesNode.and(globalExclusiveFiltersNode);
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
        topNode = handleBlackList(parameters, topNode);
        topNode = handleEndDate(parameters, topNode);
        topNode = handleHiddenEvents(parameters, topNode);
        topNode = handleWiki(parameters, topNode);
        topNode = handleOrder(topNode);

        return topNode;
    }

    /**
     * For each notification preference of the given user, add a constraint on the events to
     * - have one of the notification types that have been subscribed by the user;
     * - have a date superior to the start date corresponding to this type;
     * - match the custom defined user filters.
     *
     * @param parameters parameters
     * @return a list of maps that contains query parameters
     */
    private AbstractOperatorNode handleEventPreferences(NotificationParameters parameters) throws EventStreamException
    {
        AbstractOperatorNode preferencesNode = null;

        // Filter the notification preferences that are not bound to a specific EVENT_TYPE
        Iterator<NotificationPreference> it = parameters.preferences.stream()
                .filter(pref -> pref.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)).iterator();

        while (it.hasNext()) {
            NotificationPreference preference = it.next();

            if (!recordableEventDescriptorHelper.hasDescriptor(
                    (String) preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE),
                    parameters.user
            )) {
                continue;
            }

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
            Iterator<NotificationFilter> filterIterator
                    = notificationFilterManager.getFiltersRelatedToNotificationPreference(parameters.filters,
                    preference).iterator();
            while (filterIterator.hasNext()) {
                NotificationFilter filter = filterIterator.next();
                ExpressionNode node = filter.filterExpression(parameters.user, parameters.filterPreferences,
                        preference);
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
     * @param parameters parameters
     * @return a list of maps of parameters that should be used for the query
     */
    private AbstractOperatorNode handleExclusiveGlobalFilters(NotificationParameters parameters)
    {
        AbstractOperatorNode globalFiltersNode = null;

        for (NotificationFilter filter : parameters.filters) {
            ExpressionNode node = filter.filterExpression(parameters.user, parameters.filterPreferences,
                    NotificationFilterType.EXCLUSIVE, parameters.format);
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

    private AbstractOperatorNode handleInclusiveGlobalFilters(NotificationParameters parameters)
    {
        AbstractOperatorNode globalFiltersNode = null;

        for (NotificationFilter filter : parameters.filters) {
            ExpressionNode node = filter.filterExpression(parameters.user, parameters.filterPreferences,
                    NotificationFilterType.INCLUSIVE, parameters.format, parameters.preferences);
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

    private AbstractOperatorNode handleEndDate(NotificationParameters parameters, AbstractOperatorNode topNode)
    {
        if (parameters.endDate != null) {
            return topNode.and(
                    new LesserThanNode(
                        new PropertyValueNode(EventProperty.DATE),
                        new DateValueNode(parameters.endDate)
                    )
            );
        }
        return topNode;
    }

    private AbstractOperatorNode handleBlackList(NotificationParameters parameters, AbstractOperatorNode topNode)
    {
        if (parameters.blackList != null && !parameters.blackList.isEmpty()) {
            Collection<AbstractValueNode> values = new ArrayList<>();
            for (String value : parameters.blackList) {
                values.add(new StringValueNode(value));
            }

            AbstractOperatorNode node = new NotNode(
                    new InNode(
                            new PropertyValueNode(EventProperty.ID),
                            values
                    )
            );

            if (topNode != null) {
                return topNode.and(node);
            } else {
                return node;
            }
        }
        return topNode;
    }

    private AbstractOperatorNode handleWiki(NotificationParameters parameters, AbstractOperatorNode topNode)
    {
        // If the user is a local user
        if (parameters.user != null
                && !parameters.user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId())) {

            AbstractOperatorNode node = new EqualsNode(
                    new PropertyValueNode(EventProperty.WIKI),
                    new EntityReferenceNode(parameters.user.getWikiReference())
            );

            if (topNode != null) {
                return topNode.and(node);
            } else {
                return node;
            }
        }
        return topNode;
    }

    private AbstractOperatorNode handleOrder(AbstractOperatorNode topNode)
    {
        if (topNode != null) {
            return new OrderByNode(
                    topNode,
                    new PropertyValueNode(EventProperty.DATE),
                    OrderByNode.Order.DESC
            );
        } else {
            return null;
        }
    }

    private AbstractOperatorNode handleHiddenEvents(NotificationParameters parameters, AbstractOperatorNode topNode)
    {
        if (parameters.user == null) {
            return excludeHiddenEvents(topNode);
        }

        final DocumentReference userClass = new DocumentReference(USER_CLASS, parameters.user.getWikiReference());
        Object displayHiddenDocuments
                = documentAccessBridge.getProperty(parameters.user, userClass, "displayHiddenDocuments");

        // Don't show hidden events unless the user want to display hidden pages
        if (displayHiddenDocuments == null || Integer.valueOf(0).equals(displayHiddenDocuments)) {
            return excludeHiddenEvents(topNode);
        }

        return topNode;
    }

    private AbstractOperatorNode excludeHiddenEvents(AbstractOperatorNode topNode)
    {
        AbstractOperatorNode node = new NotEqualsNode(
                new PropertyValueNode(EventProperty.HIDDEN),
                new BooleanValueNode(true)
        );

        if (topNode != null) {
            return topNode.and(node);
        } else {
            return node;
        }
    }
}
