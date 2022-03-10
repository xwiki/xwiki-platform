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

import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;

/**
 * Skin Extension plugin that allows pulling javascript files from JAR resources.
 *
 * @version $Id$
 * @since 1.3
 */
public class JsResourceSkinExtensionPlugin extends AbstractResourceSkinExtensionPlugin
{
    /**
     * XWiki plugin constructor.
     *
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String, String, com.xpn.xwiki.XWikiContext)
     */
    public JsResourceSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    @Override
    public String getName()
    {
        return "jsrx";
    }

    @Override
    protected String getAction()
    {
        return "jsx";
    }

    @Override
    protected String generateLink(String url, String resourceName, XWikiContext context)
    {
        StringBuilder result =
            new StringBuilder("<script src='").append(XMLUtils.escapeAttributeValue(url)).append("'");

        if (isDefer(resourceName, context)) {
            result.append(" defer='defer'");
        }

        result.append("></script>\n");
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
}
