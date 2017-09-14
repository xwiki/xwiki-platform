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
package org.xwiki.notifications.filters.watch.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.InNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.internal.LocationOperatorNodeGenerator;
import org.xwiki.notifications.preferences.NotificationPreference;

/**
 * Filter to handle the watched entities feature.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Singleton
@Named(WatchedEntitiesNotificationFilter.FILTER_NAME)
public class WatchedEntitiesNotificationFilter implements NotificationFilter
{
    /**
     * Name of the filter.
     */
    public static final String FILTER_NAME = "watchedEntitiesNotificationFilter";

    private static final String ERROR = "Failed to filter the notifications.";

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private EntityReferenceResolver<String> resolver;

    @Inject
    private LocationOperatorNodeGenerator locationOperatorNodeGenerator;

    @Inject
    private EntityReferenceSerializer<String> defaultSerializer;

    @Inject
    private Logger logger;

    @Override
    public boolean filterEvent(Event event, DocumentReference user, NotificationFormat format)
    {
        List<String> watchedUsers = new ArrayList<>();
        List<EntityReference> watchedLocations = new ArrayList<>();
        fillWatchedEntities(watchedUsers, watchedLocations, user, format);

        return (watchedUsers.isEmpty() || !watchedUsers.contains(defaultSerializer.serialize(event.getUser())))
                && !doesDocumentMatchALocation(event.getDocument(), watchedLocations);
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        // The watchlist is not bound to any specific event type
        return false;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user, NotificationPreference preference)
    {
        return null;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user, NotificationFilterType type)
    {
        // Watchlist has yet not blacklist.
        if (type.equals(NotificationFilterType.EXCLUSIVE)) {
            return null;
        }

        List<String> watchedUsers = new ArrayList<>();
        List<EntityReference> watchedLocations = new ArrayList<>();
        fillWatchedEntities(watchedUsers, watchedLocations, user, null);

        // Users
        AbstractOperatorNode topNode = null;
        if (!watchedUsers.isEmpty()) {
            topNode = new InNode(
                    new PropertyValueNode(EventProperty.USER),
                    watchedUsers.stream().map(v -> new StringValueNode(v)).collect(Collectors.toList())
            );
        }

        // Locations
        for (EntityReference location : watchedLocations) {
            AbstractOperatorNode locationNode = locationOperatorNodeGenerator.generateNode(location);
            if (locationNode != null) {
                if (topNode == null) {
                    topNode = locationNode;
                } else {
                    topNode = topNode.or(locationNode);
                }
            }
        }

        return topNode;
    }

    private void fillWatchedEntities(List<String> watchedUsers, List<EntityReference> watchedLocations,
            DocumentReference user, NotificationFormat format)
    {
        try {
            Iterator<NotificationFilterPreference> iterator = format != null
                    ? getFilterPreferences(user, format) : getFilterPreferences(user);

            while (iterator.hasNext()) {
                NotificationFilterPreference preference = iterator.next();

                watchedUsers.addAll(preference.getProperties(NotificationFilterProperty.USER));
                watchedLocations.addAll(
                        parseWikiReferences(preference.getProperties(NotificationFilterProperty.WIKI)));
                watchedLocations.addAll(
                        parseSpaceReferences(preference.getProperties(NotificationFilterProperty.SPACE)));
                watchedLocations.addAll(
                        parsePageReferences(preference.getProperties(NotificationFilterProperty.PAGE)));
            }
        } catch (NotificationException e) {
            logger.warn(ERROR, e);
        }
    }

    private boolean doesDocumentMatchALocation(DocumentReference document, Collection<EntityReference> locations)
    {
        return locations.stream().anyMatch(location -> location.equals(document) || document.hasParent(location));
    }

    private Collection<EntityReference> parseWikiReferences(List<String> values)
    {
        return parseEntityReferences(values, v -> new WikiReference(v));
    }

    private Collection<EntityReference> parseSpaceReferences(List<String> values)
    {
        return parseEntityReferences(values, v -> resolver.resolve(v, EntityType.SPACE));
    }

    private Collection<EntityReference> parsePageReferences(List<String> values)
    {
        return parseEntityReferences(values, v -> resolver.resolve(v, EntityType.DOCUMENT));
    }

    private Collection<EntityReference> parseEntityReferences(List<String> values,
            Function<String, EntityReference> mapper)
    {
        return values.stream().map(mapper).collect(Collectors.toList());
    }

    private Iterator<NotificationFilterPreference> getFilterPreferences(DocumentReference user,
            NotificationFormat format) throws NotificationException
    {
        return notificationFilterManager.getFilterPreferences(user, this, NotificationFilterType.INCLUSIVE,
                format).iterator();
    }

    private Iterator<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        return notificationFilterManager.getFilterPreferences(user, this, NotificationFilterType.INCLUSIVE)
                .iterator();
    }

    @Override
    public String getName()
    {
        return FILTER_NAME;
    }

}
