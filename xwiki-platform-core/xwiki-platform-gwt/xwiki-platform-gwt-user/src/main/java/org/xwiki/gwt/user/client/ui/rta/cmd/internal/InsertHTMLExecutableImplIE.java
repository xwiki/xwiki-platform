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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;

/**
 * Overwrites {@link InsertHTMLExecutableImpl} with a custom implementation for Internet Explorer in order to ensure the
 * caret remains in the right position after the selected content is deleted. IE sometimes moves the caret inside a
 * sibling node when an entire node is deleted, instead of keeping the caret been the nodes.
 * 
 * @version $Id$
 */
public class InsertHTMLExecutableImplIE extends InsertHTMLExecutableImpl
{
    /**
     * {@inheritDoc}
     * <p>
     * Ensures the caret remains in the right position after the selected content is deleted.
     * 
     * @see InsertHTMLExecutable#deleteSelection(RichTextArea)
     */
    @Override
    protected Range deleteSelection(RichTextArea rta)
    {
        // If the start container is badly detected then this method might fail to adjust the caret position.
        Node node = rta.getDocument().getSelection().getRangeAt(0).getStartContainer();
        // Save the list of ancestors and their child nodes before deleting the selected content in order to be able to
        // detect which is the first deleted node. The first deleted node is not always the node where the selection
        // starts. In some cases (e.g. the entire content of an element is selected) Internet Explorer deletes more than
        // what is selected. Internet Explorer also has the habit of deleting sibling elements that are not visible and
        // normalizing sibling text nodes.
        List<Node> ancestors = new ArrayList<Node>();
        // Here we don't use a map because a runtime exception is thrown when the key is a node (JavaScript object).
        List<List<Node>> children = new ArrayList<List<Node>>();
        while (node != null) {
            ancestors.add(node);
            children.add(getChildren(node));
            node = node.getParentNode();
        }
        // Delete the selected content.
        Range range = super.deleteSelection(rta);
        // Look for the first ancestor that hasn't been affected by the delete command.
        int containerIndex = getFirstAttachedAncestorIndex(ancestors);
        // Look for the first child node that has been deleted.
        int offset = getFirstDeletedChildIndex(ancestors.get(containerIndex), children.get(containerIndex));
        if (offset >= 0) {
            range = rta.getDocument().createRange();
            range.setStart(ancestors.get(containerIndex), offset);
            range.collapse(true);
        }
        return range;
    }

    /**
     * Looks for the first ancestor that hasn't been affected by the delete command.
     * 
     * @param ancestors the path from a node to the root element as it was before deleting the selected content
     * @return the index of the first node in the given list of ancestors that is still attached to the document
     */
    private int getFirstAttachedAncestorIndex(List<Node> ancestors)
    {
        for (int i = ancestors.size() - 1; i > 0; i--) {
            Node expectedParent = ancestors.get(i);
            Node actualParent = null;
            try {
                actualParent = ancestors.get(i - 1).getParentNode();
            } catch (Exception e) {
                // Accessing the properties of a deleted node can trigger sometimes and exception.
                // Keep actualParent null.
            }
            if (actualParent != expectedParent) {
                return i;
            }
        }
        return ancestors.size() > 0 ? 0 : -1;
    }

    /**
     * Copy the list of child nodes of the given parent node.
     * 
     * @param parent a DOM node
     * @return the list of child nodes of the given parent node
     */
    private List<Node> getChildren(Node parent)
    {
        List<Node> children = new ArrayList<Node>(parent.getChildNodes().getLength());
        Node child = parent.getFirstChild();
        while (child != null) {
            children.add(child);
            child = child.getNextSibling();
        }
        return children;
    }

    /**
     * Looks for the first child node that doesn't have the expected parent.
     * 
     * @param expectedParent the expected parent
     * @param children the list of child nodes before the delete command was executed
     * @return the index of the first deleted child node or {@code -1} of none of the child nodes has been deleted
     */
    private int getFirstDeletedChildIndex(Node expectedParent, List<Node> children)
    {
        for (int i = 0; i < children.size(); i++) {
            Node actualParent = null;
            try {
                actualParent = children.get(i).getParentNode();
            } catch (Exception e) {
                // Accessing the properties of a deleted node can trigger sometimes and exception.
                // Keep actualParent null.
            }
            if (actualParent != expectedParent) {
                return i;
            }
        }
        // None of the child nodes has been deleted.
        return -1;
    }
}
