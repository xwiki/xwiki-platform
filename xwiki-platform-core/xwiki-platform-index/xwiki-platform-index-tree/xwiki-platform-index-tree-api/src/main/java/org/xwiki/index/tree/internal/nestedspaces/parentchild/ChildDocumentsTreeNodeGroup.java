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
package org.xwiki.index.tree.internal.nestedspaces.parentchild;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractChildDocumentsTreeNodeGroup;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

/**
 * A tree node group that contains the child documents of a specified parent document.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component
@Named("childDocuments/parentChildOnNestedSpaces")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ChildDocumentsTreeNodeGroup extends AbstractChildDocumentsTreeNodeGroup
{
    @Inject
    @Named("count")
    protected QueryFilter countQueryFilter;

    @Inject
    @Named("hidden/document")
    protected Provider<QueryFilter> hiddenDocumentQueryFilterProvider;

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    /**
     * Default constructor.
     */
    public ChildDocumentsTreeNodeGroup()
    {
        super("childDocuments");
    }

    @Override
    protected List<DocumentReference> getChildDocuments(EntityReference parentReference, int offset, int limit)
        throws QueryException
    {
        Query query = getChildDocumentsQuery(new DocumentReference(parentReference));
        query.setOffset(offset);
        query.setLimit(limit);
        List<DocumentReference> documentReferences = new ArrayList<>();
        for (Object result : query.execute()) {
            documentReferences.add(this.explicitDocumentReferenceResolver.resolve((String) result, parentReference));
        }
        return documentReferences;
    }

    @Override
    protected int getChildDocumentsCount(EntityReference parentReference) throws QueryException
    {
        Query query = getChildDocumentsQuery(new DocumentReference(parentReference));
        query.addFilter(this.countQueryFilter);
        return ((Long) query.execute().get(0)).intValue();
    }

    private Query getChildDocumentsQuery(DocumentReference documentReference) throws QueryException
    {
        Query query = this.queryManager.createQuery(
            "where doc.translation = 0 and doc.space = :space and "
                + "doc.parent in (:absoluteRef, :localRef, :relativeRef) " + "order by lower(doc.name), doc.name",
            Query.HQL);
        query.bindValue("space", this.localEntityReferenceSerializer.serialize(documentReference.getParent()));
        query.bindValue("absoluteRef", this.defaultEntityReferenceSerializer.serialize(documentReference));
        query.bindValue("localRef", this.localEntityReferenceSerializer.serialize(documentReference));
        query.bindValue("relativeRef",
            this.compactEntityReferenceSerializer.serialize(documentReference, documentReference.getParent()));
        query.setWiki(documentReference.getWikiReference().getName());
        if (Boolean.TRUE.equals(getProperties().get("filterHiddenDocuments"))) {
            query.addFilter(this.hiddenDocumentQueryFilterProvider.get());
        }
        return query;
    }

    @Override
    protected boolean canHaveChildDocuments(EntityReference parentReference)
    {
        return parentReference.getType() == EntityType.DOCUMENT;
    }
}
