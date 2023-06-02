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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.Event;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
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
import org.xwiki.notifications.preferences.internal.cache.UnboundedEntityCacheManager;
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
public class UserEventManager implements Initializable
{
    private static final String USERDATECACHE_NAME = "UserCreationDate";

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

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceFactory entityReferenceFactory;

    @Inject
    private UnboundedEntityCacheManager cacheManager;

    private Map<EntityReference, Date> userCreationDateCache;

    @Override
    public void initialize() throws InitializationException
    {
        this.userCreationDateCache = this.cacheManager.createCache(USERDATECACHE_NAME, false);
    }

    /**
     * @param event the event
     * @param user the reference of the user
     * @param format the format of the notification
     * @return true if the passed user ask to be notified about the passed event
     */
    public boolean isListening(Event event, DocumentReference user, NotificationFormat format)
    {
        try {
            if (hasAccess(user, event) && isEventAfterUserCreationDate(event, user)
                && (hasCorrespondingNotificationPreference(user, event, format)
                    || isTriggeredByAFollowedUser(user, event, format))) {
                // Apply the filters that the user has defined in its notification preferences
                // If one of the events present in the composite event does not match a user filter, remove the event
                List<NotificationFilter> filters = new ArrayList<>(this.notificationFilterManager.getAllFilters(user,
                    true, NotificationFilter.FilteringPhase.PRE_FILTERING));
                filters.sort(null);

                return !isEventFiltered(filters, event, user, format);
            }
        } catch (NotificationException e) {
            this.logger.error("Failed to get event filters for user [{}]", user, e);
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

    /**
     * Check if the event has been sent after the creation date of the user.
     *
     * @param event the event to check
     * @param user the targeted user
     * @return {@code true} if event's date or user's creation date is null, or if the event date is after the user's
     *         creation date.
     * @throws NotificationException in case of problem to retrieve user's creation date.
     */
    private boolean isEventAfterUserCreationDate(Event event, DocumentReference user) throws NotificationException
    {
        Date userCreationDate = getUserCreationDate(user);
        return event.getDate() == null || userCreationDate == null
        // after and before API are "strictly after" and "strictly before",
        // here we use the negative way to ensure we also accept "equals" date.
            || !event.getDate().before(userCreationDate);
    }

    /**
     * Retrieve user's creation date from the document creation date and put it in cache for later retrieval.
     *
     * @param user the user for whom to retrieve creation date.
     * @return the actual creation date of the user.
     * @throws NotificationException in case of problem to access the document.
     */
    private Date getUserCreationDate(DocumentReference user) throws NotificationException
    {
        Date result;
        if (!this.userCreationDateCache.containsKey(user)) {
            try {
                result = this.documentAccessBridge.getDocumentInstance(user).getCreationDate();
                this.userCreationDateCache.put(this.entityReferenceFactory.getReference(user), result);
            } catch (Exception e) {
                throw new NotificationException(String.format("Cannot find creation date for user [%s].", user), e);
            }
        } else {
            result = this.userCreationDateCache.get(user);
        }

        return result;
    }

    private boolean hasCorrespondingNotificationPreference(DocumentReference user, Event event,
        NotificationFormat format)
    {
        try {
            List<NotificationPreference> allPreferences = this.notificationPreferenceManager.getAllPreferences(user);
            for (NotificationPreference notificationPreference : allPreferences) {
                if (notificationPreference.getFormat() == format
                    && notificationPreference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)
                    && notificationPreference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)
                        .equals(event.getType())) {

                    // Ensures that the preference is enabled, and that the preference start date is before the event
                    // date. Note that we return true also if event date is null or notification preference is null
                    // for possible backward compatibility with old events.
                    return notificationPreference.isNotificationEnabled()
                        && (notificationPreference.getStartDate() == null || event.getDate() == null
                        // after and before API are "strictly after" and "strictly before",
                        // here we use the negative way to ensure we also accept "equals" date.
                            || !event.getDate().before(notificationPreference.getStartDate()));
                }
            }
            return allPreferences.isEmpty();
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
                .anyMatch(fp -> isFilterCreatedBeforeEvent(event, fp) && isUserFilterPreference(fp, format)
                    && matchUser(fp, event));
        } catch (NotificationException e) {
            return false;
        }
    }

    /**
     * Check that the filter preference started before the event has been sent.
     *
     * @param event the event to check
     * @param filterPreference the preference to check
     * @return {@code true} if the event date or the preference start date is null for backward compatibility, or if the
     *         preference start date is before the event date.
     */
    private boolean isFilterCreatedBeforeEvent(Event event, NotificationFilterPreference filterPreference)
    {
        return event.getDate() == null || filterPreference.getStartingDate() == null
        // after and before API are "strictly after" and "strictly before",
        // here we use the negative way to ensure we also accept "equals" date.
            || !event.getDate().before(filterPreference.getStartingDate());
    }

    private boolean isUserFilterPreference(NotificationFilterPreference filterPreference, NotificationFormat format)
    {
        return matchFilter(filterPreference) && matchFormat(filterPreference, format)
            && matchFilterType(filterPreference, NotificationFilterType.INCLUSIVE) && matchAllEvents(filterPreference);
    }

    private boolean matchUser(NotificationFilterPreference filterPreference, Event event)
    {
        return event.getUser() != null
            && event.getUser().equals(this.referenceResolver.resolve(filterPreference.getUser()));
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
