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
package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

/**
 * Base class for all plugin APIs. API Objects are the Java Objects that can be manipulated from
 * Velocity or Groovy scripts in XWiki documents. They wrap around a fully functional Plugin object,
 * and forward safe calls to that object.
 * 
 * @version $Id: $
 */
public class PluginApi extends Api
{
    /**
     * The inner plugin object. API calls are usually forwarded to this object.
     */
    private XWikiPluginInterface plugin;

    /**
     * API constructor. The API must know the plugin object it wraps, and the request context.
     * 
     * @param plugin The wrapped plugin object.
     * @param context Context of the request.
     */
    public PluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    /**
     * Return the inner plugin object, if the user has the required programming rights.
     * 
     * @return The wrapped plugin object.
     * @todo Why is this public and doesn't require programming rights?
     */
    public XWikiPluginInterface getPlugin()
    {
        return plugin;
    }

    /**
     * Set the inner plugin object.
     * 
     * @param plugin The wrapped plugin object.
     * @todo Is this really needed? The inner plugin should not be changed.
     */
    public void setPlugin(XWikiPluginInterface plugin)
    {
        this.plugin = plugin;
    }
}
