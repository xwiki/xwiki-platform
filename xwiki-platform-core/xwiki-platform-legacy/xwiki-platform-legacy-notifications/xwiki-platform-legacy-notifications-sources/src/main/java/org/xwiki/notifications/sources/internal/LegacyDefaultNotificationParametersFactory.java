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
package org.xwiki.notifications.sources.internal;

import java.util.HashSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.sources.NotificationParameters;

/**
 * This component aims at producing {@link NotificationParameters} instances based on
 * some given criteria. It has been introduced to help dealing with an huge amount of parameters available in old API
 * such as the notification REST API.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component(roles = DefaultNotificationParametersFactory.class)
@Singleton
public class LegacyDefaultNotificationParametersFactory extends DefaultNotificationParametersFactory
{
    @Inject
    private NotificationConfiguration configuration;

    /**
     * Modify the passed parameters to take into account user preferences.
     * 
     * @param parameters the parameters
     * @throws NotificationException if error happens
     * @since 12.6
     */
    public void useUserPreferences(NotificationParameters parameters) throws NotificationException
    {
        if (parameters.user != null) {
            // Check if we should pre or post filter events
            if (this.configuration.isEventPrefilteringEnabled()) {
                super.useUserPreferences(parameters);
            } else {
                // We only request the filters that performs post-filtering.
                parameters.filters = new HashSet<>(notificationFilterManager.getAllFilters(parameters.user, true,
                    NotificationFilter.FilteringPhase.POST_FILTERING));
                parameters.preferences =
                    notificationPreferenceManager.getPreferences(parameters.user, true, parameters.format);

                parameters.filterPreferences =
                    notificationFilterPreferenceManager.getFilterPreferences(parameters.user);
            }
        }
    }
}
