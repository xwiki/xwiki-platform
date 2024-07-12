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

import org.xwiki.stability.Unstable;

/**
 * A group of related tree nodes, similar to a document fragment in a DOM tree.
 * 
 * @version $Id$
 * @since 16.4.0RC1
 */
@Unstable
public interface TreeNodeGroup extends TreeNode
{
    @Override
    default String getParent(String nodeId)
    {
        // Tree node groups are never attached to the tree.
        return null;
    }
}
