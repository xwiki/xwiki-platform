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
package org.xwiki.gwt.dom.client.internal.mozilla;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Element;

import com.google.gwt.dom.client.Node;

/**
 * Contains methods from {@link DOMUtils} that require a different implementation in Mozilla.
 * 
 * @version $Id$
 */
public class MozillaDOMUtils extends DOMUtils
{
    @Override
    public Node splitHTMLNode(Node parent, Node descendant, int offset)
    {
        // Save the length of the descendant before the split to be able to detect where the split took place.
        int length = getLength(descendant);

        // Split the subtree rooted in the given parent.
        Node nextLevelSibling = super.splitHTMLNode(parent, descendant, offset);

        // See if the split took place.
        if (nextLevelSibling != descendant) {
            if (offset == 0) {
                // The split took place at the beginning of the descendant. Ensure the first subtree is accessible.
                // But first see if the first subtree has any leafs besides the descendant.
                Node child = getChild(parent, descendant);
                if (!isInline(child) && getFirstLeaf(child) == descendant) {
                    Node refNode = getFarthestInlineAncestor(descendant);
                    refNode = refNode == null ? child : refNode.getParentNode();
                    ensureBlockIsEditable((Element) refNode);
                }
            }
            if (offset == length) {
                // The split took place at the end of the descendant. Ensure the second subtree is accessible.
                // But first see if the second subtree has any leafs besides the nextLevelSibling.
                Node child = getChild(parent, nextLevelSibling);
                if (!isInline(child) && getLastLeaf(child) == nextLevelSibling) {
                    Node refNode = getFarthestInlineAncestor(nextLevelSibling);
                    refNode = refNode == null ? child : refNode.getParentNode();
                    ensureBlockIsEditable((Element) refNode);
                }
            }
        }

        return nextLevelSibling;
    }

    @Override
    public void ensureBlockIsEditable(Element block)
    {
        if (block.canHaveChildren()) {
            block.appendChild(block.getOwnerDocument().createBRElement());
        }
    }
}
