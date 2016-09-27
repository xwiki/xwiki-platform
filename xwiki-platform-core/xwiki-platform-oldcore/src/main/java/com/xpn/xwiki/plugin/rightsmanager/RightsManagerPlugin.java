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

package com.xpn.xwiki.plugin.rightsmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.Utils;

/**
 * Entry point of the Rights Manager plugin.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class RightsManagerPlugin extends XWikiDefaultPlugin
{
    /**
     * Identifier of Wiki Manager plugin.
     */
    public static final String PLUGIN_NAME = "rightsmanager";

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(RightsManagerPlugin.class);

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Construct the entry point of the Rights Manager plugin.
     *
     * @param name the identifier of the plugin.
     * @param className the class name of the entry point of the plugin.
     * @param context the XWiki context.
     */
    public RightsManagerPlugin(String name, String className, XWikiContext context)
    {
        super(PLUGIN_NAME, className, context);
    }

    @Override
    public void init(XWikiContext context)
    {
        // Register a listener so that the RightsManager is called when a document with a XWikiUsers object is
        // removed in order to remove that user from its groups.
        // Make sure we allow this plugin to be initialized several times in a row; i.e. don't re-register the
        // listener if it's already registered.
        RightsManagerListener rightsManagerListener = RightsManagerListener.getInstance();
        ObservationManager observationManager = Utils.getComponent(ObservationManager.class);
        if (observationManager.getListener(rightsManagerListener.getName()) == null) {
            observationManager.addListener(rightsManagerListener);
        }
    }

    @Override
    public com.xpn.xwiki.api.Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new RightsManagerPluginApi((RightsManagerPlugin) plugin, context);
    }
}
