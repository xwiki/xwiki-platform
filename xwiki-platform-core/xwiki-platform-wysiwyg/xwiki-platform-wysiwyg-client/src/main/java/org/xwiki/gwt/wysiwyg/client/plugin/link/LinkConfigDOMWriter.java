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

import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.EscapeUtils;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable.ConfigDOMWriter;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;

import com.google.gwt.dom.client.AnchorElement;

/**
 * Serializes a {@link LinkConfig} object to an HTML fragment that can be used to insert a link into the edited
 * document.
 * 
 * @version $Id$
 */
public final class LinkConfigDOMWriter implements ConfigDOMWriter<LinkConfig, AnchorElement>
{
    /**
     * The value of the link target attribute which indicates that the link should be opened in a new window.
     */
    private static final String TARGET_BLANK = "__blank";

    /**
     * {@inheritDoc}
     * 
     * @see ConfigDOMWriter#write(Object, com.google.gwt.dom.client.Element)
     */
    public void write(LinkConfig config, AnchorElement anchor)
    {
        // Required attributes.
        updateMetaData(anchor, config.getReference(), config.getType());
        anchor.setHref(config.getUrl());
        // Optional attributes.
        updateAttribute(anchor, "title", config.getTooltip());
        if (config.isOpenInNewWindow()) {
            anchor.setRel(TARGET_BLANK);
        } else if (TARGET_BLANK.equalsIgnoreCase(anchor.getRel())) {
            anchor.removeAttribute("rel");
        }
        // Update the content.
        if (!anchor.getInnerHTML().equals(config.getLabel())) {
            // Inner HTML listeners have to be notified in order to extract the meta data.
            Element.as(anchor).xSetInnerHTML(config.getLabel());
        }
    }

    /**
     * Updates the meta data of the given anchor.
     * 
     * @param anchor the anchor whose meta data will be updated
     * @param reference the new link reference
     * @param linkType the new link type
     */
    private void updateMetaData(AnchorElement anchor, String reference, LinkType linkType)
    {
        Document document = (Document) anchor.getOwnerDocument();
        DocumentFragment metaData = document.createDocumentFragment();
        metaData.appendChild(document.createComment("startwikilink:" + EscapeUtils.escapeComment(reference)));
        metaData.appendChild(document.createSpanElement());
        Element.as(metaData.getChild(1)).setClassName(linkType.getClassName());
        metaData.getChild(1).appendChild(document.createTextNode(Element.INNER_HTML_PLACEHOLDER));
        metaData.appendChild(document.createComment("stopwikilink"));
        Element.as(anchor).setMetaData(metaData);
    }

    /**
     * Updates the specified attribute of the given anchor.
     * 
     * @param anchor the anchor whose attribute will be updated
     * @param name the attribute name
     * @param value the new attribute value, {@code null} to remove the attribute
     */
    private void updateAttribute(AnchorElement anchor, String name, String value)
    {
        if (StringUtils.isEmpty(value)) {
            anchor.removeAttribute(name);
        } else {
            anchor.setAttribute(name, value);
        }
    }
}
