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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * The configuration of the default Notification Filter Preference implementation.
 * 
 * @version $Id$
 * @since 12.6
 */
@Component(roles = NotificationFilterPreferenceConfiguration.class)
@Singleton
public class NotificationFilterPreferenceConfiguration
{
    private static final String PREFERENCE_PREFIX = "eventstream.";

    @Inject
    private ConfigurationSource configurationSource;

    /**
     * @return true if the filter preferences should must be stored in the main wiki, false otherwise
     */
    public boolean useLocalStore()
    {
        if (!useMainStore()) {
            // If the main store is disabled, force local store.
            return true;
        }

        return getProperty("uselocalstore", true);
    }

    /**
     * This method determine if filter preferences must be stored in the main wiki. If the current wiki is the main
     * wiki, this method returns false, otherwise if retrieves the configuration option. If the option is not found the
     * method returns true (default behavior).
     *
     * @return true if the filter preferences should be stored in the main wiki, false otherwise
     */
    public boolean useMainStore()
    {
        return getProperty("usemainstore", true);
    }

    private boolean getProperty(String name, boolean defaultValue)
    {
        return this.configurationSource.getProperty(PREFERENCE_PREFIX + name, defaultValue);
    }
}
