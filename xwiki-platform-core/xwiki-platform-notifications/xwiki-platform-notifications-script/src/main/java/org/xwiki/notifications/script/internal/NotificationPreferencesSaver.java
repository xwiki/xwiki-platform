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
package org.xwiki.notifications.script.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper to save preferences given as JSON.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component(roles = NotificationPreferencesSaver.class)
@Singleton
public class NotificationPreferencesSaver
{

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    /**
     * Save preferences given as JSON.
     * @param json a list of preferences as JSON
     * @param userReference reference of the user concerned by the preferences
     * @throws NotificationException if error happens
     */
    public void saveNotificationPreferences(String json, DocumentReference userReference) throws NotificationException
    {
        List<NotificationPreference> toSave = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> preferences = objectMapper.reader().forType(List.class).readValue(json);
            for (Map<String, Object> item : preferences) {
                String eventType = (String) item.get("eventType");
                NotificationFormat format = NotificationFormat.valueOf(((String) item.get("format")).toUpperCase());
                boolean enabled = (Boolean) item.get("enabled");
                toSave.add(new NotificationPreference(eventType, enabled, format));
            }

            notificationPreferenceManager.saveNotificationsPreferences(userReference, toSave);

        } catch (Exception e) {
            throw new NotificationException("Failed to save preferences for notifications given as JSON.", e);
        }
    }
}
