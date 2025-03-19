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
package org.xwiki.index.tree;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.internal.nestedpages.pinned.PinnedChildPagesManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script service for index tree operations.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.5
 * @since 16.4.7
 */
@Unstable
@Component
@Singleton
@Named("index.tree")
public class IndexTreeScriptService implements ScriptService
{
    @Inject
    private PinnedChildPagesManager pinnedChildPagesManager;

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    /**
     * Retrieve the list of pinned child pages of the given parent.
     * @param parent the document for which to find pinned child pages.
     * @return the ordered list of pinned child pages.
     */
    public List<DocumentReference> getPinnedChildPages(DocumentReference parent)
    {
        return this.pinnedChildPagesManager.getPinnedChildPages(parent);
    }

    /**
     * Compute a distance between 2 tree nodes represented by their reference. The distance can be understood as the
     * number of hierarchy in the tree between two nodes, and makes a difference between terminal and non-terminal
     * documents. For example, the distance between space {@code xwiki:Space1} and document {@code xwiki:Space1.Sub
     * .MyDocument} is {@code 2}, but with document {@code xwiki:Space1.Sub.WebHome} it's {@code 1}. Also the result is
     * same if the given parent is document {@code xwiki:Space1.WebHome} instead of space {@code xwiki:Space1}.
     * Finally if the given parent argument is not a parent of the other argument, then returned value is {@code -1}.
     * @param nodeParent the reference supposed to be a parent of the other parameter
     * @param nodeChildren the node for which to compute the distance with the given parent
     * @return {@code -1} if there's no parent relationship between nodes, else returns the computed distance as
     * explained above.
     */
    public int computeDistanceBetweenNodes(EntityReference nodeParent, EntityReference nodeChildren)
    {
        int result = -1;
        if (nodeParent.equals(nodeChildren)) {
            result = 0;
        } else if (nodeChildren.hasParent(nodeParent)) {
            List<EntityReference> chainElements = new ArrayList<>(nodeChildren.getReversedReferenceChain());
            chainElements.removeAll(nodeParent.getReversedReferenceChain());
            result = chainElements.size();
            if (nodeChildren.getType() == EntityType.DOCUMENT
                && nodeChildren.getName().equals(
                    entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName())) {
                result--;
            }
        } else if (nodeParent.getType() == EntityType.DOCUMENT) {
            result = computeDistanceBetweenNodes(nodeParent.getParent(), nodeChildren);
        }
        return result;
    }
}
