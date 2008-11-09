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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Skin Extension plugin to use css files from JAR resources.
 * 
 * @version $Id: $
 * @since 1.3
 */
public class CssResourceSkinExtensionPlugin extends SkinExtensionPlugin
{
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
     * @see SkinExtensionPlugin#getLink(String, XWikiContext)
     */
    @Override
    public String getLink(String documentName, XWikiContext context)
    {
        String url = "";
        try {
            // If the current user has access to Main.WebHome, we will use this document in the URL
            // to serve the js resource. This way, the resource can be efficiently cached, since it has a
            // common URL for any page.
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), "Main.WebHome", context)) {
                url = context.getWiki().getURL("Main.WebHome", "ssx", "resource=" + documentName, context);
            }
        } catch (XWikiException e) {
            // do nothing here, we'll fold back just after.
        }
        if (url.equals("")) {
            // If we could not have an URL with Main.WebHome, we use the context document.
            url = context.getDoc().getURL("ssx", "resource=" + documentName, context);
        }
        return "<link rel='stylesheet' type='text/css' href='" + url + "'/>";
    }

    @Override
    public void beginParsing(XWikiContext context)
    {
        super.beginParsing(context);
    }

    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return super.endParsing(content, context);
    }

    /**
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
    }

    /**
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
    }

}
