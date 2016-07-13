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
package org.xwiki.gwt.wysiwyg.client.plugin.image.internal;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfigDOMWriter;

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Node;

/**
 * Overwrites {@link ImageConfigDOMWriter} in order to fix some IE bugs.
 * 
 * @version $Id$
 */
public class IEImageConfigDOMWriter extends ImageConfigDOMWriter
{
    @Override
    public void write(ImageConfig imageConfig, ImageElement image)
    {
        super.write(imageConfig, image);

        // Add a non-breaking space after the image if the image is the last visible node inside its closest block-level
        // ancestor. Without this space the user can't move the caret after the image to add new content inside the
        // current block-level container.
        // FIXME: This isn't the right place for this code because this method should affect only the given image.
        // Ensure that the image is attached, otherwise we can't add the space.
        if (image.getParentNode() != null && endsBlock(image)) {
            DOMUtils.getInstance().insertAfter(image.getOwnerDocument().createTextNode("\u00A0"), image);
        }
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the given node is not followed by any visible node inside the same block-level container,
     *         {@code false} otherwise
     */
    private boolean endsBlock(Node node)
    {
        Node ancestor = node;
        // Look for an ancestor that is followed by a visible sibling. Stop when a block-level ancestor is found.
        do {
            if (getNextVisibleSibling(ancestor) != null) {
                return false;
            }
            ancestor = ancestor.getParentNode();
        } while (ancestor != null && DOMUtils.getInstance().isInline(ancestor));
        return true;
    }

    /**
     * @param node a DOM node
     * @return the next visible sibling of the given node
     * @see #isVisible(Node)
     */
    private Node getNextVisibleSibling(Node node)
    {
        Node sibling = node;
        do {
            sibling = sibling.getNextSibling();
        } while (sibling != null && !isVisible(sibling));
        return sibling;
    }

    /**
     * @param node a DOM node
     * @return {@code true} if the give node is visible, {@code false} otherwise
     */
    private boolean isVisible(Node node)
    {
        // We treat BR differently than other elements because it is visible even though it has 0 offset width.
        return (node.getNodeType() == Node.ELEMENT_NODE && ("br".equalsIgnoreCase(node.getNodeName()) || Element.as(
            node).getOffsetWidth() > 0))
            || !StringUtils.isEmpty(node.getNodeValue());
    }
}
