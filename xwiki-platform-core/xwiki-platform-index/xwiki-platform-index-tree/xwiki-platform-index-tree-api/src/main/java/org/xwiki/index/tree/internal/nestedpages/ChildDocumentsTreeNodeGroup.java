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
package org.xwiki.index.tree.internal.nestedpages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractChildDocumentsTreeNodeGroup;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

/**
 * A tree node group that contains the child documents of a specified parent entity.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component
@Named(ChildDocumentsTreeNodeGroup.HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ChildDocumentsTreeNodeGroup extends AbstractChildDocumentsTreeNodeGroup
{
    /**
     * The component hint and also the tree node type.
     */
    public static final String HINT = "childDocuments";

    private static final String FIELD_TITLE = "title";

    private static final String PARAMETER_LOCALE = "locale";

    private static final String PARAMETER_EXCLUDED_DOCUMENTS = "excludedDocuments";

    @Inject
    @Named("count")
    protected QueryFilter countQueryFilter;

    @Inject
    @Named("hidden/document")
    protected Provider<QueryFilter> hiddenDocumentQueryFilterProvider;

    @Inject
    private LocalizationContext localizationContext;

    @Inject
    @Named("topLevelPage/nestedPages")
    private QueryFilter topLevelPageFilter;

    @Inject
    @Named("childPage/nestedPages")
    private QueryFilter childPageFilter;

    @Inject
    @Named("hiddenPage/nestedPages")
    private QueryFilter hiddenPageFilter;

    @Inject
    @Named("excludedSpace/nestedPages")
    private QueryFilter excludedSpaceFilter;

    @Inject
    @Named("excludedDocument/nestedPages")
    private QueryFilter excludedDocumentFilter;

    @Inject
    @Named("documentReferenceResolver/nestedPages")
    private QueryFilter documentReferenceResolverFilter;

    /**
     * Default constructor.
     */
    public ChildDocumentsTreeNodeGroup()
    {
        super(HINT);
    }

    @Override
    protected List<DocumentReference> getChildDocuments(EntityReference parentReference, int offset, int limit)
        throws QueryException
    {
        String orderBy = getOrderBy();
        Query query;
        if (canHaveTerminalChildDocuments(parentReference)) {
            if (FIELD_TITLE.equals(orderBy)) {
                query = this.queryManager.getNamedQuery("nestedPagesOrderedByTitle");
                query.bindValue(PARAMETER_LOCALE, this.localizationContext.getCurrentLocale().toString());
            } else {
                query = this.queryManager.getNamedQuery("nestedPagesOrderedByName");
            }
            Set<String> excludedDocuments = getExcludedDocuments(parentReference.getParent());
            if (!excludedDocuments.isEmpty()) {
                query.bindValue(PARAMETER_EXCLUDED_DOCUMENTS, excludedDocuments);
                query.addFilter(this.excludedDocumentFilter);
            }
        } else {
            if (FIELD_TITLE.equals(orderBy)) {
                query = this.queryManager.getNamedQuery("nonTerminalPagesOrderedByTitle");
                query.bindValue(PARAMETER_LOCALE, this.localizationContext.getCurrentLocale().toString());
            } else {
                // Query only the spaces table.
                query = this.queryManager.createQuery(
                    "select reference, 0 as terminal from XWikiSpace page order by lower(name), name", Query.HQL);
            }
        }

        query.setWiki(parentReference.extractReference(EntityType.WIKI).getName());
        query.setOffset(offset);
        query.setLimit(limit);

        if (parentReference.getType() == EntityType.WIKI) {
            query.addFilter(this.topLevelPageFilter);
        } else {
            query.addFilter(this.childPageFilter);
            query.bindValue("parent", this.localEntityReferenceSerializer.serialize(parentReference.getParent()));
        }

        if (!areHiddenEntitiesShown()) {
            query.addFilter(this.hiddenPageFilter);
        }

        EntityReference parentSpaceReference = parentReference.extractReference(EntityType.SPACE);
        Set<String> excludedSpaces =
            getExcludedSpaces(parentSpaceReference != null ? parentSpaceReference : parentReference);
        if (!excludedSpaces.isEmpty()) {
            query.bindValue("excludedSpaces", excludedSpaces);
            query.addFilter(this.excludedSpaceFilter);
        }

        return query.addFilter(this.documentReferenceResolverFilter).execute();
    }

    @Override
    protected int getChildDocumentsCount(EntityReference parentReference) throws QueryException
    {
        int count = getChildSpacesCount(parentReference);
        if (canHaveTerminalChildDocuments(parentReference)) {
            count += getChildTerminalPagesCount(new DocumentReference(parentReference));
        }
        return count;
    }

    @Override
    protected boolean canHaveChildDocuments(EntityReference parentReference)
    {
        return parentReference.getType() == EntityType.WIKI || (parentReference.getType() == EntityType.DOCUMENT
            && getDefaultDocumentName().equals(parentReference.getName()));
    }

    private boolean canHaveTerminalChildDocuments(EntityReference parentReference)
    {
        return parentReference.getType() == EntityType.DOCUMENT && areTerminalDocumentsShown();
    }

    private int getChildTerminalPagesCount(DocumentReference documentReference) throws QueryException
    {
        List<String> constraints = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();

        // Exclude page translations.
        constraints.add("doc.translation = 0");

        // Include only the child pages.
        constraints.add("doc.space = :space");
        parameters.put("space", this.localEntityReferenceSerializer.serialize(documentReference.getParent()));

        // Include only the terminal pages.
        constraints.add("doc.name <> :defaultDocName");
        parameters.put("defaultDocName", getDefaultDocumentName());

        // Check for page exclusions.
        Set<String> excludedDocuments = getExcludedDocuments(documentReference.getParent());
        if (!excludedDocuments.isEmpty()) {
            constraints.add("doc.fullName not in (:excludedDocuments)");
            parameters.put(PARAMETER_EXCLUDED_DOCUMENTS, excludedDocuments);
        }

        Query query = this.queryManager.createQuery(whereClause(constraints), Query.HQL);
        query.setWiki(documentReference.getWikiReference().getName());
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.bindValue(entry.getKey(), entry.getValue());
        }

        query.addFilter(this.countQueryFilter);
        if (Boolean.TRUE.equals(getProperties().get("filterHiddenDocuments"))) {
            query.addFilter(this.hiddenDocumentQueryFilterProvider.get());
        }

        return ((Long) query.execute().get(0)).intValue();
    }
}
