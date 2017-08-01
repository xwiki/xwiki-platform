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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;

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
     * Get all notification preference scope of the given user.
     *
     * @param user user interested in the notifications
     * @param format format on which the preferences apply
     * @return the list of notification preference scopes.
     * @throws NotificationException if error happens
     *
     * @since 9.5RC1
     */
    List<NotificationPreferenceFilterScope> getNotificationPreferenceScopes(DocumentReference user,
            NotificationFormat format) throws NotificationException;

    /**
     * Get all notification preference scope of the given user.
     *
     * @param user user interested in the notifications
     * @param format format on which the preferences apply
     * @param type the filter type of the scope we want to retrieve, see {@link NotificationPreferenceScopeFilterType}
     * @return the list of notification preference scopes.
     * @throws NotificationException if error happens
     *
     * @since 9.7RC1
     */
    List<NotificationPreferenceFilterScope> getNotificationPreferenceScopes(DocumentReference user,
            NotificationFormat format, NotificationPreferenceScopeFilterType type) throws NotificationException;
}
