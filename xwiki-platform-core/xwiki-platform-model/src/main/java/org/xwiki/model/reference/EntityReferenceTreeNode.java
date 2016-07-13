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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.xwiki.model.EntityType;

/**
 * A node in a {@link EntityReferenceTree}.
 * 
 * @version $Id$
 * @since 5.4RC1
 */
public class EntityReferenceTreeNode
{
    /**
     * USed to order {@link Locale}s.
     * 
     * @version $Id$
     */
    static final class LocaleComparator implements Comparator<Locale>
    {
        static final LocaleComparator INSTANCE = new LocaleComparator();

        private LocaleComparator()
        {
            // Stateless
        }

        @Override
        public int compare(Locale locale1, Locale locale2)
        {
            return locale1.toString().compareTo(locale2.toString());
        }
    }

    private final EntityReference reference;

    private final int referenceSize;

    private final Comparator<String> comparator;

    private Map<String, Map<EntityType, EntityReferenceTreeNode>> children;

    private Map<Locale, EntityReference> locales;

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
            this.children = new TreeMap<String, Map<EntityType, EntityReferenceTreeNode>>(this.comparator);
        }

        List<EntityReference> childReferenceList = childReference.getReversedReferenceChain();

        EntityReference childNodeReference = childReferenceList.get(this.referenceSize);

        Map<EntityType, EntityReferenceTreeNode> childrenByType = this.children.get(childNodeReference.getName());
        if (childrenByType == null) {
            childrenByType = new IdentityHashMap<>();
            this.children.put(childNodeReference.getName(), childrenByType);
        }

        EntityReferenceTreeNode childNode = childrenByType.get(childNodeReference.getType());
        if (childNode == null) {
            childNode = new EntityReferenceTreeNode(childNodeReference, this.comparator);
        }

        if (childReferenceList.size() > this.referenceSize + 1) {
            childNode.addChild(childReference);
        }

        if (childNodeReference.getParameter(DocumentReference.LOCALE) != null) {
            childNode.addLocale(childReference);
        }

        childrenByType.put(childNodeReference.getType(), childNode);
    }

    void addLocale(EntityReference childReference)
    {
        if (this.locales == null) {
            this.locales = new TreeMap<Locale, EntityReference>(LocaleComparator.INSTANCE);
        }

        this.locales.put((Locale) childReference.getParameter(DocumentReference.LOCALE), childReference);
    }

    /**
     * @return the reference associated to this node
     */
    public EntityReference getReference()
    {
        return this.reference;
    }

    /**
     * Return the first found direct child with passed name.
     * 
     * @param name the name of the child node
     * @return the node associated to the passed name
     */
    public EntityReferenceTreeNode get(String name)
    {
        Collection<EntityReferenceTreeNode> childrenWhithName = getChildrenInternal(name);

        if (childrenWhithName != null && !childrenWhithName.isEmpty()) {
            return childrenWhithName.iterator().next();
        }

        return null;
    }

    /**
     * @param path a path in the tree starting from this node, specified as an {@link EntityReference}
     * @return the node associated to the specified path
     * @since 7.2M1
     */
    public EntityReferenceTreeNode get(EntityReference path)
    {
        if (path == null) {
            return null;
        }

        EntityReferenceTreeNode descendant = this;
        for (EntityReference pathElement : path.getReversedReferenceChain()) {
            if (descendant.children == null) {
                return null;
            }

            Map<EntityType, EntityReferenceTreeNode> descendantByType = descendant.children.get(pathElement.getName());
            if (descendantByType == null) {
                return null;
            }

            descendant = descendantByType.get(pathElement.getType());
            if (descendant == null) {
                return null;
            }
        }

        return descendant;
    }

    /**
     * @return the child reference nodes
     */
    public Collection<EntityReferenceTreeNode> getChildren()
    {
        if (this.children != null) {
            List<EntityReferenceTreeNode> childrenList = new ArrayList<>(this.children.size() * 2);

            for (Map<EntityType, EntityReferenceTreeNode> childrenByType : this.children.values()) {
                childrenList.addAll(childrenByType.values());
            }

            return childrenList;
        }

        return Collections.emptyList();
    }

    /**
     * @param name the name of the children to return
     * @return the children with the passed name
     * @since 7.4M1
     */
    public Collection<EntityReferenceTreeNode> getChildren(String name)
    {
        Collection<EntityReferenceTreeNode> childrenWhithName = getChildrenInternal(name);

        return childrenWhithName != null ? Collections.unmodifiableCollection(childrenWhithName)
            : Collections.<EntityReferenceTreeNode>emptyList();
    }

    /**
     * @param type the type of the children to return
     * @return the children with the passed {@link EntityType}
     * @since 7.4M1
     */
    public Collection<EntityReferenceTreeNode> getChildren(EntityType type)
    {
        if (this.children != null) {
            List<EntityReferenceTreeNode> childrenList = new ArrayList<>(this.children.size());

            for (Map<EntityType, EntityReferenceTreeNode> childrenByType : this.children.values()) {
                EntityReferenceTreeNode child = childrenByType.get(type);
                if (child != null) {
                    childrenList.add(child);
                }
            }

            return childrenList;
        }

        return Collections.emptyList();
    }

    /**
     * @param name the name of the children to return
     * @return the children with the passed name
     */
    public Collection<EntityReferenceTreeNode> getChildrenInternal(String name)
    {
        if (this.children != null) {
            Map<EntityType, EntityReferenceTreeNode> childrenByType = this.children.get(name);

            if (childrenByType != null) {
                return childrenByType.values();
            }
        }

        return null;
    }

    /**
     * @return the child references containing a {@link Locale}
     */
    public Collection<EntityReference> getLocales()
    {
        return this.locales != null ? this.locales.values() : Collections.<EntityReference>emptyList();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        if (getReference() != null) {
            builder.append(getReference());
        }

        if (!getChildren().isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" = ");
            }
            builder.append(getChildren());
        }

        return builder.toString();
    }
}
