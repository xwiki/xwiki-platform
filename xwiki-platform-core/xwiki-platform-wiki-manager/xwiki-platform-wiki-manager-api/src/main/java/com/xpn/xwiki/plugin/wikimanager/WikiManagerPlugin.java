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
package com.xpn.xwiki.plugin.wikimanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.globalsearch.GlobalSearchPlugin;
import com.xpn.xwiki.plugin.globalsearch.GlobalSearchPluginApi;
import com.xpn.xwiki.XWikiContext;

/**
 * Entry point of the Wiki Manager plugin.
 * 
 * @version $Id$
 */
public class WikiManagerPlugin extends XWikiDefaultPlugin
{
    /**
     * Identifier of Wiki Manager plugin.
     */
    public static final String PLUGIN_NAME = "wikimanager";

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(WikiManagerPlugin.class);

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Tool to be able to make and merge multi wikis search queries.
     */
    private XWikiPluginInterface searchPlugin;

    /**
     * Construct the entry point of the Wiki Manager plugin.
     * 
     * @param name the identifier of the plugin.
     * @param className the class name of the entry point of the plugin.
     * @param context the XWiki context.
     */
    public WikiManagerPlugin(String name, String className, XWikiContext context)
    {
        super(PLUGIN_NAME, className, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        super.init(context);

        searchPlugin = context.getWiki().getPlugin(GlobalSearchPlugin.PLUGIN_NAME, context);
        if (searchPlugin == null) {
            searchPlugin =
                new GlobalSearchPlugin(GlobalSearchPlugin.PLUGIN_NAME, GlobalSearchPlugin.class.getName(), context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface,
     *      com.xpn.xwiki.XWikiContext)
     */
    @Override
    public com.xpn.xwiki.api.Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new WikiManagerPluginApi((WikiManagerPlugin) plugin, context);
    }

    /**
     * @param context the XWiki context.
     * @return the global search plugin api.
     */
    public GlobalSearchPluginApi getGlobalSearchApiPlugin(XWikiContext context)
    {
        return (GlobalSearchPluginApi) searchPlugin.getPluginApi(searchPlugin, context);
    }
}
