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
package com.xpn.xwiki.wysiwyg.client.plugin.link.exec;

import com.google.gwt.dom.client.Node;
import com.xpn.xwiki.wysiwyg.client.dom.DocumentFragment;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig.LinkType;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.InsertHTMLExecutable;

/**
 * Creates a link by inserting the link XHTML.
 * 
 * @version $Id$
 */
public class CreateLinkExecutable extends InsertHTMLExecutable
{
    /**
     * The name of the reference attribute of the anchor.
     */
    private static final String HREF_ATTRIBUTE_NAME = "href";

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        if (!super.isEnabled(rta)) {
            return false;
        }

        // Create link is enabled either for creating a new link or for editing an existing link
        Range range = rta.getDocument().getSelection().getRangeAt(0);
        // Check if we're editing a link
        if (LinkExecutableUtils.getSelectedAnchor(rta) != null) {
            return true;
        }

        // if no anchor on ancestor, test all the nodes touched by the selection to not contain an anchor
        if (domUtils.getFirstDescendant(range.cloneContents(), LinkExecutableUtils.ANCHOR_TAG_NAME) != null) {
            return false;
        }

        // Check if the selection does not contain any block elements
        Node commonAncestor = range.getCommonAncestorContainer();
        if (!domUtils.isInline(commonAncestor)) {
            // The selection may contain a block element, check if it actually does
            Node leaf = domUtils.getFirstLeaf(range);
            Node lastLeaf = domUtils.getLastLeaf(range);
            while (true) {
                if (leaf != null) {
                    // Check if it has any non-inline parents up to the commonAncestor
                    Node parentNode = leaf;
                    while (parentNode != commonAncestor) {
                        if (!domUtils.isInline(parentNode)) {
                            // Found a non-inline parent, return false
                            return false;
                        }
                        parentNode = parentNode.getParentNode();
                    }
                }
                // Go to next leaf, if any are left
                if (leaf == lastLeaf) {
                    break;
                } else {
                    leaf = domUtils.getNextLeaf(leaf);
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        // if the whole selection is inside an anchor, the command is executed
        return LinkExecutableUtils.getSelectedAnchor(rta) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#getParameter(RichTextArea)
     */
    public String getParameter(RichTextArea rta)
    {
        LinkConfig linkConfig = new LinkConfig();
        Element wrappingAnchor = LinkExecutableUtils.getSelectedAnchor(rta);

        if (wrappingAnchor == null) {
            return null;
        }

        // get link metadata
        DocumentFragment linkMetadata = wrappingAnchor.getMetaData();
        if (linkMetadata != null) {
            // process it
            Node startComment = linkMetadata.getChildNodes().getItem(0);
            Element wrappingSpan = (Element) linkMetadata.getChildNodes().getItem(1);
            linkConfig.setType(parseLinkType(wrappingSpan, startComment.getNodeValue().substring(14)));
            if (linkConfig.getType() == LinkType.NEW_PAGE || linkConfig.getType() == LinkType.EXISTING_PAGE) {
                parseLinkReference(startComment.getNodeValue().substring(14), linkConfig);
            }
        } else {
            // it's an external link
            linkConfig.setType(LinkType.EXTERNAL);
        }

        linkConfig.setUrl(wrappingAnchor.getAttribute(HREF_ATTRIBUTE_NAME));
        linkConfig.setLabel(wrappingAnchor.getInnerHTML());
        linkConfig.setLabelText(wrappingAnchor.getInnerText());
        return linkConfig.toJSON();
    }

    /**
     * Parses a link reference and extracts the reference components: the wiki, the space and the page name of the
     * targeted page.
     * 
     * @param reference the reference to parse
     * @param linkConfig the link config in which to store the extracted data
     */
    private void parseLinkReference(String reference, LinkConfig linkConfig)
    {
        int wikiSeparatorIndex = reference.indexOf(':');
        int spaceSeparatorIndex = reference.indexOf('.');
        if (wikiSeparatorIndex != -1) {
            linkConfig.setWiki(reference.substring(0, wikiSeparatorIndex));
        }
        if (spaceSeparatorIndex != -1) {
            linkConfig.setSpace(reference.substring(wikiSeparatorIndex + 1, spaceSeparatorIndex));
        }
        linkConfig.setPage(reference.substring(spaceSeparatorIndex + 1));
    }

    /**
     * Parses a link type from its wrapping span and from its reference.
     * 
     * @param wrappingSpan the link's wrapping span
     * @param reference the link reference
     * @return the link type, as parsed from it's wrapping span and from its reference
     */
    private LinkType parseLinkType(Element wrappingSpan, String reference)
    {
        String wrappingSpanClass = wrappingSpan.getClassName();
        if (wrappingSpanClass.equals("wikilink")) {
            return LinkType.EXISTING_PAGE;
        }
        if (wrappingSpanClass.equals("wikicreatelink")) {
            return LinkType.NEW_PAGE;
        }
        if (wrappingSpanClass.equals("wikiexternallink") && reference.startsWith("mailto")) {
            return LinkType.EMAIL;
        }
        return LinkType.EXTERNAL;
    }
}
