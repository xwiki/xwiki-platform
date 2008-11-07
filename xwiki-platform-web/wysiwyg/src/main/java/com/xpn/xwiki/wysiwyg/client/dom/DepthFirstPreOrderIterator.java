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
package com.xpn.xwiki.wysiwyg.client.dom;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.gwt.dom.client.Node;

/**
 * Iterator for the depth-first pre-order strategy, starting in a specified node.
 * 
 * @see http://www.w3.org/TR/DOM-Level-2-Traversal-Range/traversal.html#Traversal-Document
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

    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
    {
        return this.currentNode != null;
    }

    /**
     * {@inheritDoc}
     */
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
        } else if (currentNode == startNode) {
            // if we're in the top node and have no children, then search is done
            this.currentNode = null;
        // try to go right
        } else if (currentNode.getNextSibling() != null) {
            // We're under the root node, look for siblings
            this.currentNode = currentNode.getNextSibling();
        } else {
            // this is the last node in this parent's children list
            // Go up until you find a brother of an ancestor which is not null
            Node parent = currentNode.getParentNode();
            while (parent != startNode) {
                if (parent.getNextSibling() != null) {
                    this.currentNode = parent.getNextSibling();
                    break;
                }
                parent = parent.getParentNode();
            }
            // if we got back to the root searching up, then we have no more options
            if (parent == startNode) {
                this.currentNode = null;
            }
        }        
        return nodeToReturn;
    }

    /**
     * {@inheritDoc}
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
