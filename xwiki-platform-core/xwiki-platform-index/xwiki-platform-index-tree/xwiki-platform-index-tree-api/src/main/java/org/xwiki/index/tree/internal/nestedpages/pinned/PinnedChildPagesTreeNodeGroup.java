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
package org.xwiki.index.tree.internal.nestedpages.pinned;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractEntityTreeNode;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.tree.TreeNode;
import org.xwiki.tree.TreeNodeGroup;

/**
 * The group of pinned child pages tree nodes.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component
@Named(PinnedChildPagesTreeNodeGroup.HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class PinnedChildPagesTreeNodeGroup extends AbstractEntityTreeNode implements TreeNodeGroup
{
    /**
     * The component hint and also the tree node type.
     */
    public static final String HINT = "pinnedChildPages";

    @Inject
    private PinnedChildPagesManager pinnedChildPagesManager;

    /**
     * Default constructor.
     */
    public PinnedChildPagesTreeNodeGroup()
    {
        super(HINT);
    }

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        List<DocumentReference> pinnedChildPages = getPinnedChildPages(nodeId);
        return serialize(pinnedChildPages.subList(offset,
            limit < 0 ? pinnedChildPages.size() : Math.min(offset + limit, pinnedChildPages.size())));
    }

    @Override
    public int getChildCount(String nodeId)
    {
        return getPinnedChildPages(nodeId).size();
    }

    private List<DocumentReference> getPinnedChildPages(String nodeId)
    {
        @SuppressWarnings("unchecked")
        List<String> filters =
            (List<String>) getProperties().getOrDefault(TreeNode.PROPERTY_FILTERS, Collections.emptyList());
        if ("reference".equals(getProperties().get("hierarchyMode")) && filters.contains(HINT)) {
            EntityReference parentReference = resolve(nodeId);
            return this.pinnedChildPagesManager.getPinnedChildPages(parentReference);
        } else {
            return Collections.emptyList();
        }
    }
}
