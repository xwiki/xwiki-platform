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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The document node in the parent-child over nested spaces hierarchy.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("document/parentChildOnNestedSpaces")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentTreeNode extends org.xwiki.index.tree.internal.nestedpages.DocumentTreeNode
{
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    protected List<DocumentReference> getChildDocuments(DocumentReference documentReference, int offset, int limit)
        throws QueryException
    {
        Query query = getChildDocumentsQuery(documentReference);
        query.setOffset(offset);
        query.setLimit(limit);
        List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
        for (Object result : query.execute()) {
            documentReferences.add(this.explicitDocumentReferenceResolver.resolve((String) result, documentReference));
        }
        return documentReferences;
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
    protected int getChildDocumentsCount(DocumentReference documentReference) throws QueryException
    {
        Query query = getChildDocumentsQuery(documentReference);
        query.addFilter(this.countQueryFilter);
        return ((Long) query.execute().get(0)).intValue();
    }

    @Override
    protected EntityReference getParent(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        DocumentReference parentReference = document.getParentReference();
        // The parent document must be on the same space.
        if (parentReference != null && parentReference.getParent().equals(documentReference.getParent())) {
            return parentReference;
        }
        return documentReference.getParent();
    }
}
