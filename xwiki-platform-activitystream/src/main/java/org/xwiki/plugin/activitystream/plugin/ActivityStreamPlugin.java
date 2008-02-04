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

import org.xwiki.plugin.activitystream.impl.ActivityStreamImpl;
import org.xwiki.plugin.activitystream.api.ActivityStream;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Plug-in for for managing invitations and membership requests
 * 
 * @see org.xwiki.plugin.invitationmanager.api.InvitationManager
 */
public class ActivityStreamPlugin extends XWikiDefaultPlugin
{
    /**
     * We should user inversion of control instead
     */
    private ActivityStream invitationManager = new ActivityStreamImpl();

    /**
     * {@inheritDoc}
     * 
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public ActivityStreamPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);

        // move this to ActivityStreamImpl in the near future
        String mailNotificationCfg =
            context.getWiki().Param("xwiki.activitystream.mailnotification", "1").trim();
        boolean mailNotification = "1".equals(mailNotificationCfg);
        // ((ActivityStreamImpl) invitationManager).setMailNotification(mailNotification);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return new String("activitystream");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ActivityStreamPluginApi((ActivityStreamPlugin) plugin, context);
    }

    public ActivityStream getActivityStream()
    {
        return invitationManager;
    }

    public void setActivityStream(ActivityStream invitationManager)
    {
        this.invitationManager = invitationManager;
    }


    public void init(XWikiContext context) {
        super.init(context);
        try {
            invitationManager.initClasses(context);
        } catch (Exception e) {
        }
    }

    public void virtualInit(XWikiContext context) {
        super.virtualInit(context);
        try {
            invitationManager.initClasses(context);
        } catch (Exception e) {
        }
    }
}
