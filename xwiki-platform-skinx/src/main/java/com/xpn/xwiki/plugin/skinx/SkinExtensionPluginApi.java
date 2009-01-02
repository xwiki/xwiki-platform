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

import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * API for the SkinExtension Plugin.
 * 
 * @see PluginApi
 * @see AbstractSkinExtensionPlugin
 * @version $Id$
 */
public class SkinExtensionPluginApi extends PluginApi<AbstractSkinExtensionPlugin>
{
    /**
     * XWiki Plugin API constructor.
     * 
     * @param plugin The wrapped plugin.
     * @param context The current request context.
     * @see PluginApi#PluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface, XWikiContext)
     */
    public SkinExtensionPluginApi(AbstractSkinExtensionPlugin plugin, XWikiContext context)
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
    public void use(String resource)
    {
        this.getProtectedPlugin().use(resource, getXWikiContext());
    }

    /**
     * Mark a skin extension document as used in the current result, together with some parameters. How the parameters
     * are used, depends on the type of resource being pulled. For example, JS and CSS extensions use the parameters in
     * the resulting URL, while Link extensions use the parameters as attributes of the link tag. A resource is
     * registered only once per request, further calls will not result in additional links, even if it is pulled with
     * different parameters. If more than one calls per request are made, the parameters used are the ones from the last
     * call (or none, if the last call did not specify any parameters).
     * 
     * @param resource The name of the resource to pull.
     * @param parameters The parameters for this resource.
     * @see AbstractSkinExtensionPlugin#use(String, Map, XWikiContext)
     */
    public void use(String resource, Map<String, Object> parameters)
    {
        this.getProtectedPlugin().use(resource, parameters, getXWikiContext());
    }

    /**
     * Composes and returns the links to the resources pulled in the current request. This method is called at the end
     * of each request, once for each type of resource (subclass), and the result is placed in the generated XHTML.
     * 
     * @return a XHMTL fragment with all extensions imports statements for this request. This includes both extensions
     *         that are defined as being "used always" and "on demand" extensions explicitly requested for this page.
     *         Always used extensions are always, before on demand extensions, so that on demand extensions can override
     *         more general elements in the always used ones.
     */
    public String getImportString()
    {
        return this.getProtectedPlugin().getImportString(getXWikiContext());
    }
}
