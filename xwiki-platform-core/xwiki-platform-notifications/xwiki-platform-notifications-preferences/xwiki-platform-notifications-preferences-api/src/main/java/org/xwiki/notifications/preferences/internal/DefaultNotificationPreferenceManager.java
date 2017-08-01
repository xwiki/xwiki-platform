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
package org.xwiki.notifications.preferences.internal;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;

/**
 * This is the default implementation of {@link NotificationPreferenceManager}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
public class DefaultNotificationPreferenceManager implements NotificationPreferenceManager
{
    @Inject
    @Named("cached")
    private ModelBridge cachedModelBridge;

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference user)
            throws NotificationException
    {
        return cachedModelBridge.getNotificationsPreferences(user);
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        cachedModelBridge.setStartDateForUser(user, startDate);
    }

    @Override
    public void saveNotificationsPreferences(DocumentReference userReference,
            List<NotificationPreference> notificationPreferences) throws NotificationException
    {
        cachedModelBridge.saveNotificationsPreferences(userReference, notificationPreferences);
    }
}
