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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProvider;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;

/**
 * Abstract class for implementation of {@link NotificationPreferenceProvider} when the preferences are stored in a
 * document.
 *
 * @version $Id$
 * @since 10.2RC1
 * @since 9.11.3
 */
public abstract class AbstractDocumentNotificationPreferenceProvider implements NotificationPreferenceProvider
{
    @Inject
    @Named("cached")
    protected ModelBridge cachedModelBridge;

    @Override
    public void savePreferences(List<NotificationPreference> preferences) throws NotificationException
    {
        for (NotificationPreference preference : preferences) {
            // As we are saving notification preferences a user profile, we can only accept
            // TargetableNotificationPreference.
            if (preference instanceof TargetableNotificationPreference) {
                TargetableNotificationPreference targetablePreference = (TargetableNotificationPreference) preference;

                // TODO: Find a way to send one array of preferences to save per user
                cachedModelBridge.saveNotificationsPreferences(targetablePreference.getTarget(),
                        Arrays.asList(targetablePreference));
            } else {
                throw new NotificationException(String.format("The notification preference %s is not a "
                        + "TargetableNotificationPreference.", preference));
            }
        }
    }
}
