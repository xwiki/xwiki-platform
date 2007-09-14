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

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApplicationManagerPlugin extends XWikiDefaultPlugin
{
    protected static final Log LOG = LogFactory.getLog(ApplicationManagerPlugin.class);

    // ////////////////////////////////////////////////////////////////////////////

    public static final String PLUGIN_NAME = "applicationmanager";

    // ////////////////////////////////////////////////////////////////////////////

    public ApplicationManagerPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface,
     *      com.xpn.xwiki.XWikiContext)
     */
    public com.xpn.xwiki.api.Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ApplicationManagerPluginApi((ApplicationManagerPlugin) plugin, context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    public void init(XWikiContext context)
    {
        super.init(context);
    }
}
