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
 *
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.alexa;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Plugin allowing to query Alexa services.
 * 
 * @version $Id$
 * @deprecated the plugin technology is deprecated
 */
@Deprecated
public class AlexaPlugin extends XWikiDefaultPlugin
{
    /**
     * The mandatory plugin constructor, this is the method called (through reflection) by the plugin manager.
     * 
     * @param name the plugin name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
    public AlexaPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#getName()
     */
    @Override
    public String getName()
    {
        return "alexa";
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#getPluginApi(XWikiPluginInterface, XWikiContext)
     */
    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new AlexaPluginApi((AlexaPlugin) plugin, context);
    }
}
