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
 * Base class for all plugin APIs. API Objects are the Java Objects that can be manipulated from Velocity or Groovy
 * scripts in XWiki documents. They wrap around a fully functional and privileged Plugin object, and forward safe calls
 * to that object.
 *
 * @param <T> The type of the internal plugin.
 * @version $Id$
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public class PluginApi<T extends XWikiPluginInterface> extends Api
{
    /**
     * The inner plugin object. API calls are usually forwarded to this object.
     */
    private T plugin;

    /**
     * API constructor. The API must know the plugin object it wraps, and the request context.
     *
     * @param plugin The wrapped plugin object.
     * @param context Context of the request.
     */
    public PluginApi(T plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    /**
     * Return the inner plugin object. This method is only for the plugin API's internal use, and should not be exposed
     * to scripting languages. It is an XWiki practice to expose all the functionality using an API, and allow access to
     * the internal objects only to users with Programming Rights.
     *
     * @return The wrapped plugin object.
     * @since 1.3RC1
     */
    protected T getProtectedPlugin()
    {
        return this.plugin;
    }

    /**
     * Return the inner plugin object, if the user has the required programming rights.
     *
     * @return The wrapped plugin object.
     * @since 1.3RC1
     */
    public T getInternalPlugin()
    {
        if (hasProgrammingRights()) {
            return this.plugin;
        }
        return null;
    }

    /**
     * Set the inner plugin object.
     *
     * @param plugin The wrapped plugin object.
     */
    // TODO: Is this really needed? The inner plugin should not be changed.
    public void setPlugin(T plugin)
    {
        this.plugin = plugin;
    }
}
