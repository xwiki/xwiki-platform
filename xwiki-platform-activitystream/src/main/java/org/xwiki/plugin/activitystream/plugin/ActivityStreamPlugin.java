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
package org.xwiki.plugin.activitystream.plugin;

import org.xwiki.plugin.activitystream.api.ActivityStream;
import org.xwiki.plugin.activitystream.impl.ActivityStreamImpl;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Plug-in for for managing streams of activity events
 * 
 * @see ActivityStream
 */
public class ActivityStreamPlugin extends XWikiDefaultPlugin
{
    /**
     * We should user inversion of control instead
     */
    private ActivityStream activityStream;

    /**
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public ActivityStreamPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        setActivityStream(new ActivityStreamImpl());
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return new String("activitystream");
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiDefaultPlugin#getPluginApi
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ActivityStreamPluginApi((ActivityStreamPlugin) plugin, context);
    }

    /**
     * @return The {@link ActivityStream} component used in behind by this plug-in instance
     */
    public ActivityStream getActivityStream()
    {
        return activityStream;
    }

    /**
     * @param activityStream The {@link ActivityStream} component to be used
     */
    public void setActivityStream(ActivityStream activityStream)
    {
        this.activityStream = activityStream;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiDefaultPlugin#init(XWikiContext)
     */
    public void init(XWikiContext context)
    {
        super.init(context);
        try {
            activityStream.initClasses(context);
        } catch (Exception e) {
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiDefaultPlugin#virtualInit(XWikiContext)
     */
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
        try {
            activityStream.initClasses(context);
        } catch (Exception e) {
        }
    }
}
