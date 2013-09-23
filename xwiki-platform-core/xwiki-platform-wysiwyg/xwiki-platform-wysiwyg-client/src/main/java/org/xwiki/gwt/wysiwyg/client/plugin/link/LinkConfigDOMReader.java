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

import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.EscapeUtils;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.AbstractInsertElementExecutable.ConfigDOMReader;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigDOMReader;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.ImageElement;

/**
 * Creates an {@link LinkConfig} object from an {@link AnchorElement}.
 * 
 * @version $Id$
 */
public class LinkConfigDOMReader implements ConfigDOMReader<LinkConfig, AnchorElement>
{
    /**
     * The string that separates link reference components.
     */
    private static final String LINK_REFERENCE_COMPONENT_SEPARATOR = "|-|";

    /**
     * The object used to extract the image file name when the anchor wraps an image.
     */
    private final ImageConfigDOMReader imageConfigHTMLParser = new ImageConfigDOMReader();

    /**
     * {@inheritDoc}
     * 
     * @see ConfigDOMReader#read(com.google.gwt.dom.client.Element)
     */
    public LinkConfig read(AnchorElement anchor)
    {
        LinkConfig linkConfig = new LinkConfig();

        // Get link meta data.
        DocumentFragment linkMetadata = Element.as(anchor).getMetaData();
        if (linkMetadata != null) {
            // Process the meta data.
            String startComment = linkMetadata.getChildNodes().getItem(0).getNodeValue();
            Element wrappingSpan = (Element) linkMetadata.getChildNodes().getItem(1);
            linkConfig.setReference(EscapeUtils.unescapeBackslash(startComment.substring("startwikilink:".length())));
            linkConfig.setType(readLinkType(wrappingSpan, linkConfig.getReference()));
        } else {
            // It's an external link.
            linkConfig.setType(LinkType.EXTERNAL);
        }

        // NOTE: We use getAttribute and not getHref to access the URL because we want the exact value of the attribute
        // and not the resolved (absolute) URL.
        linkConfig.setUrl(anchor.getAttribute("href"));
        linkConfig.setLabel(anchor.getInnerHTML());
        if (anchor.getChildNodes().getLength() == 1 && "img".equalsIgnoreCase(anchor.getFirstChild().getNodeName())) {
            // The anchor wraps an image.
            ImageElement image = (ImageElement) anchor.getFirstChild();
            linkConfig.setLabelText(imageConfigHTMLParser.read(image).getReference());
            linkConfig.setReadOnlyLabel(true);
        } else {
            linkConfig.setLabelText(anchor.getInnerText());
        }
        linkConfig.setTooltip(anchor.getTitle());
        linkConfig.setOpenInNewWindow("__blank".equals(anchor.getRel()));

        return linkConfig;
    }

    /**
     * Parses a link type from its wrapping span and from its reference.
     * 
     * @param wrappingSpan the link's wrapping span
     * @param reference the link reference
     * @return the link type, as parsed from it's wrapping span and from its reference
     */
    private LinkType readLinkType(Element wrappingSpan, String reference)
    {
        LinkType linkType = LinkType.getByClassName(wrappingSpan.getClassName());
        if (linkType == null) {
            // Default to external link.
            linkType = LinkType.EXTERNAL;
        } else if (linkType == LinkType.EXTERNAL && "mailto".equalsIgnoreCase(getLinkProtocol(reference))) {
            // Email links don't user a dedicated CSS class name.
            linkType = LinkType.EMAIL;
        }
        return linkType;
    }

    /**
     * Extracts the link protocol from a link reference.
     * <p>
     * NOTE: Ideally the client shouldn't know the syntax/format of the link reference. The link reference should be
     * parsed on the server side. We need to extract the link protocol to be able to determine the link type which is
     * then used to determine what link wizard to open when editing a link. Making an asynchronous request to parse the
     * link reference to get only the link type/protocol before any link wizard is opened is a bit difficult with the
     * current design. This needs to be fixed nevertheless. Until then we make the assumption that the link protocol is
     * between the first and second occurrence of the {@link #LINK_REFERENCE_COMPONENT_SEPARATOR}.
     * 
     * @param linkReference a link reference, taken from a link XHTML marker / annotation / meta data.
     * @return the link protocol extracted from the given link reference
     */
    private String getLinkProtocol(String linkReference)
    {
        int beginIndex = linkReference.indexOf(LINK_REFERENCE_COMPONENT_SEPARATOR);
        if (beginIndex < 0) {
            return null;
        }
        beginIndex += LINK_REFERENCE_COMPONENT_SEPARATOR.length();
        int endIndex = linkReference.indexOf(LINK_REFERENCE_COMPONENT_SEPARATOR, beginIndex);
        if (endIndex < 0) {
            return null;
        }
        return linkReference.substring(beginIndex, endIndex);
    }
}
