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
package org.xwiki.notifications.notifiers.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.user.EventUserFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Provider user events related helpers.
 * 
 * @version $Id$
 * @since 12.1RC1
 */
@Component(roles = UserEventManager.class)
@Singleton
public class UserEventManager
{
    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Logger logger;

    /**
     * @param event the event
     * @param user the reference of the user
     * @param format the format of the notification
     * @return true if the passed user ask to be notified about the passed event
     */
    public boolean isListening(Event event, DocumentReference user, NotificationFormat format)
    {
        if (hasAccess(user, event) && (hasCorrespondingNotificationPreference(user, event, format)
            || isTriggeredByAFollowedUser(user, event, format))) {
            try {
                // Apply the filters that the user has defined in its notification preferences
                // If one of the events present in the composite event does not match a user filter, remove the event
                List<NotificationFilter> filters =
                    new ArrayList<>(this.notificationFilterManager.getAllFilters(user, true));
                filters.sort(null);

                return !isEventFiltered(filters, event, user, format);
            } catch (NotificationException e) {
                this.logger.error("Failed to get event filters for user [{}]", user, e);
            }
        }

        return false;
    }

    private boolean hasAccess(DocumentReference user, Event event)
    {
        DocumentReference document = event.getDocument();
        if (document != null) {
            return this.authorizationManager.hasAccess(Right.VIEW, user, document);
        }

        SpaceReference space = event.getSpace();
        if (space != null) {
            return this.authorizationManager.hasAccess(Right.VIEW, user, space);
        }

        WikiReference wiki = event.getWiki();

        if (wiki != null) {
            return this.authorizationManager.hasAccess(Right.VIEW, user, wiki);
        }

        return true;
    }

    private boolean hasCorrespondingNotificationPreference(DocumentReference user, Event event,
        NotificationFormat format)
    {
        try {
            for (NotificationPreference notificationPreference : this.notificationPreferenceManager
                .getAllPreferences(user)) {
                if (notificationPreference.getFormat() == format
                    && notificationPreference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)
                    && notificationPreference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)
                        .equals(event.getType())) {
                    return notificationPreference.isNotificationEnabled();
                }
            }
        } catch (NotificationException e) {
            this.logger.warn("Unable to retrieve the notifications preferences of [{}]: {}", user,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return false;
    }

    private boolean isTriggeredByAFollowedUser(DocumentReference user, Event event, NotificationFormat format)
    {
        try {
            return notificationFilterPreferenceManager.getFilterPreferences(user).stream()
                .anyMatch(fp -> isUserFilterPreference(fp, format) && matchUser(fp, event));
        } catch (NotificationException e) {
            return false;
        }
    }

    private boolean isUserFilterPreference(NotificationFilterPreference filterPreference, NotificationFormat format)
    {
        return matchFilter(filterPreference) && matchFormat(filterPreference, format)
            && matchFilterType(filterPreference, NotificationFilterType.INCLUSIVE) && matchAllEvents(filterPreference);
    }

    private boolean matchUser(NotificationFilterPreference filterPreference, Event event)
    {
        return event.getUser().equals(this.referenceResolver.resolve(filterPreference.getUser()));
    }

    private boolean matchFormat(NotificationFilterPreference filterPreference, NotificationFormat format)
    {
        return format == null || filterPreference.getNotificationFormats().contains(format);
    }

    private boolean matchFilter(NotificationFilterPreference pref)
    {
        return pref.isEnabled() && EventUserFilter.FILTER_NAME.equals(pref.getFilterName());
    }

    private boolean matchFilterType(NotificationFilterPreference pref, NotificationFilterType filterType)
    {
        return pref.getFilterType() == filterType;
    }

    private boolean matchAllEvents(NotificationFilterPreference filterPreference)
    {
        // When the list of event types concerned by the filter is empty, we consider that the filter concerns
        // all events.
        return filterPreference.getEventTypes().isEmpty();
    }

    private boolean isEventFiltered(List<NotificationFilter> filters, Event event, DocumentReference user,
        NotificationFormat format) throws NotificationException
    {
        Collection<NotificationFilterPreference> filterPreferences =
            notificationFilterPreferenceManager.getFilterPreferences(user);
        for (NotificationFilter filter : filters) {
            NotificationFilter.FilterPolicy policy = filter.filterEvent(event, user, filterPreferences, format);
            switch (policy) {
                case FILTER:
                    return true;
                case KEEP:
                    return false;
                default:
                    // Do nothing
            }
        }

        return false;
    }

}
