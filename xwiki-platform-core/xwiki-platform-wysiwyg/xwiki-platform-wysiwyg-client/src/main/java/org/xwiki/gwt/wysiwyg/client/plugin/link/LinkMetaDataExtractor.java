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

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.InnerHTMLListener;
import org.xwiki.gwt.dom.client.Text;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig.LinkType;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;

/**
 * Analyzes the passed subtree and detects the link structures and transforms them in minimal HTML elements with meta
 * fragments attached, so that the editor operates with minimal HTML.
 * 
 * @version $Id$
 */
public class LinkMetaDataExtractor implements InnerHTMLListener
{
    /**
     * {@inheritDoc}
     * 
     * @see InnerHTMLListener#onInnerHTMLChange(Element)
     */
    public void onInnerHTMLChange(Element parent)
    {
        // look up all images in this subtree
        NodeList<com.google.gwt.dom.client.Element> anchors = parent.getElementsByTagName("a");
        for (int i = 0; i < anchors.getLength(); i++) {
            Element anchor = (Element) anchors.getItem(i);
            processElement(anchor);
        }
    }

    /**
     * Processes the passed anchor looking for the wrapping span and neighbor comments to encapsulate it all in a meta
     * fragment and leave only the anchor in the tree.
     * 
     * @param anchor the anchor element found
     */
    private void processElement(Element anchor)
    {
        // Search for parent span and surrounding comments.
        Element parentNode = anchor.getParentElement().cast();
        if (parentNode == null || !"span".equalsIgnoreCase(parentNode.getNodeName())
            || LinkType.getByClassName(parentNode.getClassName()) == null || !hasLinkMarkers(parentNode)) {
            return;
        }

        DocumentFragment metaFragment = ((Document) anchor.getOwnerDocument()).createDocumentFragment();
        // Move the link markers.
        metaFragment.appendChild(parentNode.getPreviousSibling());
        metaFragment.appendChild(parentNode.getNextSibling());
        // Create the place-holder and replace the anchor.
        Text placeholder = (Text) ((Document) anchor.getOwnerDocument()).createTextNode(Element.INNER_HTML_PLACEHOLDER);
        parentNode.replaceChild(placeholder, anchor);
        // Replace the parent node with the anchor.
        parentNode.getParentElement().replaceChild(anchor, parentNode);
        // Put the parent node in the meta fragment.
        metaFragment.insertAfter(parentNode, metaFragment.getFirstChild());
        anchor.setMetaData(metaFragment);
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is wrapped by the link marker comments, {@code false} otherwise
     */
    private boolean hasLinkMarkers(Node node)
    {
        return hasLinkStartMarker(node) && hasLinkStopMarker(node);
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is preceded by the link start marker, {@code false} otherwise
     */
    private boolean hasLinkStartMarker(Node node)
    {
        Node previousSibling = node.getPreviousSibling();
        return previousSibling != null && previousSibling.getNodeType() == DOMUtils.COMMENT_NODE
            && previousSibling.getNodeValue().startsWith("startwikilink");
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is followed by the link stop marker, {@code false} otherwise
     */
    private boolean hasLinkStopMarker(Node node)
    {
        Node nextSibling = node.getNextSibling();
        return nextSibling != null && nextSibling.getNodeType() == DOMUtils.COMMENT_NODE
            && nextSibling.getNodeValue().startsWith("stopwikilink");
    }
}
