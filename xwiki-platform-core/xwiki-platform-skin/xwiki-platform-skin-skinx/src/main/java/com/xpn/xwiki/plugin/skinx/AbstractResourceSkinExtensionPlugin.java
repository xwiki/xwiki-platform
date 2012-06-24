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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Skin Extension plugin to use extension files from JAR resources.
 * 
 * @version $Id$
 */
public abstract class AbstractResourceSkinExtensionPlugin extends AbstractSkinExtensionPlugin
{
    /**
     * XWiki plugin constructor.
     * 
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public AbstractResourceSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    /**
     * Get the action which the url should specify for calling this resource.
     * 
     * @return String Action name.
     */
    protected abstract String getAction();

    /**
     * Takes a URL string and outputs a link which will cause the browser to load the url.
     * 
     * @param url String representation of the url to load (eg: {@code /res/url.js})
     * @param resourceName name of the pulled resource
     * @param context the current request context
     * @return HTML code linking to the pulled resource (eg: {@code <script type="text/javascript" src="/res/url.js"/>})
     */
    protected abstract String generateLink(String url, String resourceName, XWikiContext context);

    @Override
    public String getLink(String resourceName, XWikiContext context)
    {
        // If the current user has access to Main.WebHome, we will use this document in the URL
        // to serve the resource. This way, the resource can be efficiently cached, since it has a
        // common URL for any page.
        try {
            String page = context.getWiki().getDefaultSpace(context) + "." + context.getWiki().getDefaultPage(context);
            if (!context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), page, context)) {
                page = context.getDoc().getFullName();
            }
            return generateLink(context.getWiki().getURL(page, getAction(),
                "resource=" + sanitize(resourceName) + parametersAsQueryString(resourceName, context), context),
                resourceName, context);
        } catch (XWikiException e) {
            // Do nothing here; we can't access the wiki, so don't link to this resource at all.
            return "";
        }
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
     * Not supported for resource extensions.
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
