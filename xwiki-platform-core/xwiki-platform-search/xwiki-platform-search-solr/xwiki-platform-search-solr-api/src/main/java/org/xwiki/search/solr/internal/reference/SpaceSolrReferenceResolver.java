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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.search.solr.internal.api.FieldUtils;
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
public class SpaceSolrReferenceResolver extends AbstractSolrReferenceResolver
{
    /**
     * Used to resolve document references.
     */
    @Inject
    @Named("document")
    private Provider<SolrReferenceResolver> documentResolverProvider;

    /**
     * Used to resolve document references.
     */
    @Inject
    @Named("wiki")
    private Provider<SolrReferenceResolver> wikiResolverProvider;

    /**
     * Query manager used to perform queries on the XWiki model.
     */
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Override
    public List<EntityReference> getReferences(EntityReference spaceReference) throws SolrIndexerException
    {
        List<EntityReference> result = new ArrayList<EntityReference>();
        EntityReference wikiReference = spaceReference.extractReference(EntityType.WIKI);
        String localSpaceReference = this.localEntityReferenceSerializer.serialize(spaceReference);

        // Ignore the space reference because it is not indexable.

        // Make sure the list of spaces is from the requested wiki
        List<String> documentNames;
        try {
            documentNames =
                this.queryManager.getNamedQuery("getSpaceDocsName").setWiki(wikiReference.getName())
                    .bindValue("space", localSpaceReference).execute();
        } catch (QueryException e) {
            throw new SolrIndexerException("Failed to query space [" + spaceReference + "] documents", e);
        }

        for (String documentName : documentNames) {
            EntityReference documentReference = new EntityReference(documentName, EntityType.DOCUMENT, spaceReference);

            try {
                this.documentResolverProvider.get().getReferences(documentReference).forEach(result::add);
            } catch (Exception e) {
                this.logger.error("Failed to resolve references for document [" + documentReference + "]", e);
            }
        }

        return result;
    }

    @Override
    public String getQuery(EntityReference reference) throws SolrIndexerException
    {
        StringBuilder builder = new StringBuilder();

        EntityReference wikiReference = reference.extractReference(EntityType.WIKI);
        builder.append(wikiResolverProvider.get().getQuery(wikiReference));

        builder.append(QUERY_AND);

        builder.append(FieldUtils.SPACE_EXACT);
        builder.append(':');
        String localSpaceReference = this.localEntityReferenceSerializer.serialize(reference);
        builder.append(ClientUtils.escapeQueryChars(localSpaceReference));

        return builder.toString();
    }
}
