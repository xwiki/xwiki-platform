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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import java.util.Map;

import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Generates html link blocks for all types of links.
 * 
 * @version $Id$
 */
public final class LinkHTMLGenerator
{
    /**
     * The singleton instance of this class.
     */
    private static LinkHTMLGenerator instance;

    /**
     * Class constructor, private so that the class is a singleton.
     */
    private LinkHTMLGenerator()
    {
    }

    /**
     * @return the instance of this class.
     */
    public static synchronized LinkHTMLGenerator getInstance()
    {
        if (instance == null) {
            instance = new LinkHTMLGenerator();
        }
        return instance;
    }

    /**
     * Generates the link HTML block corresponding to the specified link configuration data.
     * 
     * @param config the link configuration data
     * @return the String representing the link HTML block for the passed configuration data
     */
    public String getLinkHTML(LinkConfig config)
    {
        StringBuffer html = new StringBuffer("");
        String spanClass = "";
        switch (config.getType()) {
            case WIKIPAGE:
                spanClass = "wikilink";
                break;
            case NEW_WIKIPAGE:
                spanClass = "wikicreatelink";
                break;
            case ATTACHMENT:
            case EMAIL:
            case EXTERNAL:
            default:
                spanClass = "wikiexternallink";
        }
        String quoteString = "\"";
        html.append("<!--startwikilink:");
        html.append(config.getReference());
        html.append("--><span class=\"");
        html.append(spanClass);
        html.append("\"><a href=\"");
        html.append(config.getUrl());
        html.append(quoteString);
        // serialize the config parameters
        for (Map.Entry<String, String> param : config.listParameters()) {
            if (!StringUtils.isEmpty(param.getValue())) {
                html.append(" ");
                html.append(param.getKey());
                html.append("=\"");
                html.append(param.getValue().replace(quoteString, "&quot;"));
                html.append(quoteString);
            }
        }
        html.append(">");
        html.append(config.getLabel());
        html.append("</a></span><!--stopwikilink-->");
        return html.toString();
    }
}
