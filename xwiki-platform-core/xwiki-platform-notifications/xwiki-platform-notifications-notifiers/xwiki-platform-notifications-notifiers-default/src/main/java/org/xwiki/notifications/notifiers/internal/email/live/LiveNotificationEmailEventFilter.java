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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.user.EventUserFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReferenceResolver;

/**
 * Component dedicated to perform filtering of events before sending them live.
 * TODO: this component should probably be refactored to be used for both live email and user event dispatcher.
 *
 * @version $Id$
 * @since 13.1
 * @since 12.10.5
 * @since 12.6.8
 */
@Component(roles = LiveNotificationEmailEventFilter.class)
@Singleton
public class LiveNotificationEmailEventFilter
{
    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Check if an event is filtered by one of the given filters.
     *
     * @param filters the filters against which to check the event.
     * @param event the event to check.
     * @param user the user for whom to perform the filtering.
     * @return {@code true} if at least one filter filters the event, {@code false} in all other cases.
     * @throws NotificationException in case of problem in a filter.
     */
    public boolean isEventFiltered(List<NotificationFilter> filters, Event event, DocumentReference user)
        throws NotificationException
    {
        Collection<NotificationFilterPreference> filterPreferences
            = notificationFilterPreferenceManager.getFilterPreferences(user);
        for (NotificationFilter filter : filters) {
            NotificationFilter.FilterPolicy policy = filter.filterEvent(event, user, filterPreferences,
                NotificationFormat.EMAIL);
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

    /**
     * Check if an event should be considered for the given user.
     *
     * @param user the user for whom to check if the event should be handled.
     * @param event the event to check.
     * @return {@code true} if the user has a notification preference matching the event, or if the
     *          event is coming from a followed user, {@code false} in all other cases.
     */
    public boolean isCompositeEventHandled(DocumentReference user, CompositeEvent event)
    {
        return this.hasCorrespondingNotificationPreference(user, event) || this.isTriggeredByAFollowedUser(user, event);
    }

    /**
     * Check if the event contains a document that can be accessed.
     * We consider a document can be accesser if the user has proper rights to see it, and if the document is not
     * hidden, or if the document is hidden, if the user decided to display hidden documents.
     * @param user the user for which to check if the document can be accessed.
     * @param event the event that might contain a document.
     * @return {@code true} if the event contains a document and the user has view rights on it and the document is not
     *           hidden, or if hidden if the user displays them, {@code false} in all other cases.
     */
    public boolean canAccessEvent(DocumentReference user, CompositeEvent event) throws NotificationException
    {
        DocumentReference document = event.getDocument();
        return (document == null
            || (authorizationManager.hasAccess(Right.VIEW, user, document) && this.isHiddenStatusHandled(user, event)));
    }

    /**
     * Check if the given event concerns an hidden document.
     * @param event the event for which to check the document.
     * @return {@code true} if the event contains a document which is hidden, {@code false} in all other cases.
     * @throws NotificationException if the event contains a document that cannot be loaded.
     */
    private boolean isHiddenStatusHandled(DocumentReference user, CompositeEvent event) throws NotificationException
    {
        if (event.getDocument() != null) {
            try {
                DocumentModelBridge document =
                    this.documentAccessBridge.getTranslatedDocumentInstance(event.getDocument());
                return !document.isHidden() || this.userPropertiesResolver
                    .resolve(this.userReferenceResolver.resolve(user)).displayHiddenDocuments();
            } catch (Exception e) {
                throw new NotificationException(
                    String.format("Error while loading [%s] to check if it is hidden.", event.getDocument()));
            }
        }
        return false;
    }

    /**
     * Test if the given user has enabled the notification preference corresponding to the given composite event.
     *
     * @param user the user from which we have to extract
     * @param compositeEvent
     * @return
     */
    private boolean hasCorrespondingNotificationPreference(DocumentReference user, CompositeEvent compositeEvent)
    {
        try {
            for (NotificationPreference notificationPreference
                : notificationPreferenceManager.getAllPreferences(user)) {
                if (notificationPreference.getFormat().equals(NotificationFormat.EMAIL)
                    && notificationPreference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)
                    && notificationPreference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)
                    .equals(compositeEvent.getType())) {
                    return notificationPreference.isNotificationEnabled();
                }
            }
        } catch (NotificationException e) {
            this.logger.warn("Unable to retrieve the notifications preferences of [{}]: [{}]", user,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return false;
    }

    private boolean isTriggeredByAFollowedUser(DocumentReference user, CompositeEvent compositeEvent)
    {
        try {
            return notificationFilterPreferenceManager.getFilterPreferences(user).stream().anyMatch(
                fp -> isUserFilterPreference(fp) && matchUser(fp, compositeEvent)
            );
        } catch (NotificationException e) {
            return false;
        }
    }

    /**
     * @param filterPreference a notification filter preference
     * @return either or not it is a preference about following a user
     */
    private boolean isUserFilterPreference(NotificationFilterPreference filterPreference)
    {
        return matchFilter(filterPreference)
            && matchFormat(filterPreference, NotificationFormat.EMAIL)
            && matchFilterType(filterPreference, NotificationFilterType.INCLUSIVE)
            && matchAllEvents(filterPreference);
    }

    /**
     * @param filterPreference a filter preference
     * @param event a composite event
     * @return either or not the given preference is about following a user that have generated the given composite
     * event
     */
    private boolean matchUser(NotificationFilterPreference filterPreference, CompositeEvent event)
    {
        return event.getUsers().contains(referenceResolver.resolve(filterPreference.getUser()));
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

    /**
     * @param filterPreference a filter preference
     * @return either or not the preference concern all event types
     */
    private boolean matchAllEvents(NotificationFilterPreference filterPreference)
    {
        // When the list of event types concerned by the filter is empty, we consider that the filter concerns
        // all events.
        return filterPreference.getEventTypes().isEmpty();
    }
}
