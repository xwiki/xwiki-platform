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
package com.xpn.xwiki.plugin.activitystream.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStream;
import com.xpn.xwiki.plugin.activitystream.impl.ActivityStreamImpl;

/**
 * Plug-in for for managing streams of activity events.
 * 
 * @see ActivityStream
 * @version $Id$
 */
public class ActivityStreamPlugin extends XWikiDefaultPlugin
{
    /**
     * Name of the plugin.
     */
    public static final String PLUGIN_NAME = "activitystream";

    /**
     * We should user inversion of control instead.
     */
    private ActivityStream activityStream;

    /**
     * Constructor.
     * 
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     * @param name name of the plugin
     * @param className class name of the plugin
     * @param context the XWiki context
     */
    public ActivityStreamPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        setActivityStream(new ActivityStreamImpl());
    }

    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ActivityStreamPluginApi((ActivityStreamPlugin) plugin, context);
    }

    /**
     * @return The {@link ActivityStream} component used in behind by this plug-in instance
     */
    public ActivityStream getActivityStream()
    {
        return this.activityStream;
    }

    /**
     * @param activityStream The {@link ActivityStream} component to be used
     */
    public void setActivityStream(ActivityStream activityStream)
    {
        this.activityStream = activityStream;
    }

    /**
     * Get a preference for the activitystream from the XWiki configuration.
     * 
     * @param preference Name of the preference to get the value from
     * @param defaultValue Default value if the preference is not found in the configuration
     * @param context the XWiki context
     * @return value for the given preference
     */
    public String getActivityStreamPreference(String preference, String defaultValue, XWikiContext context)
    {
        String preferencePrefix = "xwiki.plugin.activitystream.";
        String prefName = preferencePrefix + preference;
        return context.getWiki().getXWikiPreference(prefName, prefName, defaultValue, context);
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
        try {
            this.activityStream.init(context);
        } catch (Exception e) {
            // Do nothing.
        }
    }

    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
    }
}
