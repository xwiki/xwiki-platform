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
package org.xwiki.export.pdf.internal.job;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.comparator.DocumentReferenceComparator;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.tree.Tree;

/**
 * Compares document references by looking at their position in the navigation tree of nested pages.
 *
 * @version $Id$
 * @since 16.10.0
 */
@Component(roles = NavigationTreeDocumentReferenceComparator.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class NavigationTreeDocumentReferenceComparator extends DocumentReferenceComparator implements Initializable
{
    /**
     * The number of children to fetch at once.
     */
    private static final int BATCH_SIZE = 100;

    private static final class ChildrenCache
    {
        private List<EntityReference> children = new LinkedList<>();

        private int offset;

        public List<EntityReference> getChildren()
        {
            return this.children;
        }

        public int getOffset()
        {
            return this.offset;
        }

        public void setOffset(int offset)
        {
            this.offset = offset;
        }
    }

    @Inject
    @Named("nestedPages")
    private Tree nestedPagesTree;

    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    private EntityReferenceResolver<String> defaultEntityReferenceResolver;

    private Map<EntityReference, ChildrenCache> childrenCachePerParent = new HashMap<>();

    private Map<EntityReference, Integer> siblingIndex = new HashMap<>();

    @Override
    public void initialize() throws InitializationException
    {
        this.nestedPagesTree.getProperties().put("hierarchyMode", "reference");
        this.nestedPagesTree.getProperties().put("orderBy", "title");
        this.nestedPagesTree.getProperties().put("filterHiddenDocuments", false);
        this.nestedPagesTree.getProperties().put("filters", List.of("pinnedChildPages"));
    }

    @Override
    protected List<EntityReference> getPath(EntityReference entityReference)
    {
        return this.nestedPagesTree.getPath(getNodeId(entityReference)).stream()
            .map(this::resolveEntityReferenceFromNodeId).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private String getNodeId(EntityReference entityReference)
    {
        if (entityReference == null) {
            // The root node.
            return "farm:*";
        } else {
            return entityReference.getType().name().toLowerCase() + ':'
                + this.defaultEntityReferenceSerializer.serialize(entityReference);
        }
    }

    private EntityReference resolveEntityReferenceFromNodeId(String nodeId)
    {
        String[] parts = StringUtils.split(nodeId, ":", 2);
        if (parts == null || parts.length != 2) {
            return null;
        }
        try {
            EntityType entityType = EntityType.valueOf(parts[0].toUpperCase());
            return this.defaultEntityReferenceResolver.resolve(parts[1], entityType);
        } catch (IllegalArgumentException e) {
            // Unknown entity type.
            return null;
        }
    }

    @Override
    protected int compareSiblingEntities(EntityReference alice, EntityReference bob)
    {
        if (equalsIgnoringParameters(alice, bob)) {
            // Appears to be the same entity but some parameters may differ (e.g. the locale) so we fallback to the base
            // comparator.
            return super.compareSiblingEntities(alice, bob);
        }

        ChildrenCache childrenCache =
            this.childrenCachePerParent.computeIfAbsent(getParent(alice), parent -> new ChildrenCache());
        int aliceIndex = this.siblingIndex.computeIfAbsent(alice, child -> childrenCache.getChildren().indexOf(child));
        int bobIndex = this.siblingIndex.computeIfAbsent(bob, child -> childrenCache.getChildren().indexOf(child));
        if (aliceIndex < 0 && bobIndex < 0) {
            if (!findFirst(alice, bob)) {
                // We couldn't find any of the entities in the navigation tree so we fallback to the base comparator.
                return super.compareSiblingEntities(alice, bob);
            } else {
                aliceIndex = this.siblingIndex.get(alice);
                bobIndex = this.siblingIndex.get(bob);
            }

        }

        if (aliceIndex >= 0) {
            if (bobIndex >= 0) {
                // We found both entities so we can compare their indices.
                return aliceIndex - bobIndex;
            }
            // We found only the first entity so far so we know for sure it appears before the second entity.
            return -1;
        } else {
            // We found only the second entity so far so we know for sure it appears before the first entity.
            return 1;
        }
    }

    private boolean equalsIgnoringParameters(EntityReference alice, EntityReference bob)
    {
        return Objects.equals(alice.getType(), bob.getType()) && Objects.equals(alice.getName(), bob.getName())
            && Objects.equals(alice.getParent(), bob.getParent());
    }

    private EntityReference getParent(EntityReference childReference)
    {
        return resolveEntityReferenceFromNodeId(this.nestedPagesTree.getParent(getNodeId(childReference)));
    }

    private boolean findFirst(EntityReference alice, EntityReference bob)
    {
        EntityReference parent = getParent(alice);
        ChildrenCache childrenCache = this.childrenCachePerParent.get(parent);
        List<EntityReference> batch;
        do {
            int offset = childrenCache.getOffset();
            batch = fetchMoreChildren(parent);

            int aliceIndex = batch.indexOf(alice);
            if (aliceIndex >= 0) {
                this.siblingIndex.put(alice, offset + aliceIndex);
            }
            int bobIndex = batch.indexOf(bob);
            if (bobIndex >= 0) {
                this.siblingIndex.put(bob, offset + bobIndex);
            }
            if (aliceIndex >= 0 || bobIndex >= 0) {
                return true;
            }
        } while (!batch.isEmpty());

        return false;
    }

    private List<EntityReference> fetchMoreChildren(EntityReference parent)
    {
        ChildrenCache childrenCache = this.childrenCachePerParent.get(parent);
        List<EntityReference> children = getChildren(parent, childrenCache.getOffset(), BATCH_SIZE);
        childrenCache.getChildren().addAll(children);
        childrenCache.setOffset(childrenCache.getOffset() + children.size());
        return children;
    }

    private List<EntityReference> getChildren(EntityReference parent, int offset, int limit)
    {
        List<String> children = this.nestedPagesTree.getChildren(getNodeId(parent), offset, limit);
        if (children == null) {
            return List.of();
        }
        return children.stream().map(this::resolveEntityReferenceFromNodeId).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
