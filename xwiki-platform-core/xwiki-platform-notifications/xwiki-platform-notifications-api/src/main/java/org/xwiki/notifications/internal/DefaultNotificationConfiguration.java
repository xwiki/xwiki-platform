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
package org.xwiki.notifications.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.notifications.NotificationConfiguration;

/**
 * Default implementation for (@link {@link org.xwiki.notifications.NotificationConfiguration}.
 * 
 * @version $Id$
 * @since 9.4RC1
 */
@Component
@Singleton
public class DefaultNotificationConfiguration implements NotificationConfiguration
{
    private static final String CONFIGURATION_PREFIX = "notifications.";

    @Inject
    private ConfigurationSource configurationSource;

    @Override
    public boolean isEnabled()
    {
        return configurationSource.getProperty(CONFIGURATION_PREFIX + "enabled", true);
    }

    @Override
    public boolean areEmailsEnabled()
    {
        return configurationSource.getProperty(CONFIGURATION_PREFIX + "emails.enabled", true);
    }

    @Override
    public int liveNotificationsGraceTime()
    {
        int graceTime = configurationSource.getProperty(CONFIGURATION_PREFIX + "emails.live.graceTime", 10);

        return (graceTime < 0) ? 0 : graceTime;
    }

    @Override
    public boolean isEventPreFilteringEnabled()
    {
        return configurationSource.getProperty(CONFIGURATION_PREFIX + "eventPreFilteringEnabled", false);
    }
}
