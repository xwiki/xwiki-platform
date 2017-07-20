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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFilter;
import org.xwiki.notifications.NotificationFormat;

/**
 * Notification filter that handle the generic {@link NotificationPreferenceScope}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named("scope")
@Singleton
public class ScopeNotificationFilter implements NotificationFilter
{
    private static final String ERROR = "Failed to filter the notifications.";

    private static final String PREFIX_FORMAT = "scopeNotifFilter_%s_%d";

    @Inject
    @Named("cached")
    private ModelBridge modelBridge;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Logger logger;

    @Override
    public boolean filterEvent(Event event, DocumentReference user, NotificationFormat format)
    {
        return this.filterEventByFilterType(event, user, format, NotificationPreferenceScopeFilterType.EXCLUSIVE)
                || this.filterEventByFilterType(event, user, format, NotificationPreferenceScopeFilterType.INCLUSIVE);
    }

    @Override
    public String queryFilterOR(DocumentReference user, NotificationFormat format, String type)
    {
        return this.generateQueryString(user, format, type, NotificationPreferenceScopeFilterType.INCLUSIVE);
    }

    @Override
    public String queryFilterAND(DocumentReference user, NotificationFormat format, String type)
    {
        return this.generateQueryString(user, format, type, NotificationPreferenceScopeFilterType.EXCLUSIVE);
    }

    @Override
    public Map<String, Object> queryFilterParams(DocumentReference user, NotificationFormat format,
            List<String> enabledEventTypes)
    {
        Map<String, Object> params =
                this.generateQueryFilterParams(user, format, enabledEventTypes,
                        NotificationPreferenceScopeFilterType.INCLUSIVE);
        params.putAll(this.generateQueryFilterParams(user, format, enabledEventTypes,
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
            for (NotificationPreferenceScope scope : modelBridge.getNotificationPreferenceScopes(user, format,
                    scopeFilterType)) {
                if (scope.getEventType().equals(event.getType())) {
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
        if (scopeFilterType.equals(NotificationPreferenceScopeFilterType.INCLUSIVE)) {
            return hasRestriction && !matchRestriction;
        } else {
            return false;
        }
    }

    /**
     * Generate a map of parameters that should be used with the query made from
     * {@link #queryFilterAND(DocumentReference, NotificationFormat, String)}
     * and {@link #queryFilterOR(DocumentReference, NotificationFormat, String)}.
     *
     * @since 9.7RC1
     */
    private Map<String, Object> generateQueryFilterParams(DocumentReference user, NotificationFormat format,
            List<String> enabledEventTypes, NotificationPreferenceScopeFilterType filterType)
    {
        Map<String, Object> params = new HashMap<>();

        try {
            int number = 0;
            for (NotificationPreferenceScope scope : modelBridge.getNotificationPreferenceScopes(user, format,
                    filterType)) {

                // Create a suffix to make sure our parameter has a unique name
                final String suffix = String.format(PREFIX_FORMAT, filterType, ++number);
                final String wikiParam = "wiki_%s";

                // Don't try to add parameters to the query if the event type is not enabled, the parameters will not
                // be found in the query and it will be broken
                if (!enabledEventTypes.contains(scope.getEventType())) {
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

    /**
     * Generate parts of a query used to retrieve events from a given user.
     * Depending on the {@link NotificationPreferenceScopeFilterType} given, the generated query will have a different
     * content.
     *
     * Generated syntax for INCLUSIVE filters:
     * (--filter1--) OR (--filter2--) OR (--filter3--) ...
     *
     * Generated syntax for EXCLUSIVE filters:
     * NOT (--filter1--) AND NOT (--filter2--) AND NOT (--filter3--) ...
     *
     * @since 9.7RC1
     */
    private String generateQueryString(DocumentReference user, NotificationFormat format, String type,
            NotificationPreferenceScopeFilterType filterType)
    {
        StringBuilder stringBuilder = new StringBuilder();

        String separator = "";

        try {
            int number = 0;
            for (NotificationPreferenceScope scope : modelBridge.getNotificationPreferenceScopes(user, format,
                    filterType)) {
                number++;
                if (!scope.getEventType().equals(type)) {
                    continue;
                }

                stringBuilder.append(separator);

                // If we have an EXCLUSIVE filter, negate the filter block
                stringBuilder.append(
                        (filterType.equals(NotificationPreferenceScopeFilterType.INCLUSIVE)) ? "(" : " NOT (");

                // Create a suffix to make sure our parameter has a unique name
                final String suffix = String.format(PREFIX_FORMAT, filterType.name(), number);

                switch (scope.getScopeReference().getType()) {
                    case DOCUMENT:
                        stringBuilder.append(String.format("event.wiki = :wiki_%s AND event.page = :page_%s",
                                suffix, suffix));
                        break;
                    case SPACE:
                        stringBuilder.append(
                                String.format(
                                        "event.wiki = :wiki_%s AND event.space LIKE :space_%s ESCAPE '!'",
                                        suffix,
                                        suffix
                                )
                        );
                        break;
                    case WIKI:
                        stringBuilder.append(String.format("event.wiki = :wiki_%s", suffix));
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

    private String escape(String format)
    {
        // See EscapeLikeParametersQuery#convertParameters()
        return format.replaceAll("([%_!])", "!$1");
    }
}
