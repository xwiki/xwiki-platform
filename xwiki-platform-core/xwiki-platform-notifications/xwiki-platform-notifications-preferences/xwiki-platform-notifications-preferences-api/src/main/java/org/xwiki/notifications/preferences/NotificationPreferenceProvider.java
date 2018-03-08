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
package org.xwiki.notifications.preferences;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.stability.Unstable;

/**
 * Provides notifications preferences from multiple sources.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Role
@Unstable
public interface NotificationPreferenceProvider
{
    /**
     * Get the priority that the preferences given by this provider should have.
     * This is useful when different notification preferences are in conflict. The higher is the number, the higher
     * is the priority.
     *
     * @return the priority of the provider
     */
    int getProviderPriority();

    /**
     * Get every registered {@link NotificationPreference} for the given user.
     *
     * @param user the user for which to retrieve the notification preferences
     * @return a list of notification preferences
     * @throws NotificationException if an error happened
     */
    List<NotificationPreference> getPreferencesForUser(DocumentReference user) throws NotificationException;

    /**
     * Get every registered and/or inherited {@link NotificationPreference} for the given wiki.
     *
     * @param wiki the wiki for which to retrieve the notification preferences
     * @return a list of notification preferences
     * @throws NotificationException if an error happened
     *
     * @since 10.2RC1
     * @since 9.11.4
     */
    List<NotificationPreference> getPreferencesForWiki(WikiReference wiki) throws NotificationException;

    /**
     * Save a given list of preferences.
     *
     * @param preferences the {@link NotificationPreference} to save
     * @throws NotificationException if an error occurred
     */
    void savePreferences(List<NotificationPreference> preferences) throws NotificationException;
}
