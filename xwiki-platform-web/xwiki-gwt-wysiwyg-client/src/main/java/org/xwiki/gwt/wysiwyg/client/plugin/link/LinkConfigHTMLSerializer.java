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
package org.xwiki.gwt.wysiwyg.client.plugin.link;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable.ConfigHTMLSerializer;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;

/**
 * Serializes a {@link LinkConfig} object to an HTML fragment that can be used to insert a link into the edited
 * document.
 * 
 * @version $Id$
 */
public final class LinkConfigHTMLSerializer implements ConfigHTMLSerializer<LinkConfig>
{
    /**
     * The mapping between link types and CSS class names.
     */
    private static final Map<LinkType, String> CLASS_NAME_MAPPING;

    static {
        CLASS_NAME_MAPPING = new HashMap<LinkType, String>();
        CLASS_NAME_MAPPING.put(LinkType.WIKIPAGE, "wikilink");
        CLASS_NAME_MAPPING.put(LinkType.NEW_WIKIPAGE, "wikicreatelink");
        CLASS_NAME_MAPPING.put(LinkType.EXTERNAL, "wikiexternallink");
        CLASS_NAME_MAPPING.put(LinkType.ATTACHMENT, CLASS_NAME_MAPPING.get(LinkType.EXTERNAL));
        CLASS_NAME_MAPPING.put(LinkType.EMAIL, CLASS_NAME_MAPPING.get(LinkType.EXTERNAL));
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfigHTMLSerializer#serialize(Object)
     */
    public String serialize(LinkConfig config)
    {
        StringBuffer html = new StringBuffer();
        html.append("<!--startwikilink:");
        html.append(config.getReference());
        html.append("--><span");
        html.append(serializeAttribute("class", CLASS_NAME_MAPPING.get(config.getType())));
        html.append("><a");
        html.append(serializeAttribute("href", config.getUrl()));
        html.append(serializeAttribute("title", config.getTooltip()));
        html.append(serializeAttribute("rel", config.isOpenInNewWindow() ? "__blank" : null));
        html.append(">");
        html.append(config.getLabel());
        html.append("</a></span><!--stopwikilink-->");
        return html.toString();
    }

    /**
     * Serializes an HTML attribute.
     * 
     * @param name the name of the attribute
     * @param value the value of the attribute
     * @return the HTML serialization of the specified attribute
     */
    private String serializeAttribute(String name, String value)
    {
        return value != null ? " " + name + "=\"" + value.replace("\"", "&quot;") + '"' : "";
    }
}
