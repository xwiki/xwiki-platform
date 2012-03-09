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
package org.xwiki.gwt.dom.client;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.gwt.dom.client.Node;

/**
 * Iterator for the depth-first pre-order strategy, starting in a specified node.
 * 
 * @see "http://www.w3.org/TR/DOM-Level-2-Traversal-Range/traversal.html#Traversal-Document"
 * @version $Id$
 */
public class DepthFirstPreOrderIterator implements Iterator<Node>
{
    /**
     * The current position of the iterator.
     */
    private Node currentNode;

    /**
     * The node where the iteration has started (the root of the subtree which we're iterating).
     */
    private Node startNode;

    /**
     * Creates an iterator for the subtree rooted in startNode.
     * 
     * @param startNode root of the subtree to iterate through.
     */
    public DepthFirstPreOrderIterator(Node startNode)
    {
        this.startNode = startNode;
        this.currentNode = startNode;
    }

    @Override
    public boolean hasNext()
    {
        return this.currentNode != null;
    }

    @Override
    public Node next()
    {
        // return the currentNode
        Node nodeToReturn = this.currentNode;
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        // compute the next node
        // try to go down
        if (currentNode.getFirstChild() != null) {
            this.currentNode = currentNode.getFirstChild();
        } else {
            // try to go right: from this node or any of its ancestors, until we haven't reached the startNode
            Node ancestor = currentNode;
            while (ancestor != startNode) {
                if (ancestor.getNextSibling() != null) {
                    this.currentNode = ancestor.getNextSibling();
                    break;
                }
                ancestor = ancestor.getParentNode();
            }
            // if we got back to the root searching up, then we have no more options
            if (ancestor == startNode) {
                this.currentNode = null;
            }
        }
        return nodeToReturn;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
