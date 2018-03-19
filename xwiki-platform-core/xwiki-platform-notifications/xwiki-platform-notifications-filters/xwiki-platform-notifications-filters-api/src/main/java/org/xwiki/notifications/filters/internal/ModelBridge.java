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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;

/**
 * Internal role that make requests to the model and avoid a direct dependency to oldcore.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
public interface ModelBridge
{
    /**
     * Get all the notification preferences that corresponds located on the given document.
     *
     * @param documentReference the document where to load the preferences.
     * @param providerHint hint of the provider
     * @return a set of available filter preferences
     * @throws NotificationException if an error happens
     *
     * @since 10.3RC1
     * @since 9.11.5
     */
    Set<NotificationFilterPreference> getFilterPreferences(DocumentReference documentReference, String providerHint)
            throws NotificationException;

    /**
     * For all toggeable notification filters, get if the filter is enabled regarding given document.
     *
     * @param documentReference the document
     * @return a map of notification filters with their activation state
     * @throws NotificationException if an error happens
     * @since 10.3RC1
     * @since 9.11.5
     */
    Map<String, Boolean> getToggeableFilterActivations(DocumentReference documentReference)
            throws NotificationException;

    /**
     * Delete a filter preference.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferenceName name of the filter preference
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    void deleteFilterPreference(DocumentReference user, String filterPreferenceName) throws NotificationException;

    /**
     * Enable or disable a filter preference.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferenceName name of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceName, boolean enabled)
            throws NotificationException;

    /**
     * Save a collection of NotificationFilterPreferences.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferences a list of NotificationFilterPreference
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    void saveFilterPreferences(DocumentReference user, Collection<NotificationFilterPreference> filterPreferences)
        throws NotificationException;
}
