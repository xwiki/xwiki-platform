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
package org.xwiki.search.solr.internal.reference;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.search.solr.internal.api.SolrIndexerException;

/**
 * Resolve space references.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("space")
@Singleton
public class SpaceSolrDocumentReferenceResolver extends AbstractSolrDocumentReferenceResolver
{
    /**
     * Used to resolve document references.
     */
    @Inject
    @Named("document")
    private SolrDocumentReferenceResolver documentResolver;

    /**
     * Query manager used to perform queries on the XWiki model.
     */
    @Inject
    private QueryManager queryManager;

    @Override
    public List<EntityReference> getReferences(EntityReference spaceReference) throws SolrIndexerException
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        // Ignore the space reference because it is not indexable.

        // Make sure the list of spaces is from the requested wiki
        List<String> documentNames;
        try {
            documentNames =
                this.queryManager.getNamedQuery("getSpaceDocsName").setWiki(spaceReference.getParent().getName())
                    .bindValue("space", spaceReference.getName()).execute();
        } catch (QueryException e) {
            throw new SolrIndexerException("Failed to query space [" + spaceReference + "] documents", e);
        }

        for (String documentName : documentNames) {
            EntityReference documentReference = new EntityReference(documentName, EntityType.DOCUMENT, spaceReference);

            try {
                result.addAll(this.documentResolver.getReferences(documentReference));
            } catch (Exception e) {
                this.logger.error("Failed to resolve references for document [" + documentReference + "]", e);
            }
        }

        return result;
    }
}
