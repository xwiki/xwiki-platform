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
package com.xpn.xwiki.plugin.activitystream.impl;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityStreamPlugin;

/**
 * Internal helper to get some configuration about the Activity Stream.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component(roles = ActivityStreamConfiguration.class)
@Singleton
public class ActivityStreamConfiguration
{
    @Inject
    private Provider<XWikiContext> contextProvider;

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

        XWikiContext context = contextProvider.get();

        ActivityStreamPlugin plugin =
                (ActivityStreamPlugin) context.getWiki().getPlugin(ActivityStreamPlugin.PLUGIN_NAME, context);
        return Integer.parseInt(plugin.getActivityStreamPreference("uselocalstore", "1", context)) == 1;
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
        XWikiContext context = contextProvider.get();

        if (context.isMainWiki()) {
            // We're in the main database, we don't have to store the data twice.
            return false;
        }

        ActivityStreamPlugin plugin =
                (ActivityStreamPlugin) context.getWiki().getPlugin(ActivityStreamPlugin.PLUGIN_NAME, context);
        return Integer.parseInt(plugin.getActivityStreamPreference("usemainstore", "1", context)) == 1;
    }
}
