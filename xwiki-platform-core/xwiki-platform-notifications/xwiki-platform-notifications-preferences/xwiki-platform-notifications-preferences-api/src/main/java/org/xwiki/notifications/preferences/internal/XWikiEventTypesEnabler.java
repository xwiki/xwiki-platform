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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;

import static java.util.Arrays.asList;

/**
 * Component that make sure that notification are enabled for at least one XWiki Event Types (create, update, delete,
 * addComment). This is or internal usage.
 *
 * @version $Id$
 * @since 10.3
 * @since 9.11.4
 */
@Component(roles = XWikiEventTypesEnabler.class)
@Singleton
public class XWikiEventTypesEnabler
{
    private static final List<String> XWIKI_EVENT_TYPES = asList("create", "update", "delete", "addComment");

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private Provider<TargetableNotificationPreferenceBuilder> targetableNotificationPreferenceBuilderProvider;

    /**
     * Make sure that notification are enabled for at least one XWiki Event Types (create, update, delete,
     * addComment) for the current user.
     * @param user user for who we handle notifications
     * @throws NotificationException if an error occurs
     *
     * @since 9.11.5
     * @since 10.3
     */
    public void ensureXWikiNotificationsAreEnabled(DocumentReference user) throws NotificationException
    {
        if (isNotificationDisabled(user)) {
            TargetableNotificationPreferenceBuilder builder = targetableNotificationPreferenceBuilderProvider.get();
            List<NotificationPreference> preferencesToCreate = new ArrayList<>();
            Date now = new Date();
            for (String eventType : XWIKI_EVENT_TYPES) {
                for (NotificationFormat format : NotificationFormat.values()) {
                    preferencesToCreate.add(createNotificationPreference(user, builder, eventType, format, now));
                }
            }
            notificationPreferenceManager.savePreferences(preferencesToCreate);
        }
    }

    /**
     * @param user user for who we handle notifications
     * @return true if there is not a single preference enabled for the default event types.
     * @throws NotificationException if an error occurs
     *
     * @since 9.11.5
     * @since 10.3
     */
    public boolean isNotificationDisabled(DocumentReference user) throws NotificationException
    {
        List<NotificationPreference> preferences = notificationPreferenceManager.getAllPreferences(user);
        // We consider that notifs are disabled if there is not a single preference enabled for the default event types.
        return preferences.stream().noneMatch(pref -> pref.isNotificationEnabled()
                && XWIKI_EVENT_TYPES.contains(pref.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)));
    }

    private TargetableNotificationPreference createNotificationPreference(DocumentReference user,
            TargetableNotificationPreferenceBuilder builder, String eventType, NotificationFormat format, Date date)
    {
        builder.prepare();
        builder.setCategory(NotificationPreferenceCategory.DEFAULT);
        builder.setEnabled(true);
        builder.setFormat(format);
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, eventType);
        builder.setProperties(properties);
        builder.setProviderHint(UserProfileNotificationPreferenceProvider.NAME);
        builder.setStartDate(date);
        builder.setTarget(user);
        return builder.build();
    }
}
