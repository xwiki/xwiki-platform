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

import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * A tree node group that aggregates multiple, optional, tree nodes.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Role
@Unstable
public interface CompositeTreeNodeGroup extends TreeNodeGroup
{
    /**
     * Adds an optional tree node to the group, that is enabled for a given node id only when the specified condition is
     * met.
     * 
     * @param treeNode the optional tree node to add
     * @param condition the condition that must be met for the tree node to be enabled for a given node id
     */
    void addTreeNode(TreeNode treeNode, Predicate<String> condition);

    /**
     * Sets the id generator used to generate ids for the aggregated child nodes.
     * 
     * @param idGenerator a function that generates the id for an aggregated child node, given the parent node id and
     *            the child node type
     */
    void setIdGenerator(BinaryOperator<String> idGenerator);
}
