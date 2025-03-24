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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class for representing a composite tree, where each type of node is handled by a separate component. The node
 * identifiers are prefixed with the node type.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
public abstract class AbstractCompositeTree extends AbstractTree
{
    protected final Map<String, TreeNode> treeNodeByNodeType = new HashMap<>();

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        TreeNode treeNode = getTreeNode(nodeId);
        return treeNode != null ? treeNode.getChildren(nodeId, offset, limit) : null;
    }

    @Override
    public int getChildCount(String nodeId)
    {
        TreeNode treeNode = getTreeNode(nodeId);
        return treeNode != null ? treeNode.getChildCount(nodeId) : 0;
    }

    @Override
    public String getParent(String nodeId)
    {
        TreeNode treeNode = getTreeNode(nodeId);
        return treeNode != null ? treeNode.getParent(nodeId) : null;
    }

    /**
     * @param nodeId the node identifier
     * @return the {@link TreeNode} component that handles the specified node type
     */
    protected TreeNode getTreeNode(String nodeId)
    {
        TreeNode treeNode = null;
        String[] parts = StringUtils.split(nodeId, ":", 2);
        if (parts != null && parts.length == 2) {
            treeNode = this.treeNodeByNodeType.get(parts[0]);
            if (treeNode != null) {
                // Update the node properties.
                treeNode = withSameProperties(treeNode);
            }
        }
        return treeNode;
    }
}
