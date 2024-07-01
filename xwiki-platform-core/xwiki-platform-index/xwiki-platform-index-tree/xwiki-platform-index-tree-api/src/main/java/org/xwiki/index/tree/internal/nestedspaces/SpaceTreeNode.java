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
package org.xwiki.index.tree.internal.nestedspaces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractEntityTreeNode;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

/**
 * The space node in the nested spaces hierarchy.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named(SpaceTreeNode.HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SpaceTreeNode extends AbstractEntityTreeNode
{
    /**
     * The component hint and also the tree node type.
     */
    public static final String HINT = "space";

    @Inject
    private LocalizationContext localizationContext;

    @Inject
    @Named("hidden/document")
    private Provider<QueryFilter> hiddenDocumentQueryFilterProvider;

    @Inject
    @Named("count")
    private QueryFilter countQueryFilter;

    /**
     * Reuse the child page filter from the nested pages hierarchy.
     */
    @Inject
    @Named("childPage/nestedPages")
    private QueryFilter childPageFilter;

    /**
     * Reuse the hidden page filter from the nested pages hierarchy.
     */
    @Inject
    @Named("hiddenPage/nestedPages")
    private QueryFilter hiddenPageFilter;

    @Inject
    @Named("documentOrSpaceReferenceResolver/nestedSpaces")
    private QueryFilter documentOrSpaceReferenceResolverFilter;

    /**
     * Default constructor.
     */
    public SpaceTreeNode()
    {
        super(HINT);
    }

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        EntityReference spaceReference = resolve(nodeId);
        if (spaceReference != null && spaceReference.getType() == EntityType.SPACE) {
            try {
                return serialize(getChildren(new SpaceReference(spaceReference), offset, limit));
            } catch (QueryException e) {
                this.logger.warn("Failed to retrieve the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return Collections.emptyList();
    }

    protected List<? extends EntityReference> getChildren(SpaceReference spaceReference, int offset, int limit)
        throws QueryException
    {
        return getChildrenQuery(spaceReference, offset, limit).execute();
    }

    protected Query getChildrenQuery(SpaceReference spaceReference, int offset, int limit) throws QueryException
    {
        String orderBy = getOrderBy();
        Query query;
        if (areTerminalDocumentsShown()) {
            if ("title".equals(orderBy)) {
                query = this.queryManager.getNamedQuery("nestedSpacesOrderedByTitle");
                query.bindValue("locale", this.localizationContext.getCurrentLocale().toString());
            } else {
                query = this.queryManager.getNamedQuery("nestedSpacesOrderedByName");
            }
        } else {
            // Query only the spaces table.
            query = this.queryManager.createQuery(
                "select reference, 0 as terminal from XWikiSpace page order by lower(name), name", Query.HQL);
        }

        query.setWiki(spaceReference.getWikiReference().getName());
        query.setOffset(offset);
        query.setLimit(limit);

        query.addFilter(this.childPageFilter);
        query.bindValue("parent", this.localEntityReferenceSerializer.serialize(spaceReference));

        if (!areHiddenEntitiesShown()) {
            query.addFilter(this.hiddenPageFilter);
        }

        query.addFilter(this.documentOrSpaceReferenceResolverFilter);

        return query;
    }

    @Override
    public int getChildCount(String nodeId)
    {
        EntityReference spaceReference = resolve(nodeId);
        if (spaceReference != null && spaceReference.getType() == EntityType.SPACE) {
            try {
                return getChildCount(new SpaceReference(spaceReference));
            } catch (QueryException e) {
                this.logger.warn("Failed to count the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return 0;
    }

    protected int getChildCount(SpaceReference spaceReference) throws QueryException
    {
        int count = getChildSpacesCount(spaceReference);
        if (areTerminalDocumentsShown()) {
            count += getChildDocumentsCount(spaceReference);
        }
        return count;
    }

    protected int getChildDocumentsCount(SpaceReference spaceReference) throws QueryException
    {
        List<String> constraints = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        return getChildDocumentsCount(spaceReference, constraints, parameters);
    }

    protected int getChildDocumentsCount(SpaceReference spaceReference, List<String> constraints,
        Map<String, Object> parameters) throws QueryException
    {
        constraints.add("doc.translation = 0");
        constraints.add("doc.space = :space");

        parameters.put(HINT, this.localEntityReferenceSerializer.serialize(spaceReference));

        Query query = this.queryManager.createQuery(whereClause(constraints), Query.HQL);
        query.addFilter(this.countQueryFilter);
        if (Boolean.TRUE.equals(getProperties().get("filterHiddenDocuments"))) {
            query.addFilter(this.hiddenDocumentQueryFilterProvider.get());
        }
        query.setWiki(spaceReference.getWikiReference().getName());
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.bindValue(entry.getKey(), entry.getValue());
        }
        return ((Long) query.execute().get(0)).intValue();
    }

    @Override
    public String getParent(String nodeId)
    {
        EntityReference spaceReference = resolve(nodeId);
        if (spaceReference != null && spaceReference.getType() == EntityType.SPACE) {
            return serialize(spaceReference.getParent());
        }
        return null;
    }
}
