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

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

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

        // TODO: introduce new properties in xwiki.properties and deprecated the old ones
        return configurationSource.getProperty(LEGACY_PREFERENCE_PREFIX + "uselocalstore", 1).equals(1);
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
        if (wikiDescriptorManager.isMainWiki(wikiDescriptorManager.getCurrentWikiId())) {
            // We're in the main database, we don't have to store the data twice.
            return false;
        }

        // TODO: introduce new properties in xwiki.properties and deprecated the old ones
        return configurationSource.getProperty(LEGACY_PREFERENCE_PREFIX + "usemainstore", 1).equals(1);
    }
}
