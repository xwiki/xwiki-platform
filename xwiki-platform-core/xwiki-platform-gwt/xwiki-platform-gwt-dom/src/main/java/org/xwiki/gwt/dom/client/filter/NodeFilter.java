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
package org.xwiki.gwt.dom.client.filter;

import com.google.gwt.dom.client.Node;

/**
 * Interface used to filter DOM nodes.
 * 
 * @version $Id$
 * @see "http://www.w3.org/TR/DOM-Level-2-Traversal-Range/traversal.html#Traversal-NodeFilter"
 */
public interface NodeFilter
{
    /**
     * The action that should be taken on the filtered node.
     */
    enum Action
    {
        /**
         * Accept the node. Note iterators should return this node.
         */
        ACCEPT,

        /**
         * Reject the node. Tree iterators should reject the children of this node too. Node list iterators should
         * consider this equivalent to {@link #SKIP}.
         */
        REJECT,

        /**
         * Skip this single node. Both tree and list iterators will still consider the children of this node.
         */
        SKIP;
    }

    /**
     * Applies this filter on the given DOM node.
     * 
     * @param node the node to be filtered
     * @return the action that should be taken on the filtered node by the calling iterator
     */
    Action acceptNode(Node node);
}
