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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.eventstream.Event;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationProperty;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.NotificationFormat;

/**
 * Abstract class that helps defining notification filters based on a scope in the wiki.
 * The classes extending this abstract can then choose to filter either based on the eventType of the event,
 * or the applicationId of the event, for example.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public abstract class AbstractScopeNotificationFilter implements NotificationFilter
{
    private static final String ERROR = "Failed to filter the notifications.";

    @Inject
    @Named("cached")
    protected ModelBridge modelBridge;

    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> serializer;

    @Inject
    protected Logger logger;

    /**
     * Generate a custom suffix for a query parameter. This suffix can use the passed parameters as a base for
     * its generation and should be unique to every filter extending {@link AbstractScopeNotificationFilter}.
     *
     * @param filterType the type of filter we’re dealing with
     * @param parameterNumber the number of the current parameter in the query
     * @return a query parameter suffix that should be unique
     */
    protected abstract String generateParameterSuffix(NotificationPreferenceScopeFilterType filterType,
            int parameterNumber);

    /**
     * As other filters can be defined out of {@link AbstractScopeNotificationFilter}, they will have to add their own
     * conditions to the query that is made for the filter. This method allows a child class to define extra conditions
     * on an event in a filter query.
     *
     * @param suffix a suffix that can be used to identify parameters used in the query
     * @return a restriction on the filter query
     */
    protected abstract String generateQueryRestriction(String suffix);

    /**
     * From the restriction created in {@link #generateQueryRestriction(String)}, create a map of the corresponding
     * query parameters with the given suffix.
     *
     * @param suffix the suffix that should be used in the parameters name
     * @return a map of the query parameters to use
     */
    protected abstract Map<String, Object> generateQueryRestrictionParams(String suffix);

    /**
     * Given a {@link NotificationPreferenceFilterScope} and the current filtering context (defined by a
     * {@link NotificationFormat} and a map of {@link NotificationProperty}, determine if a the current filter should
     * apply with the given scope.
     *
     * @param scope the reference scope
     * @param format the format of the notification to filter
     * @param properties a map of properties describing the notification to filter
     * @return true if the filter should be applied to the given scope.
     */
    protected abstract boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope,
            NotificationFormat format, Map<NotificationProperty, String> properties);

    /**
     * Based on the same principle as {@link AbstractScopeNotificationFilter#scopeMatchesFilteringContext(
     * NotificationPreferenceFilterScope, NotificationFormat, Map)}.
     *
     * @param scope the reference scope
     * @param format the format of the notification to filter
     * @param propertiesList a list of the properties to filter
     * @return true if this scope can be applied to at least one of the given properties
     */
    protected abstract boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope,
            NotificationFormat format, List<Map<NotificationProperty, String>> propertiesList);

    /**
     * Given a {@link NotificationPreferenceFilterScope} and the current filtering context (defined by a
     * {@link NotificationFormat} and an {@link Event}, determine if a the current filter should
     * apply with the given scope.
     *
     * @param scope the reference scope
     * @param format the format of the notification to filter
     * @param event the {@link Event} to use
     * @return true if the filter should be applied to the given scope.
     */
    protected abstract boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope,
            NotificationFormat format, Event event);

    @Override
    public boolean filterEvent(Event event, DocumentReference user, NotificationFormat format)
    {
        return this.filterEventByFilterType(event, user, format, NotificationPreferenceScopeFilterType.EXCLUSIVE)
                || this.filterEventByFilterType(event, user, format, NotificationPreferenceScopeFilterType.INCLUSIVE);
    }

    @Override
    public String queryFilterOR(DocumentReference user, NotificationFormat format,
            Map<NotificationProperty, String> properties)
    {
        return this.generateQueryString(user, format, properties,
                NotificationPreferenceScopeFilterType.INCLUSIVE);
    }

    @Override
    public String queryFilterAND(DocumentReference user, NotificationFormat format,
            Map<NotificationProperty, String> properties)
    {
        return this.generateQueryString(user, format, properties,
                NotificationPreferenceScopeFilterType.EXCLUSIVE);
    }

    @Override
    public Map<String, Object> queryFilterParams(DocumentReference user, NotificationFormat format,
            List<Map<NotificationProperty, String>> propertiesList)
    {
        Map<String, Object> params =
                this.generateQueryFilterParams(user, format, propertiesList,
                        NotificationPreferenceScopeFilterType.INCLUSIVE);
        params.putAll(this.generateQueryFilterParams(user, format, propertiesList,
                NotificationPreferenceScopeFilterType.EXCLUSIVE));

        return params;
    }

    /**
     * Just as {@link #filterEvent(Event, DocumentReference, NotificationFormat)}, use the given user, the event, the
     * format of the wanted notification and the type of filter we want to apply (see
     * {@link NotificationPreferenceScopeFilterType}.
     *
     * @since 9.7RC1
     */
    private boolean filterEventByFilterType(Event event, DocumentReference user, NotificationFormat format,
            NotificationPreferenceScopeFilterType scopeFilterType)
    {
        // Indicate if a restriction exist concerning this type of event
        boolean hasRestriction = false;
        // Indicate if a restriction matches the document of the event
        boolean matchRestriction = false;

        try {
            for (NotificationPreferenceFilterScope scope : modelBridge.getNotificationPreferenceScopes(user, format,
                    scopeFilterType)) {
                if (scopeMatchesFilteringContext(scope, format, event)) {
                    hasRestriction = true;

                    if (event.getDocument().equals(scope.getScopeReference())
                            || event.getDocument().hasParent(scope.getScopeReference())) {

                        // If we have a match on an EXCLUSIVE filter, we don’t need to go any further
                        if (scopeFilterType.equals(NotificationPreferenceScopeFilterType.EXCLUSIVE)) {
                            return true;
                        }

                        matchRestriction = true;
                        break;
                    }
                }
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        /**
         * In case we have an INCLUSIVE filter, we check if we had a restriction that was not satisfied.
         * In the case of an EXCLUSIVE filter, if a restriction has been found, then the function should have already
         * returned true.
         */
        return (scopeFilterType.equals(NotificationPreferenceScopeFilterType.INCLUSIVE)
                && hasRestriction && !matchRestriction);
    }

    /**
     * Generate parts of a query used to retrieve events from a given user.
     * Depending on the {@link NotificationPreferenceScopeFilterType} given, the generated query will have a different
     * content.
     *
     * Generated syntax for INCLUSIVE filters:
     * (--filter1--) OR (--filter2--) OR (--filter3--) ...
     *
     * Generated syntax for EXCLUSIVE filters:
     * NOT (--filter1--) AND NOT (--filter2--) AND NOT (--filter3--) ...
     *
     * @since 9.7RC1
     */
    private String generateQueryString(DocumentReference user, NotificationFormat format,
            Map<NotificationProperty, String> properties, NotificationPreferenceScopeFilterType filterType)
    {
        StringBuilder stringBuilder = new StringBuilder();

        String separator = "";

        try {
            int number = 0;
            for (NotificationPreferenceFilterScope scope : modelBridge.getNotificationPreferenceScopes(user, format,
                    filterType)) {
                number++;
                if (!scopeMatchesFilteringContext(scope, format, properties)) {
                    continue;
                }

                stringBuilder.append(separator);

                // If we have an EXCLUSIVE filter, negate the filter block
                stringBuilder.append(
                        (filterType.equals(NotificationPreferenceScopeFilterType.INCLUSIVE)) ? "(" : " NOT (");

                // Create a suffix to make sure our parameter has a unique name
                final String suffix = generateParameterSuffix(filterType, number);

                final String eventRestriction = generateQueryRestriction(suffix);

                switch (scope.getScopeReference().getType()) {
                    case DOCUMENT:
                        stringBuilder.append(String.format(
                                "(%s) AND event.wiki = :wiki_%s AND event.page = :page_%s",
                                eventRestriction, suffix, suffix));
                        break;
                    case SPACE:
                        stringBuilder.append(
                                String.format(
                                        "(%s) AND event.wiki = :wiki_%s AND event.space LIKE :space_%s ESCAPE '!'",
                                        eventRestriction, suffix, suffix));
                        break;
                    case WIKI:
                        stringBuilder.append(String.format("(%s) AND event.wiki = :wiki_%s",
                                eventRestriction, suffix));
                        break;
                    default:
                        break;
                }

                stringBuilder.append(")");

                separator = (filterType.equals(NotificationPreferenceScopeFilterType.INCLUSIVE)) ? " OR " : " AND ";
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        return stringBuilder.toString();
    }

    /**
     * Generate a map of parameters that should be used with the query made from
     * {@link #queryFilterAND(DocumentReference, NotificationFormat, Map)}
     * and {@link #queryFilterOR(DocumentReference, NotificationFormat, Map)}.
     *
     * @since 9.7RC1
     */
    private Map<String, Object> generateQueryFilterParams(DocumentReference user, NotificationFormat format,
            List<Map<NotificationProperty, String>> properties, NotificationPreferenceScopeFilterType filterType)
    {
        Map<String, Object> params = new HashMap<>();

        try {
            int number = 0;
            for (NotificationPreferenceFilterScope scope : modelBridge.getNotificationPreferenceScopes(user, format,
                    filterType)) {

                // Create a suffix to make sure our parameter has a unique name
                final String suffix = generateParameterSuffix(filterType, ++number);
                final String wikiParam = "wiki_%s";

                // Don't try to add parameters to the query if the scope does not match this filtering context
                if (!scopeMatchesFilteringContext(scope, format, properties)) {
                    continue;
                }

                switch (scope.getScopeReference().getType()) {
                    case DOCUMENT:
                        params.put(String.format(wikiParam, suffix), scope.getScopeReference().extractReference(
                                EntityType.WIKI).getName());
                        params.put(String.format("page_%s", suffix),
                                serializer.serialize(scope.getScopeReference()));
                        break;
                    case SPACE:
                        params.put(String.format(wikiParam, suffix), scope.getScopeReference().extractReference(
                                EntityType.WIKI).getName());
                        params.put(String.format("space_%s", suffix),
                                escape(serializer.serialize(scope.getScopeReference())) + "%"
                        );
                        break;
                    case WIKI:
                        params.put(String.format(wikiParam, suffix), scope.getScopeReference().extractReference(
                                EntityType.WIKI).getName());
                        break;
                    default:
                        break;
                }
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        return params;
    }

    protected String escape(String format)
    {
        // See EscapeLikeParametersQuery#convertParameters()
        return format.replaceAll("([%_!])", "!$1");
    }
}
