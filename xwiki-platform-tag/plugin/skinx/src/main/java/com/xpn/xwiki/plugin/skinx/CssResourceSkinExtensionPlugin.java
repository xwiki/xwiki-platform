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
 */
package com.xpn.xwiki.plugin.skinx;

import java.util.Collections;
import java.util.Set;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Skin Extension plugin to use css files from JAR resources.
 * 
 * @version $Id$
 * @since 1.3
 */
public class CssResourceSkinExtensionPlugin extends AbstractSkinExtensionPlugin
{
    /**
     * XWiki plugin constructor.
     * 
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public CssResourceSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    @Override
    public String getName()
    {
        return "ssrx";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSkinExtensionPlugin#getLink(String, XWikiContext)
     */
    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        String result = "";
        // If the current user has access to Main.WebHome, we will use this document in the URL
        // to serve the css resource. This way, the resource can be efficiently cached, since it has a
        // common URL for any page.
        try {
            String page = context.getWiki().getDefaultWeb(context) + "." + context.getWiki().getDefaultPage(context);
            if (!context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), page, context)) {
                page = context.getDoc().getFullName();
            }
            String url =
                context.getWiki().getURL(page, "ssx",
                    "resource=" + documentName + parametersAsQueryString(documentName, context), context);
            result = "<link rel='stylesheet' type='text/css' href='" + url + "'/>";
        } catch (XWikiException e) {
            // Do nothing here; we can't access the wiki, so don't link to this resource at all.
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no support for always used resource-based extensions yet.
     * </p>
     * 
     * @see AbstractSkinExtensionPlugin#getAlwaysUsedExtensions(XWikiContext)
     */
    @Override
    public Set<String> getAlwaysUsedExtensions(XWikiContext context)
    {
        // There is no mean to define an always used extension for something else than a document extension now,
        // so for resources-based extensions, we return an emtpy set.
        // An idea for the future could be to have an API for plugins and components to register always used resources
        // extensions.
        return Collections.emptySet();
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
}
