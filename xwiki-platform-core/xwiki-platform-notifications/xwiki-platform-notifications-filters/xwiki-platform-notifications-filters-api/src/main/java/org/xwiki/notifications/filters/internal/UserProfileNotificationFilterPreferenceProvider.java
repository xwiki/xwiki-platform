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

import java.util.Date;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;

/**
 * This is the default implementation of the role {@link NotificationFilterPreferenceProvider}. It allows retrieving
 * filter preferences from the user XObjects.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named(UserProfileNotificationFilterPreferenceProvider.HINT)
@Singleton
public class UserProfileNotificationFilterPreferenceProvider implements NotificationFilterPreferenceProvider
{
    /**
     * Hint for this provider.
     */
    public static final String HINT = "userProfile";

    @Inject
    @Named("cached")
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException
    {
        return this.filterPreferencesModelBridge.getFilterPreferences(user);
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(WikiReference wikiReference)
        throws NotificationException
    {
        return this.filterPreferencesModelBridge.getFilterPreferences(wikiReference);
    }

    @Override
    public void saveFilterPreferences(DocumentReference user, Set<NotificationFilterPreference> filterPreferences)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.saveFilterPreferences(user, filterPreferences);
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException
    {
        this.filterPreferencesModelBridge.deleteFilterPreference(user, filterPreferenceId);
    }

    @Override
    public void deleteFilterPreferences(DocumentReference user, Set<String> filterPreferenceIds)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.deleteFilterPreferences(user, filterPreferenceIds);
    }

    @Override
    public void deleteFilterPreference(WikiReference wikiReference, String filterPreferenceId)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.deleteFilterPreference(wikiReference, filterPreferenceId);
    }

    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceId, boolean enabled)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.setFilterPreferenceEnabled(user, filterPreferenceId, enabled);
    }

    @Override
    public void setFilterPreferenceEnabled(WikiReference wikiReference, String filterPreferenceId, boolean enabled)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.setFilterPreferenceEnabled(wikiReference, filterPreferenceId, enabled);
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        this.filterPreferencesModelBridge.setStartDateForUser(user, startDate);
    }
}
