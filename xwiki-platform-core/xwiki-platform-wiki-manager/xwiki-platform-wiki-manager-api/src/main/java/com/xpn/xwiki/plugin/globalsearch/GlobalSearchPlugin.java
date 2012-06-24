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
package com.xpn.xwiki.plugin.globalsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;

/**
 * Entry point of the Global Search plugin.
 * 
 * @version $Id$
 */
public class GlobalSearchPlugin extends XWikiDefaultPlugin
{
    /**
     * Identifier of Global Search plugin.
     */
    public static final String PLUGIN_NAME = "globalsearch";

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearchPlugin.class);

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Construct the entry point of the Global Search plugin.
     * 
     * @param name the identifier of the plugin.
     * @param className the class name of the entry point of the plugin.
     * @param context the XWiki context.
     */
    public GlobalSearchPlugin(String name, String className, XWikiContext context)
    {
        super(PLUGIN_NAME, className, context);
    }

    @Override
    public com.xpn.xwiki.api.Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new GlobalSearchPluginApi((GlobalSearchPlugin) plugin, context);
    }
}
