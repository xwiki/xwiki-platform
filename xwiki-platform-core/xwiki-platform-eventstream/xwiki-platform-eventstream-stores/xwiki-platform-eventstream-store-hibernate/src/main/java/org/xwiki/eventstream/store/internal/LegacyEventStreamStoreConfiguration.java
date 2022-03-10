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
package org.xwiki.eventstream.store.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Internal helper to get some configuration about the Legacy Event Store.
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component(roles = LegacyEventStreamStoreConfiguration.class)
@Singleton
public class LegacyEventStreamStoreConfiguration
{
    private static final String LEGACY_PREFERENCE_PREFIX = "xwiki.plugin.activitystream.";

    private static final String PREFERENCE_PREFIX = "eventstream.";

    private static final String DAYS_TO_KEEP_EVENTS = "daystokeepevents";

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource legacyConfigurationSource;

    @Inject
    private ConfigurationSource configurationSource;

    /**
     * This method determine if events must be store in the local wiki. If the activitystream is set not to store events
     * in the main wiki, the method will return true. If events are stored in the main wiki, the method retrieves the
     * 'platform.plugin.activitystream.uselocalstore' configuration option. If the option is not found the method
     * returns true (default behavior).
     *
     * @return true if the activity stream is configured to store events in the main wiki, false otherwise
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
     * This method determine if events must be store in the main wiki. If the current wiki is the main wiki, this method
     * returns false, otherwise if retrieves the 'platform.plugin.activitystream.usemainstore' configuration option. If
     * the option is not found the method returns true (default behavior).
     *
     * @return true if the activity stream is configured to store events in the main wiki, false otherwise
     */
    public boolean useMainStore()
    {
        return getProperty("usemainstore", true);
    }

    private boolean getProperty(String name, boolean defaultValue)
    {
        if (configurationSource.containsKey(PREFERENCE_PREFIX + name)) {
            return configurationSource.getProperty(PREFERENCE_PREFIX + name, defaultValue);
        }

        if (legacyConfigurationSource.containsKey(LEGACY_PREFERENCE_PREFIX + name)) {
            return legacyConfigurationSource.getProperty(LEGACY_PREFERENCE_PREFIX + name, defaultValue);
        }

        return defaultValue;
    }

    /**
     * @return the number of days events should be kept (default: infinite duration).
     */
    public int getNumberOfDaysToKeep()
    {
        return configurationSource.getProperty(PREFERENCE_PREFIX + DAYS_TO_KEEP_EVENTS,
                legacyConfigurationSource.getProperty(LEGACY_PREFERENCE_PREFIX + DAYS_TO_KEEP_EVENTS, 0));
    }
}
