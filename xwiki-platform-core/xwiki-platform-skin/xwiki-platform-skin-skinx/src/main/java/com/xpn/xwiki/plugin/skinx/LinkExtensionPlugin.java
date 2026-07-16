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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import com.xpn.xwiki.XWikiContext;

/**
 * Skin eXtension that allows inserting generic links in the {@code <head>} section of the resulting XHTML.
 * Unlike JavaScript or StyleSheet extensions, Link extensions don't pull XDocuments as scripting or styling resources
 * for the current document, but register additional related resources. Examples include:
 * <ul>
 * <li>RSS/Atom feeds</li>
 * <li>navigation links in a paged collection of pages (prev, next, index, glossary...)</li>
 * <li>semantic/metadata links (DCMI, DOAP, FOAF, RDF, OWL...)</li>
 * <li>generic links to other related resources or alternate views for the current document</li>
 * </ul>
 * 
 * @version $Id$
 * @since 1.5
 */
public class LinkExtensionPlugin extends AbstractSkinExtensionPlugin
{
    /**
     * XWiki plugin constructor.
     * 
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public LinkExtensionPlugin(String name, String className, XWikiContext context)
    {
        super("linkx", className, context);
    }

    @Override
    public String getLink(String link, XWikiContext context)
    {
        Map<String, Object> params = getParametersForResource(link, context);
        StringBuilder result = new StringBuilder("<link href=\"" + StringEscapeUtils.escapeXml(link) + "\"");
        for (Entry<String, Object> entry : params.entrySet()) {
            result.append(" ");
            result.append(StringEscapeUtils.escapeXml(entry.getKey()));
            result.append("='");
            result.append(StringEscapeUtils.escapeXml(entry.getValue().toString()));
            result.append("'");
        }
        result.append("/>\n");
        return result.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no support for always used link extensions yet.
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
     * Not supported for link extensions.
     * </p>
     * 
     * @see com.xpn.xwiki.plugin.skinx.AbstractSkinExtensionPlugin#hasPageExtensions(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public boolean hasPageExtensions(XWikiContext context)
    {
        return false;
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
