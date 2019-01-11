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

package com.xpn.xwiki.plugin.skinx;

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;

/**
 * API for the SkinExtension Plugin.
 * 
 * @see com.xpn.xwiki.plugin.PluginApi
 * @see SkinExtensionPluginApi
 * @version $Id$
 */
public class SkinFileExtensionPluginApi extends SkinExtensionPluginApi
{
    /**
     * XWiki Plugin API constructor.
     * 
     * @param plugin The wrapped plugin.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.PluginApi#PluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface, XWikiContext)
     */
    public SkinFileExtensionPluginApi(AbstractSkinExtensionPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Mark a resource as used in the current result. A resource is registered only once per request, further calls will
     * not result in additional links, even if it is pulled with different parameters.
     * 
     * @param resource The name of the resource to pull.
     * @see AbstractSkinExtensionPlugin#use(String, XWikiContext)
     */
    @Override
    public void use(String resource)
    {
        this.use(resource, false);
    }

    /**
     * Mark a resource as used in the current result. A resource is registered only once per request, further calls will
     * not result in additional links, even if it is pulled with different parameters.
     * 
     * @param resource The name of the resource to pull.
     * @param forceSkinAction True if the resource should be pulled by the 'skin' action.
     * @see AbstractSkinExtensionPlugin#use(String, XWikiContext)
     */
    public void use(String resource, boolean forceSkinAction)
    {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("forceSkinAction", forceSkinAction);
        this.getProtectedPlugin().use(resource, parameters, getXWikiContext());
    }
}
