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
import org.xwiki.observation.event.Event;

/**
 * The configuration of the default Notification Filter Preference implementation. Note: the configuration options
 * {@link #useLocalStore()} and {@link  #useMainStore()} are not used in a consistent way.
 * {@link DocumentMovedListener#onEvent(Event, Object, Object)} consider that both method can return {@code true} at the
 * same time, meaning that preferences can be stored duplicated in both the main wiki and a local wiki if both options
 * are true. Whereas, {@link NotificationFilterPreferenceStore} only consider {@link #useMainStore()} to decides where
 * to store the preferences.
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
     * Indicates if the filter preference must be stored in the local wiki, by reading the
     * {@code eventstream.usemainstore} and {@code eventstream.uselocalstore} properties. The default value of the
     * properties is {@code true}.
     *
     * @return {@code false} if {@link #useMainStore()} returns {@code true}, and {@code eventstream.uselocalstore} is
     *     {@code false}, {@code true} is returned otherwise
     */
    public boolean useLocalStore()
    {
        if (!useMainStore()) {
            // If the main store is disabled, force local store.
            return true;
        }

        return getProperty("uselocalstore");
    }

    /**
     * Indicates if the filter preference must be stored in the main wiki, by reading the
     * {@code eventstream.usemainstore} property. The default value is {@code true}.
     *
     * @return {@code true} if the filter preferences should be stored in the main wiki, {@code false} otherwise
     */
    public boolean useMainStore()
    {
        return getProperty("usemainstore");
    }

    private boolean getProperty(String name)
    {
        return this.configurationSource.getProperty(PREFERENCE_PREFIX + name, true);
    }
}
