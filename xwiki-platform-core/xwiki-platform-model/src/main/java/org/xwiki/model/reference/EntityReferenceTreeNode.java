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
package org.xwiki.model.reference;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xwiki.stability.Unstable;

/**
 * A node in a {@link EntityReferenceTree}.
 * 
 * @version $Id$
 * @since 5.4RC1
 */
@Unstable
public class EntityReferenceTreeNode
{
    private final EntityReference reference;

    private final int referenceSize;

    private final Comparator<String> comparator;

    private Map<String, EntityReferenceTreeNode> children;

    protected EntityReferenceTreeNode(Comparator<String> comparator)
    {
        this.reference = null;
        this.referenceSize = 0;
        this.comparator = comparator;
    }

    EntityReferenceTreeNode(EntityReference reference, Comparator<String> comparator)
    {
        this.reference = reference;
        this.referenceSize = reference.getReversedReferenceChain().size();
        this.comparator = comparator;
    }

    void addChild(EntityReference childReference)
    {
        if (this.children == null) {
            this.children = new TreeMap<String, EntityReferenceTreeNode>(this.comparator);
        }

        List<EntityReference> childReferenceList = childReference.getReversedReferenceChain();

        EntityReference childNodeReference = childReferenceList.get(this.referenceSize);
        EntityReferenceTreeNode childNode = this.children.get(childNodeReference.getName());
        if (childNode == null) {
            childNode = new EntityReferenceTreeNode(childNodeReference, this.comparator);
        }

        if (childReferenceList.size() > this.referenceSize + 1) {
            childNode.addChild(childReference);
        }

        this.children.put(childNode.getReference().getName(), childNode);
    }

    /**
     * @return the reference associated to this node
     */
    public EntityReference getReference()
    {
        return this.reference;
    }

    /**
     * @param name the name of the child node
     * @return the node associated to the passed name
     */
    public EntityReferenceTreeNode get(String name)
    {
        return this.children != null ? this.children.get(name) : null;
    }

    /**
     * @return the child reference nodes
     */
    public Collection<EntityReferenceTreeNode> getChildren()
    {
        return this.children != null ? this.children.values() : Collections.<EntityReferenceTreeNode> emptyList();
    }
}
