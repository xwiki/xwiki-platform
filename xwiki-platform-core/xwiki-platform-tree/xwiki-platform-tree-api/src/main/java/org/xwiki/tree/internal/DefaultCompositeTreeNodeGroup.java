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
package org.xwiki.tree.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.tree.AbstractTreeNode;
import org.xwiki.tree.CompositeTreeNodeGroup;
import org.xwiki.tree.TreeNode;
import org.xwiki.tree.TreeNodeGroup;

/**
 * Default implementation of {@link CompositeTreeNodeGroup}.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
// Register this only as a CompositeTreeNodeGroup component and not as a TreeNode component.
@Component(roles = CompositeTreeNodeGroup.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultCompositeTreeNodeGroup extends AbstractTreeNode implements CompositeTreeNodeGroup
{
    private static class Entry
    {
        private final TreeNode treeNode;

        private final Predicate<String> condition;

        Entry(TreeNode treeNode, Predicate<String> condition)
        {
            this.treeNode = treeNode;
            this.condition = condition;
        }
    }

    private List<Entry> entries = new ArrayList<>();

    private BinaryOperator<String> idGenerator;

    @Override
    public void addTreeNode(TreeNode treeNode, Predicate<String> condition)
    {
        this.entries.add(new Entry(withSameProperties(treeNode), condition));
    }

    @Override
    public void setIdGenerator(BinaryOperator<String> idGenerator)
    {
        this.idGenerator = idGenerator;
    }

    @Override
    public int getChildCount(String nodeId)
    {
        return this.entries.stream().mapToInt(entry -> getLength(entry, nodeId)).sum();
    }

    private int getLength(Entry entry, String nodeId)
    {
        if (!entry.condition.test(nodeId)) {
            return 0;
        } else if (entry.treeNode instanceof TreeNodeGroup) {
            return withSameProperties(entry.treeNode).getChildCount(nodeId);
        } else {
            return 1;
        }
    }

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        int index = 0;
        int count = 0;
        while (index < this.entries.size()) {
            int length = getLength(this.entries.get(index), nodeId);
            if (count + length <= offset) {
                count += length;
                index++;
            } else {
                break;
            }
        }
        List<String> children = new ArrayList<>();
        while (index < this.entries.size() && (limit < 0 || children.size() < limit)) {
            Entry entry = this.entries.get(index++);
            if (entry.condition.test(nodeId)) {
                if (entry.treeNode instanceof TreeNodeGroup) {
                    children.addAll(withSameProperties(entry.treeNode).getChildren(nodeId, offset - count,
                        Math.max(limit - children.size(), -1)));
                    count = offset;
                } else {
                    children.add(this.idGenerator.apply(nodeId, entry.treeNode.getType()));
                }
            }
        }
        return children;
    }
}
