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

    private static final String PREFIX_FORMAT = "scopeNotifFilter_%d";

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
        // Indicate if a restriction exist concerning this type of event
        boolean hasRestriction = false;
        // Indicate if a restriction matches the document of the event
        boolean matchRestriction = false;

        try {
            for (NotificationPreferenceScope scope : modelBridge.getNotificationPreferenceScopes(user, format)) {
                if (scope.getEventType().equals(event.getType())) {
                    hasRestriction = true;

                    if (event.getDocument().equals(scope.getScopeReference())
                            || event.getDocument().hasParent(scope.getScopeReference())) {
                        matchRestriction = true;
                        break;
                    }
                }
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        return hasRestriction && !matchRestriction;
    }

    @Override
    public String queryFilterOR(DocumentReference user, NotificationFormat format, String type)
    {
        StringBuilder stringBuilder = new StringBuilder();

        String separator = "";

        try {
            int number = 0;
            for (NotificationPreferenceScope scope : modelBridge.getNotificationPreferenceScopes(user, format)) {
                number++;
                if (!scope.getEventType().equals(type)) {
                    continue;
                }
                stringBuilder.append(separator);
                stringBuilder.append("(");

                // Create a suffix to make sure our parameter has a unique name
                final String suffix = String.format(PREFIX_FORMAT, number);

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
                separator = " OR ";
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }

        return stringBuilder.toString();
    }

    @Override
    public String queryFilterAND(DocumentReference user, NotificationFormat format, String type)
    {
        return null;
    }

    @Override
    public Map<String, Object> queryFilterParams(DocumentReference user, NotificationFormat format,
            List<String> enabledEventTypes)
    {
        Map<String, Object> params = new HashMap<>();

        try {
            int number = 0;
            for (NotificationPreferenceScope scope : modelBridge.getNotificationPreferenceScopes(user, format)) {

                // Create a suffix to make sure our parameter has a unique name
                final String suffix = String.format(PREFIX_FORMAT, ++number);
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

    private String escape(String format)
    {
        // See EscapeLikeParametersQuery#convertParameters()
        return format.replaceAll("([%_!])", "!$1");
    }
}
