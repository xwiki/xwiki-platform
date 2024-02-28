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
package org.xwiki.notifications.filters.script;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.service.ScriptService;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;

/**
 * Script service for the notification filters.
 *
 * @since 9.7RC1
 * @version $Id$
 */
@Component
@Named("notification.filters")
@Singleton
public class NotificationFiltersScriptService implements ScriptService
{
    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    @Named("cached")
    private FilterPreferencesModelBridge cachedFilterPreferencesModelBridge;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Utility method to convert a given {@link UserReference} to {@link DocumentReference}.
     * This conversion is only possible if the given UserReference is a DocumentUserReference, or the instance of
     * {@link CurrentUserReference}. In other cases this will throw an exception.
     * This method should be removed once all APIs will use {@link UserReference}.
     * @param userReference the reference for which to retrieve a DocumentReference.
     * @return the context document reference if the user reference is null or the current user reference, else
     *         return the document reference contains in the DocumentUserReference.
     * @throws NotificationException if the user reference is not an instance of DocumentUserReference and not the
     *                               CurrentUserReference.
     * @deprecated Since 13.2RC1: the various API using DocumentReference for users should be refactored
     *              to use UserReference directly.
     */
    @Deprecated
    private DocumentReference convertReference(UserReference userReference) throws NotificationException
    {
        DocumentReference result;
        if (userReference == null || userReference == CurrentUserReference.INSTANCE) {
            result = documentAccessBridge.getCurrentUserReference();
        } else if (userReference instanceof DocumentUserReference) {
            result = ((DocumentUserReference) userReference).getReference();
        } else {
            throw new NotificationException(
                String.format("This should only be used with DocumentUserReference, "
                    + "the given reference was a [%s]", userReference.getClass().getSimpleName()));
        }
        return result;
    }

    /**
     * Get a set of notification filters that can be toggled by the current user.
     *
     * @return a set of notification filters that are toggleable
     * @throws NotificationException if an error occurs
     */
    public Set<NotificationFilter> getToggleableNotificationFilters() throws NotificationException
    {
        return getToggleableNotificationFilters(CurrentUserReference.INSTANCE);
    }

    /**
     * Get a set of notification filters that can be toggled by the given user.
     *
     * @param userReference the user for which to retrieve the notification filters.
     * @return a set of notification filters that are toggleable
     * @throws NotificationException if an error occurs
     * @since 13.2RC1
     */
    public Set<NotificationFilter> getToggleableNotificationFilters(UserReference userReference)
        throws NotificationException
    {
        return notificationFilterManager.getToggleableFilters(
            notificationFilterManager.getAllFilters(this.convertReference(userReference), false)).collect(
            Collectors.toSet());
    }

    /**
     * Get a set of notification filters that can be toggled for the given wiki.
     * Note: we use a distinct name than {@code getToggleableNotificationFilters} since a WikiReference can be converted
     * to a UserReference by our converters, check https://jira.xwiki.org/browse/XWIKI-18496.
     *
     * @param wikiReference the wiki for which to retrieve the notification filters.
     * @return a set of notification filters that are toggleable
     * @throws NotificationException if an error occurs
     * @since 13.3RC1
     */
    public Set<NotificationFilter> getWikiToggleableNotificationFilters(WikiReference wikiReference)
        throws NotificationException
    {
        return notificationFilterManager.getToggleableFilters(
            notificationFilterManager.getAllFilters(wikiReference)).collect(
            Collectors.toSet());
    }

    /**
     * @return a collection of every {@link NotificationFilter} available to the current user.
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    public Collection<NotificationFilter> getFilters() throws NotificationException
    {
        return getFilters(CurrentUserReference.INSTANCE);
    }

    /**
     * @param userReference the user for which to retrieve the filters.
     * @return a collection of every {@link NotificationFilter} available for the given user.
     * @throws NotificationException if the reference is not correct or if an error happens when retrieving the filters
     * @since 13.2RC1
     */
    public Collection<NotificationFilter> getFilters(UserReference userReference) throws NotificationException
    {
        return notificationFilterManager.getAllFilters(convertReference(userReference), false);
    }

    /**
     * Get the filters of the given wiki.
     * Note: we use a distinct name than {@code getFilters} since a WikiReference can be converted to a UserReference by
     * our converters, check https://jira.xwiki.org/browse/XWIKI-18496.
     * @param wikiReference the wiki for which to retrieve the filters.
     * @return a collection of every {@link NotificationFilter} available for the given user.
     * @throws NotificationException if the reference is not correct or if an error happens when retrieving the filters
     * @since 13.3RC1
     */
    public Collection<NotificationFilter> getWikiFilters(WikiReference wikiReference) throws NotificationException
    {
        return notificationFilterManager.getAllFilters(wikiReference);
    }

    /**
     * Get a collection of notification filters preferences that are available for the current user and that corresponds
     * to the given filter.
     *
     * @param filter the filter associated to the preferences
     * @return a set of {@link NotificationFilterPreference}
     * @throws NotificationException if an error occurs
     *
     * @since 9.8RC1
     */
    public Set<NotificationFilterPreference> getFilterPreferences(NotificationFilter filter)
            throws NotificationException
    {
        return getFilterPreferences(filter, CurrentUserReference.INSTANCE);
    }

    /**
     * Get a collection of notification filters preferences that are available for the given user and that corresponds
     * to the given filter.
     *
     * @param filter the filter associated to the preferences
     * @param userReference the user for which to retrieve the filter
     * @return a set of {@link NotificationFilterPreference}
     * @throws NotificationException if an error occurs
     *
     * @since 13.2RC1
     */
    public Set<NotificationFilterPreference> getFilterPreferences(NotificationFilter filter,
        UserReference userReference) throws NotificationException
    {
        return notificationFilterPreferenceManager.getFilterPreferences(
            notificationFilterPreferenceManager.getFilterPreferences(convertReference(userReference)), filter
        ).collect(Collectors.toSet());
    }

    /**
     * Get a collection of notification filters preferences that are available for the given wiki and that corresponds
     * to the given filter.
     * Note: we use a distinct name than {@code getFilterPreferences} since a WikiReference can be converted to a
     * UserReference by our converters, check https://jira.xwiki.org/browse/XWIKI-18496.
     *
     * @param filter the filter associated to the preferences
     * @param wikiReference the wiki for which to retrieve the filter
     * @return a set of {@link NotificationFilterPreference}
     * @throws NotificationException if an error occurs
     *
     * @since 13.3RC1
     */
    public Set<NotificationFilterPreference> getWikiFilterPreferences(NotificationFilter filter,
        WikiReference wikiReference) throws NotificationException
    {
        return notificationFilterPreferenceManager.getFilterPreferences(
            notificationFilterPreferenceManager.getFilterPreferences(wikiReference), filter
        ).collect(Collectors.toSet());
    }

    /**
     * Get a displayable form of the given {@link NotificationFilterPreference}.
     *
     * @param filter the filter bound to the given preference
     * @param preference the filter preference to display
     * @return a {@link Block} that can be used to display the given notification filter
     * @throws NotificationException if an error occurs
     *
     * @since 9.8RC1
     */
    public Block displayFilterPreference(NotificationFilter filter, NotificationFilterPreference preference)
            throws NotificationException
    {
        return notificationFilterManager.displayFilter(filter, preference);
    }

    /**
     * Delete a filter preference.
     * @param filterPreferenceId name of the filter preference
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    public void deleteFilterPreference(String filterPreferenceId) throws NotificationException
    {
        deleteFilterPreference(filterPreferenceId, CurrentUserReference.INSTANCE);
    }

    /**
     * Delete a filter preference for the given user.
     * @param filterPreferenceId name of the filter preference
     * @param userReference the user for which to delete the filter preference.
     * @throws NotificationException if an error happens
     *
     * @since 13.2RC1
     */
    public void deleteFilterPreference(String filterPreferenceId, UserReference userReference)
        throws NotificationException
    {
        notificationFilterPreferenceManager.deleteFilterPreference(convertReference(userReference),
            filterPreferenceId);
    }

    /**
     * Delete a filter preference for the given wiki.
     * Note: we use a distinct name than {@code deleteFilterPreference} since a WikiReference can be converted to a
     * UserReference by our converters, check https://jira.xwiki.org/browse/XWIKI-18496.
     * @param filterPreferenceId name of the filter preference
     * @param wikiReference the wiki for which to delete the filter preference.
     * @throws NotificationException if an error happens
     *
     * @since 13.3RC1
     */
    public void deleteWikiFilterPreference(String filterPreferenceId, WikiReference wikiReference)
        throws NotificationException
    {
        notificationFilterPreferenceManager.deleteFilterPreference(wikiReference,
            filterPreferenceId);
    }

    /**
     * Enable or disable a filter preference.
     * @param filterPreferenceId id of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    public void setFilterPreferenceEnabled(String filterPreferenceId, boolean enabled) throws NotificationException
    {
        this.setFilterPreferenceEnabled(filterPreferenceId, enabled, CurrentUserReference.INSTANCE);
    }

    /**
     * Enable or disable a filter preference.
     * @param filterPreferenceId id of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @param userReference the user for which to enable or disable a filter.
     * @throws NotificationException if an error happens
     *
     * @since 13.2RC1
     */
    public void setFilterPreferenceEnabled(String filterPreferenceId, boolean enabled, UserReference userReference)
        throws NotificationException
    {
        notificationFilterPreferenceManager.setFilterPreferenceEnabled(convertReference(userReference),
            filterPreferenceId, enabled);
    }

    /**
     * Enable or disable a filter preference.
     * Note: we use a distinct name than {@code setFilterPreferenceEnabled} since a WikiReference can be converted to a
     * UserReference by our converters, check https://jira.xwiki.org/browse/XWIKI-18496.
     *
     * @param filterPreferenceId id of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @param wikiReference the wiki for which to enable or disable a filter.
     * @throws NotificationException if an error happens
     *
     * @since 13.3RC1
     */
    public void setWikiFilterPreferenceEnabled(String filterPreferenceId, boolean enabled, WikiReference wikiReference)
        throws NotificationException
    {
        notificationFilterPreferenceManager.setFilterPreferenceEnabled(wikiReference,
            filterPreferenceId, enabled);
    }

    /**
     * Update the start date for every filter preference that current user has.
     *
     * @param startDate the new start date
     * @throws NotificationException if an error occurs
     *
     * @since 10.5RC1
     * @since 10.4
     * @since 9.11.5
     */
    public void setStartDate(Date startDate) throws NotificationException
    {
        this.setStartDate(startDate, CurrentUserReference.INSTANCE);
    }

    /**
     * Update the start date for every filter preference that given user has.
     *
     * @param startDate the new start date
     * @param userReference the user for which to set the start date
     * @throws NotificationException if an error occurs
     *
     * @since 13.2RC1
     */
    public void setStartDate(Date startDate, UserReference userReference) throws NotificationException
    {
        notificationFilterPreferenceManager.setStartDateForUser(convertReference(userReference), startDate);
    }

    /**
     * Create a scope notification filter preference for the current user.
     *
     * @param type type of the filter preference to create
     * @param formats formats concerned by the preference
     * @param eventTypes the event types concerned by the preference
     * @param reference the reference of the wiki, the space or the page concerned by the preference
     * @throws NotificationException if an error occurs
     *
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void createScopeFilterPreference(NotificationFilterType type, Set<NotificationFormat> formats,
            List<String> eventTypes, EntityReference reference) throws NotificationException
    {
        createScopeFilterPreference(type, formats, eventTypes, reference, CurrentUserReference.INSTANCE);
    }

    /**
     * Create a scope notification filter preference for the given user.
     *
     * @param type type of the filter preference to create
     * @param formats formats concerned by the preference
     * @param eventTypes the event types concerned by the preference
     * @param reference the reference of the wiki, the space or the page concerned by the preference
     * @param userReference the user for which to create the filter
     * @throws NotificationException if an error occurs
     *
     * @since 13.2RC1
     */
    public void createScopeFilterPreference(NotificationFilterType type, Set<NotificationFormat> formats,
        List<String> eventTypes, EntityReference reference, UserReference userReference) throws NotificationException
    {
        cachedFilterPreferencesModelBridge.createScopeFilterPreference(
            convertReference(userReference),
            type,
            formats,
            eventTypes,
            reference);
    }

    /**
     * Create a scope notification filter preference for the given user.
     * Note: we use a distinct name than {@code createScopeFilterPreference} since a WikiReference can be converted to a
     * UserReference by our converters, check https://jira.xwiki.org/browse/XWIKI-18496.
     *
     * @param type type of the filter preference to create
     * @param formats formats concerned by the preference
     * @param eventTypes the event types concerned by the preference
     * @param reference the reference of the wiki, the space or the page concerned by the preference
     * @param wikiReference the wiki for which to create the filter
     * @throws NotificationException if an error occurs
     *
     * @since 13.3RC1
     */
    public void createWikiScopeFilterPreference(NotificationFilterType type, Set<NotificationFormat> formats,
        List<String> eventTypes, EntityReference reference, WikiReference wikiReference) throws NotificationException
    {
        cachedFilterPreferencesModelBridge.createScopeFilterPreference(wikiReference, type, formats, eventTypes,
            reference);
    }
}
