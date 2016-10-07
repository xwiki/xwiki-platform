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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.tree.AbstractTreeNode;

/**
 * Base class for tree nodes that represent entities.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
public abstract class AbstractEntityTreeNode extends AbstractTreeNode
{
    @Inject
    protected EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    protected QueryManager queryManager;

    @Inject
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @Inject
    @Named("user")
    private ConfigurationSource userPreferencesSource;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    protected EntityReference resolve(String nodeId)
    {
        String[] parts = StringUtils.split(nodeId, ":", 2);
        if (parts == null || parts.length != 2) {
            return null;
        }

        try {
            EntityType entityType = EntityType.valueOf(camelCaseToUnderscore(parts[0]).toUpperCase());
            return this.currentEntityReferenceResolver.resolve(parts[1], entityType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected String serialize(EntityReference entityReference)
    {
        return underscoreToCamelCase(entityReference.getType().name().toLowerCase()) + ':'
            + this.defaultEntityReferenceSerializer.serialize(entityReference);
    }

    protected <E extends EntityReference> List<String> serialize(List<E> entityReferences)
    {
        List<String> nodeIds = new ArrayList<String>();
        for (EntityReference entityReference : entityReferences) {
            nodeIds.add(serialize(entityReference));
        }
        return nodeIds;
    }

    private String camelCaseToUnderscore(String nodeType)
    {
        return StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(nodeType), '_');
    }

    private String underscoreToCamelCase(String entityType)
    {
        StringBuilder result = new StringBuilder();
        for (String part : StringUtils.split(entityType, '_')) {
            result.append(StringUtils.capitalize(part));
        }
        return StringUtils.uncapitalize(result.toString());
    }

    protected boolean areHiddenEntitiesShown()
    {
        boolean shown = !Boolean.TRUE.equals(getProperties().get("filterHiddenDocuments"));
        if (!shown) {
            Integer value = this.userPreferencesSource.getProperty("displayHiddenDocuments", Integer.class);
            shown = value != null && value == 1;
        }
        return shown;
    }

    protected String whereClause(List<String> constraints)
    {
        return "where " + StringUtils.join(constraints, " and ");
    }

    protected int getChildSpacesCount(EntityReference parentReference) throws QueryException
    {
        List<String> constraints = new ArrayList<String>();
        Map<String, Object> parameters = new HashMap<String, Object>();

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

        String statement = "select count(*) from XWikiSpace " + whereClause(constraints);
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        query.setWiki(parentReference.extractReference(EntityType.WIKI).getName());
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
}
