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

import java.util.Collections;
import java.util.Set;

import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Javascript Skin File Extension plugin to use js files from the skin.
 * 
 * @version $Id$
 * @since 1.6
 */
public class JsSkinFileExtensionPlugin extends AbstractSkinExtensionPlugin
{
    /**
     * The identifier for this plugin; used for accessing the plugin from velocity, and as the action returning the
     * extension content.
     */
    public static final String PLUGIN_NAME = "jsfx";

    /**
     * XWiki plugin constructor.
     * 
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public JsSkinFileExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SkinFileExtensionPluginApi((AbstractSkinExtensionPlugin) plugin, context);
    }

    @Override
    public String getLink(String filename, XWikiContext context)
    {
        StringBuilder result = new StringBuilder("<script src='");
        result.append(XMLUtils.escapeAttributeValue(getSkinFileURL(filename, context)));

        if (isDefer(filename, context)) {
            result.append("' defer='defer");
        }

        result.append("'></script>\n");
        return result.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * We must override this method since the plugin manager only calls it for classes that provide their own
     * implementation, and not an inherited one.
     * </p>
     * 
     * @see AbstractSkinExtensionPlugin#endParsing(String, XWikiContext)
     */
    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return super.endParsing(content, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no support for always used skinfile-based extensions.
     * </p>
     * 
     * @see AbstractSkinExtensionPlugin#getAlwaysUsedExtensions(XWikiContext)
     */
    @Override
    public Set<String> getAlwaysUsedExtensions(XWikiContext context)
    {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not supported for skinfile-based extensions.
     * </p>
     * 
     * @see com.xpn.xwiki.plugin.skinx.AbstractSkinExtensionPlugin#hasPageExtensions(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public boolean hasPageExtensions(XWikiContext context)
    {
        return false;
    }
}
