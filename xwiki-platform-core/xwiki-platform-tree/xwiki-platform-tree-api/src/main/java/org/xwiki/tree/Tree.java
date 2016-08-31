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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * The interface used to represent a tree structure.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Role
@Unstable
public interface Tree extends TreeNode
{
    /**
     * The property that specifies the id of the root node. This is used to represent a sub-tree.
     */
    String PROPERTY_ROOT = "root";

    /**
     * Retrieve the path of the specified node.
     * 
     * @param nodeId the node id
     * @return the path of the specified node
     */
    List<String> getPath(String nodeId);
}
