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
package org.xwiki.validator.framework;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Utility class allowing to iterate over a {@link NodeList}.
 * 
 * @version $Id$
 */
public class NodeListIterable implements Iterator<Node>, Iterable<Node> 
{
    
    /**
     * Wrapped {@link NodeList}.
     */
    private NodeList nodeList;
    
    /**
     * Current iterator index.
     */
    private int index;

    /**
     * Constructor.
     * 
     * @param nodeList {@link NodeList} to wrap. 
     */
    public NodeListIterable(NodeList nodeList) {
        this.nodeList = nodeList;
    }
    
    /**
     * @return the wrapped {@link NodeList}.
     */
    public NodeList getNodeList()
    {
        return nodeList;
    }

    /**
     * @return the next {@link Node} in the list
     * @throws NoSuchElementException if the next {@link Node} cannot be retrieved 
     */
    public Node next() throws NoSuchElementException {
        Node node = nodeList.item(index++);
        
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node;
    }

    /**
     * @return true if there is at least another element in the list
     */
    public boolean hasNext() {
        return index < nodeList.getLength();
    }

    @Override
    public NodeListIterable iterator() {
        return this;
    }

    /**
     * The remove method is overriden in order to throw an {@link UnsupportedOperationException} when accessed.
     * 
     * @throws UnsupportedOperationException everytime it is called 
     */    
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
