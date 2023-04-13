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

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProvider;

/**
 * This is the default implementation of {@link NotificationPreferenceProvider}.
 * Note that this provider uses the notification preferences defined in the user profile XObjects in order to work
 * properly.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
@Named(UserProfileNotificationPreferenceProvider.NAME)
public class UserProfileNotificationPreferenceProvider extends AbstractDocumentNotificationPreferenceProvider
{
    /**
     * The name of the provider.
     */
    public static final String NAME = "userProfile";

    @Override
    public int getProviderPriority()
    {
        return 500;
    }

    @Override
    public List<NotificationPreference> getPreferencesForUser(DocumentReference user)
            throws NotificationException
    {
        return cachedNotificationPreferenceModelBridge.getNotificationsPreferences(user);
    }

    @Override
    public List<NotificationPreference> getPreferencesForWiki(WikiReference wiki) throws NotificationException
    {
        // It makes no sense in this provider
        return Collections.emptyList();
    }
}
