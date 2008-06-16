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

package com.xpn.xwiki.plugin.applicationmanager;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Entry point of the Application Manager plugin.
 * 
 * @version $Id: $
 */
public class ApplicationManagerPlugin extends XWikiDefaultPlugin
{
    /**
     * Identifier of Application Manager plugin.
     */
    public static final String PLUGIN_NAME = "applicationmanager";

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * The logging tool.
     */
    protected static final Log LOG = LogFactory.getLog(ApplicationManagerPlugin.class);

    /**
     * Notification rule on document create or modify.
     */
    private DocChangeRule docChangeRule;

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Construction the entry point of the Application Manager plugin.
     * 
     * @param name the identifier of the plugin.
     * @param className the class name of the entry point of the plugin.
     * @param context the XWiki context.
     */
    public ApplicationManagerPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        if (docChangeRule == null) {
            docChangeRule = new DocChangeRule(ApplicationManager.getInstance());
        }

        context.getWiki().getNotificationManager().addGeneralRule(docChangeRule);

        String database = context.getDatabase();
        try {
            XWikiURLFactory urlf =
                context.getWiki().getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);
            context.setDatabase(context.getMainXWiki());
            ApplicationManager.getInstance().updateAllApplicationTranslation(
                ApplicationManagerMessageTool.getDefault(context).get(
                    ApplicationManagerMessageTool.COMMENT_REFRESHALLTRANSLATIONS), context);
        } catch (XWikiException e) {
            LOG.error(ApplicationManagerMessageTool.getDefault(context).get(
                ApplicationManagerMessageTool.LOG_REFRESHALLTRANSLATIONS), e);
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#flushCache(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void flushCache(XWikiContext context)
    {
        context.getWiki().getNotificationManager().removeGeneralRule(docChangeRule);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface,
     *      com.xpn.xwiki.XWikiContext)
     */
    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ApplicationManagerPluginApi((ApplicationManagerPlugin) plugin, context);
    }
}
