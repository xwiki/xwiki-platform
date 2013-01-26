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
package org.xwiki.search.solr.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.internal.api.SolrIndex;
import org.xwiki.search.solr.internal.api.SolrIndexException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of the index.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class DefaultSolrIndex implements SolrIndex
{
    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    /**
     * Reference to String serializer.
     */
    @Inject
    protected EntityReferenceSerializer<String> serializer;

    /**
     * Communication with the Solr instance.
     */
    @Inject
    protected Provider<SolrInstance> solrInstanceProvider;

    /**
     * Execution component.
     */
    @Inject
    protected Execution execution;

    // /**
    // * XWikiStubContextProvider component.
    // */
    // @Inject
    // protected XWikiStubContextProvider contextProvider;

    /**
     * Component manager used to get metadata extractors.
     */
    @Inject
    protected ComponentManager componentManager;

    /**
     * Extract contained indexable references.
     */
    @Inject
    protected IndexableReferenceExtractor indexableReferenceExtractor;

    @Override
    public void index(EntityReference reference) throws SolrIndexException
    {
        index(Arrays.asList(reference));
    }

    @Override
    public void index(List<EntityReference> references) throws SolrIndexException
    {
        // Build the list of references to index directly
        List<EntityReference> indexableReferences = getUniqueIndexableEntityReferences(references);

        // Build the list of Solr input documents to be indexed.
        List<SolrInputDocument> solrDocuments = getSolrDocuments(indexableReferences);

        // Push and commit the new index data to the server.
        SolrInstance solrInstance = solrInstanceProvider.get();
        try {
            solrInstance.add(solrDocuments);
            solrInstance.commit();
        } catch (Exception e) {
            String message = "Failed to push index changes to the Solr server. Rolling back.";
            logger.error(message, e);
            try {
                solrInstance.rollback();
            } catch (Exception ex) {
                // Just log the failure.
                logger.error("Failed to rollback index changes.", ex);
            }

            // Throw the main exception onwards.
            throw new SolrIndexException(message, e);
        }
    }

    /**
     * @param startReferences the references from where to start the search from.
     * @return the unique list of indexable references starting from each of the input start references.
     * @throws SolrIndexException if problems occur.
     */
    protected List<EntityReference> getUniqueIndexableEntityReferences(List<EntityReference> startReferences)
        throws SolrIndexException
    {
        List<EntityReference> result = new ArrayList<EntityReference>();

        for (EntityReference reference : startReferences) {
            // Avoid duplicates
            if (result.contains(reference)) {
                continue;
            }

            List<EntityReference> containedReferences = indexableReferenceExtractor.getReferences(reference);
            for (EntityReference containedReference : containedReferences) {
                // Avoid duplicates again
                if (result.contains(containedReference)) {
                    continue;
                }

                result.add(containedReference);
            }
        }

        return result;
    }

    /**
     * @param references the references to extract metadata from. Unsupported references are skipped.
     * @return the list of {@link SolrInputDocument}s containing extracted metadata from each of the passed references.
     * @throws SolrIndexException if problems occur.
     * @throws IllegalArgumentException if there is an incompatibility between a reference and the assigned extractor.
     */
    private List<SolrInputDocument> getSolrDocuments(List<EntityReference> references) throws SolrIndexException,
        IllegalArgumentException
    {
        List<SolrInputDocument> solrDocuments = new ArrayList<SolrInputDocument>();
        for (EntityReference reference : references) {
            SolrMetadataExtractor metadataExtractor = getMetadataExtractor(reference.getType());
            // If the entity type is supported, use the extractor to get the SolrInputDocuent.
            if (metadataExtractor != null) {
                SolrInputDocument entitySolrDocument = metadataExtractor.getSolrDocument(reference);
                if (entitySolrDocument != null) {
                    solrDocuments.add(entitySolrDocument);
                }
            }
        }
        return solrDocuments;
    }

    /**
     * @param entityType the entity type
     * @return the metadata extractor that is registered for the specified type or {@code null} if none exists.
     */
    protected SolrMetadataExtractor getMetadataExtractor(EntityType entityType)
    {
        SolrMetadataExtractor result = null;
        try {
            result = componentManager.getInstance(SolrMetadataExtractor.class, entityType.name().toLowerCase());
        } catch (ComponentLookupException e) {
            // Entity type not supported.
        }

        return result;
    }

    @Override
    public void delete(EntityReference reference) throws SolrIndexException
    {
        delete(Arrays.asList(reference));
    }

    @Override
    public void delete(List<EntityReference> references) throws SolrIndexException
    {
        // Preserve consistency by deleting all the indexable entities contained by each input reference.
        List<EntityReference> indexableReferences = getUniqueIndexableEntityReferences(references);

        // Get the IDs of all the references to delete.
        List<String> ids = getIds(indexableReferences);

        // Push and commit the index IDs to delete from the Solr server.
        SolrInstance solrInstance = solrInstanceProvider.get();
        try {
            solrInstance.delete(ids);
            solrInstance.commit();
        } catch (Exception e) {
            String message = "Failed to push index deletions to the Solr server. Rolling back.";
            logger.error(message, e);
            try {
                solrInstance.rollback();
            } catch (Exception re) {
                // Just log the failure.
                logger.error("Failed to rollback index deletions.", re);
            }

            // Throw the main exception onwards.
            throw new SolrIndexException(message, e);
        }
    }

    /**
     * @return the XWikiContext
     */
    protected XWikiContext getXWikiContext()
    {
        ExecutionContext executionContext = this.execution.getContext();
        XWikiContext context = (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        // FIXME: Do we need this? Maybe when running an index Thread?
        // if (context == null) {
        // context = this.contextProvider.createStubContext();
        // executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, context);
        // }
        return context;
    }

    /**
     * @param references the references.
     * @return the IDs of the entities, as they are used in the index.
     * @throws SolrIndexException if problems occur.
     */
    protected List<String> getIds(List<EntityReference> references) throws SolrIndexException
    {
        List<String> result = new ArrayList<String>();
        for (EntityReference reference : references) {
            SolrMetadataExtractor metadataExtractor = getMetadataExtractor(reference.getType());
            if (metadataExtractor != null) {
                String id = metadataExtractor.getId(reference);
                result.add(id);
            }
        }

        return result;
    }
}
