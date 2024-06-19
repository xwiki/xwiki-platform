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
package org.xwiki.index.tree.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.converter.Converter;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.tree.AbstractTreeNode;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserPropertiesResolver;

/**
 * Base class for tree nodes that represent entities.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
public abstract class AbstractEntityTreeNode extends AbstractTreeNode
{
    /**
     * There is a single farm node and this is its id.
     */
    protected static final String FARM_NODE_ID = "farm:*";

    @Inject
    protected EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    protected QueryManager queryManager;

    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    protected AbstractEntityTreeNode(String type)
    {
        super(type);
    }

    protected EntityReference resolve(String nodeId)
    {
        return this.entityTreeNodeIdConverter.convert(EntityReference.class, nodeId);
    }

    protected String serialize(EntityReference entityReference)
    {
        return this.entityTreeNodeIdConverter.convert(String.class, entityReference);
    }

    protected <E extends EntityReference> List<String> serialize(List<E> entityReferences)
    {
        return entityReferences.stream().map(this::serialize).collect(Collectors.toList());
    }

    protected boolean areHiddenEntitiesShown()
    {
        boolean shown = !Boolean.TRUE.equals(getProperties().get("filterHiddenDocuments"));
        if (!shown) {
            shown = this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE).displayHiddenDocuments();
        }
        return shown;
    }

    protected String whereClause(List<String> constraints)
    {
        return "where " + StringUtils.join(constraints, " and ");
    }

    protected int getChildSpacesCount(EntityReference parentReference) throws QueryException
    {
        List<String> constraints = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();

        EntityReference parentSpaceReference = parentReference.extractReference(EntityType.SPACE);
        if (parentSpaceReference != null) {
            constraints.add("parent = :parent");
            parameters.put("parent", this.localEntityReferenceSerializer.serialize(parentSpaceReference));
        } else {
            constraints.add("parent is null");
        }
        if (!areHiddenEntitiesShown()) {
            constraints.add("hidden <> true");
        }
        EntityReference wikiReference = parentReference.extractReference(EntityType.WIKI);
        Set<String> excludedSpaces =
            getExcludedSpaces(parentSpaceReference != null ? parentSpaceReference : wikiReference);
        if (!excludedSpaces.isEmpty()) {
            constraints.add("reference not in (:excludedSpaces)");
            parameters.put("excludedSpaces", excludedSpaces);
        }

        String statement = "select count(*) from XWikiSpace " + whereClause(constraints);
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        query.setWiki(wikiReference.getName());
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.bindValue(entry.getKey(), entry.getValue());
        }

        return ((Long) query.execute().get(0)).intValue();
    }

    protected String getDefaultDocumentName()
    {
        return this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
    }

    protected boolean areTerminalDocumentsShown()
    {
        return !Boolean.FALSE.equals(getProperties().get("showTerminalDocuments"));
    }

    protected Set<EntityReference> getExcludedEntities(String parentNodeId)
    {
        return getExclusions(parentNodeId).stream().map(this::resolve).filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    protected Set<String> getExcludedWikis()
    {
        return getExcludedEntities(FARM_NODE_ID).stream().filter(AbstractEntityTreeNode::isWiki)
            .map(EntityReference::getName).collect(Collectors.toSet());
    }

    protected Set<String> getExcludedSpaces(EntityReference parentReference)
    {
        return getExcludedEntities(serialize(parentReference)).stream()
            .filter(entityReference -> this.isSpace(entityReference) || this.isNestedDocument(entityReference))
            .filter(entityReference -> entityReference.hasParent(parentReference))
            .map(entityReference -> this.localEntityReferenceSerializer
                .serialize(entityReference.extractReference(EntityType.SPACE)))
            .collect(Collectors.toSet());
    }

    protected Set<String> getExcludedDocuments(EntityReference parentReference)
    {
        return getExcludedEntities(serialize(parentReference)).stream().filter(this::isTerminalDocument)
            .filter(entityReference -> entityReference.hasParent(parentReference))
            .map(entityReference -> this.localEntityReferenceSerializer.serialize(entityReference))
            .collect(Collectors.toSet());
    }

    private static boolean isWiki(EntityReference entityReference)
    {
        return entityReference.getType() == EntityType.WIKI;
    }

    private boolean isSpace(EntityReference entityReference)
    {
        return entityReference.getType() == EntityType.SPACE;
    }

    private boolean isNestedDocument(EntityReference entityReference)
    {
        return entityReference.getType() == EntityType.DOCUMENT
            && Objects.equals(entityReference.getName(), getDefaultDocumentName());
    }

    private boolean isTerminalDocument(EntityReference entityReference)
    {
        return entityReference.getType() == EntityType.DOCUMENT
            && !Objects.equals(entityReference.getName(), getDefaultDocumentName());
    }
}
