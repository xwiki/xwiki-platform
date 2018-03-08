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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
 * @since 9.11.4
 */
public abstract class AbstractDocumentNotificationPreferenceProvider implements NotificationPreferenceProvider
{
    @Inject
    @Named("cached")
    protected ModelBridge cachedModelBridge;

    @Inject
    protected Logger logger;

    @Override
    public void savePreferences(List<NotificationPreference> preferences) throws NotificationException
    {
        // Create a map of preferences per target, so we can save the target's document only once for all preferences
        Map<EntityReference, List<NotificationPreference>> preferencesPerTarget = new HashMap<>();

        for (NotificationPreference preference : preferences) {
            if (preference instanceof TargetableNotificationPreference) {
                TargetableNotificationPreference targetablePreference = (TargetableNotificationPreference) preference;

                List<NotificationPreference> list = preferencesPerTarget.get(targetablePreference.getTarget());
                if (list == null) {
                    list = new ArrayList<>();
                    preferencesPerTarget.put(targetablePreference.getTarget(), list);
                }

                list.add(targetablePreference);
            } else {
                logger.warn("Unsupported NotificationPreference class: [{}]. This preference will not be saved.",
                        preference.getClass().getName());
            }
        }

        for (Map.Entry<EntityReference, List<NotificationPreference>> entry : preferencesPerTarget.entrySet()) {
            savePreferences(entry.getValue(), entry.getKey());
        }
    }

    protected void savePreferences(List<NotificationPreference> preferences, EntityReference target)
            throws NotificationException
    {
        if (target instanceof DocumentReference) {
            cachedModelBridge.saveNotificationsPreferences((DocumentReference) target, preferences);
        } else {
            logger.warn("Preference's target [{}] is not a document reference. The corresponding preference will not"
                    + " be saved.");
        }
    }
}
