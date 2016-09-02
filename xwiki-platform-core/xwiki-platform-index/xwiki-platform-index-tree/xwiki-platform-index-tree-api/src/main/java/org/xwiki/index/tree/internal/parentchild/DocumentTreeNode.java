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
package org.xwiki.index.tree.internal.parentchild;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The document tree node for the (deprecated) parent-child hierarchy.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Component
@Named("document/parentChild")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentTreeNode extends org.xwiki.index.tree.internal.nestedpages.DocumentTreeNode
{
    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    private DocumentQueryHelper documentQueryHelper;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    protected List<DocumentReference> getChildDocuments(DocumentReference documentReference, int offset, int limit)
        throws QueryException
    {
        return this.documentQueryHelper.resolve(getChildrenQuery(new DocumentReference(documentReference)), offset,
            limit, documentReference);
    }

    private Query getChildrenQuery(DocumentReference parentReference) throws QueryException
    {
        List<String> constraints = new ArrayList<String>();
        constraints.add("doc.translation = 0");
        constraints
            .add("(doc.parent in (:absoluteRef, :localRef) or (doc.space = :space and doc.parent = :relativeRef))");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("space", this.localEntityReferenceSerializer.serialize(parentReference.getParent()));
        parameters.put("absoluteRef", this.defaultEntityReferenceSerializer.serialize(parentReference));
        parameters.put("localRef", this.localEntityReferenceSerializer.serialize(parentReference));
        parameters.put("relativeRef",
            this.compactEntityReferenceSerializer.serialize(parentReference, parentReference.getParent()));
        Query query = this.documentQueryHelper.getQuery(constraints, parameters, getProperties());
        query.setWiki(parentReference.getWikiReference().getName());
        return query;
    }

    @Override
    protected int getChildDocumentsCount(DocumentReference documentReference) throws QueryException
    {
        Query query = getChildrenQuery(documentReference);
        query.addFilter(this.countQueryFilter);
        return ((Long) query.execute().get(0)).intValue();
    }

    @Override
    protected EntityReference getParent(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        DocumentReference parentReference = document.getParentReference();
        // The parent document must be on the same wiki.
        if (parentReference == null
            || !parentReference.getWikiReference().equals(documentReference.getWikiReference())) {
            return documentReference.getWikiReference();
        }
        return parentReference;
    }
}
