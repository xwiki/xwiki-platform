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
package org.xwiki.tree;

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * The interface used to represent a node in a tree structure.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Role
public interface TreeNode
{
    /**
     * The property that specifies how the child nodes are sorted.
     */
    String PROPERTY_ORDER_BY = "orderBy";

    /**
     * The property that specifies the nodes to exclude.
     */
    String PROPERTY_EXCLUSIONS = "exclusions";

    /**
     * Retrieve the children of the specified node.
     * 
     * @param nodeId the node id
     * @param offset the offset within the list of child nodes
     * @param limit the number of child nodes to return
     * @return a part of the child nodes of the specified node
     */
    List<String> getChildren(String nodeId, int offset, int limit);

    /**
     * Retrieve the number of children of the specified node.
     * 
     * @param nodeId the node id
     * @return the number of child nodes
     */
    int getChildCount(String nodeId);

    /**
     * Retrieve the parent of the specified node.
     * 
     * @param nodeId the node id
     * @return the parent node id
     */
    String getParent(String nodeId);

    /**
     * Retrieve the properties that can be used to configure the tree node. For instance you may want to sort or filter
     * the child nodes.
     * 
     * @return the map of tree node properties
     */
    Map<String, Object> getProperties();
}
