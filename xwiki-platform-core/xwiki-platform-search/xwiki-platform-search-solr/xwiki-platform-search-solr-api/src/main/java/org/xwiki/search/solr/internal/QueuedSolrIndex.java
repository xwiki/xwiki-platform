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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndex;
import org.xwiki.search.solr.internal.api.SolrIndexException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;

import com.xpn.xwiki.util.AbstractXWikiRunnable;

/**
 * The {@link Runnable} {@link SolrIndex} implementation that is executed inside a thread launched by the default
 * {@link SolrIndex} implementation.
 * <p/>
 * The {@link QueuedSolrIndex} expects that the references it receives are already expanded (they are "leaf-references")
 * as opposed to the default implementation that performs expansion (using {@link IndexableReferenceExtractor}) before
 * delegating to the {@link QueuedSolrIndex}.
 * <p/>
 * This implementation does not directly process the given leaf-references, but adds them to a processing queue, in the
 * order they were received. The {@link Runnable} part of this implementation is the one that sequentially reads and
 * processes the queue.
 * 
 * @version $Id$
 */
@Component
@Named("queued")
public class QueuedSolrIndex extends AbstractXWikiRunnable implements SolrIndex, Initializable
{
    /**
     * TODO DOCUMENT ME!
     */
    public static final String SOLR_THREAD_BATCH_SIZE_PROPERTY = "solr.thread.batchSize";

    /**
     * TODO DOCUMENT ME!
     */
    public static final int DEFAULT_BATCH_SIZE = 50;

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Component manager used to get metadata extractors.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The Solr configuration source.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * Communication with the Solr instance.
     */
    @Inject
    private Provider<SolrInstance> solrInstanceProvider;

    /**
     * The queue of index operation to perform.
     */
    private BlockingQueue<Entry<EntityReference, IndexOperation>> indexOperationsQueue;

    /**
     * The maximum number of operations to treat as a batch before committing the changes to the Solr server.
     */
    private int batchSize;

    @Override
    public void initialize() throws InitializationException
    {
        // Initialize the queue
        indexOperationsQueue = new LinkedBlockingQueue<Entry<EntityReference, IndexOperation>>();

        // Read the batchSize from the xwiki.properties file
        try {
            this.batchSize = configuration.getProperty(SOLR_THREAD_BATCH_SIZE_PROPERTY, DEFAULT_BATCH_SIZE);
        } catch (Exception e) {
            logger.error("Failed to read the queue batch size. Using the default.", e);
        }
    }

    @Override
    protected void runInternal()
    {
        while (!Thread.currentThread().interrupted()) {
            // Block until there is at least one entry in the queue
            Entry<EntityReference, IndexOperation> queueEntry = null;
            try {
                queueEntry = indexOperationsQueue.take();
            } catch (InterruptedException e) {
                // Stop and pass the interrupt to the current thread.
                Thread.currentThread().interrupt();
                return;
            }

            // Add the entry to the batch.
            List<Entry<EntityReference, IndexOperation>> batchList =
                new ArrayList<Entry<EntityReference, IndexOperation>>();
            batchList.add(queueEntry);

            // Add to the batch until either the batch size is achieved or the queue gets emptied
            int tempBatchSize = this.batchSize - 1;
            while (!indexOperationsQueue.isEmpty() && tempBatchSize > 0) {
                queueEntry = indexOperationsQueue.poll();
                batchList.add(queueEntry);
                tempBatchSize--;
            }

            // Process the current batch
            processBatch(batchList);
        }
    }

    /**
     * Process a batch of operations that were just read from the index operations queue. This method also commits the
     * batch when it finishes to process it.
     * 
     * @param batchList the batch to process
     */
    private void processBatch(List<Entry<EntityReference, IndexOperation>> batchList)
    {
        SolrInstance solrInstance = solrInstanceProvider.get();

        // To improve performance, we group contiguous index or delete operations and issue them only when a
        // different type of operation is encountered.
        List<SolrInputDocument> solrDocumentsToAdd = new ArrayList<SolrInputDocument>();
        List<String> solrDocumentIDsToDelete = new ArrayList<String>();
        IndexOperation previousOperation = null;

        for (Entry<EntityReference, IndexOperation> batchEntry : batchList) {
            EntityReference reference = batchEntry.getKey();
            IndexOperation operation = batchEntry.getValue();

            try {
                // Issue add/delete operations to the server in batches whenever the contiguity stops
                if (previousOperation != null && !operation.equals(previousOperation)) {
                    if (IndexOperation.INDEX.equals(previousOperation)) {
                        solrInstance.add(solrDocumentsToAdd);

                        // Clear the just processed contiguous inner-batch.
                        solrDocumentsToAdd.clear();
                    } else if (IndexOperation.DELETE.equals(previousOperation)) {
                        solrInstance.delete(solrDocumentIDsToDelete);

                        // Clear the just processed contiguous inner-batch.
                        solrDocumentIDsToDelete.clear();
                    }

                    // Start of a new contiguous operation batch.
                }

                // For the current contiguous operations queue, group the changes
                if (IndexOperation.INDEX.equals(operation)) {
                    SolrInputDocument solrDocument = getSolrDocument(reference);
                    if (solrDocument != null) {
                        solrDocumentsToAdd.add(solrDocument);
                    }
                } else if (IndexOperation.DELETE.equals(operation)) {
                    String id = getId(reference);
                    solrDocumentIDsToDelete.add(id);
                }
            } catch (Exception e) {
                logger.error("Failed to process entity [{}] for the [{}] operation", reference, operation, e);
            }
        }

        // Commit the index changes so that they become available to queries. This is a costly operation and that is
        // the reason why we perform it at the end of the batch.
        try {
            solrInstance.commit();
        } catch (Exception e) {
            logger.error("Failed to commit index changes to the Solr server. Rolling back.", e);
            try {
                solrInstance.rollback();
            } catch (Exception ex) {
                // Just log the failure.
                logger.error("Failed to rollback index changes.", ex);
            }
        }
    }

    /**
     * @param reference the reference to extract metadata from.
     * @return the {@link SolrInputDocument} containing extracted metadata from the passed reference; {@code null} if
     *         the reference type is not supported.
     * @throws SolrIndexException if problems occur.
     * @throws IllegalArgumentException if there is an incompatibility between a reference and the assigned extractor.
     */
    private SolrInputDocument getSolrDocument(EntityReference reference) throws SolrIndexException,
        IllegalArgumentException
    {
        SolrInputDocument solrDocument = null;
        SolrMetadataExtractor metadataExtractor = getMetadataExtractor(reference.getType());
        // If the entity type is supported, use the extractor to get the SolrInputDocuent.
        if (metadataExtractor != null) {
            solrDocument = metadataExtractor.getSolrDocument(reference);
        }

        return solrDocument;
    }

    /**
     * @param entityType the entity type
     * @return the metadata extractor that is registered for the specified type or {@code null} if none exists.
     */
    private SolrMetadataExtractor getMetadataExtractor(EntityType entityType)
    {
        SolrMetadataExtractor result = null;
        try {
            result = componentManager.getInstance(SolrMetadataExtractor.class, entityType.name().toLowerCase());
        } catch (ComponentLookupException e) {
            logger.warn("Unsupported entity type: [{}]", entityType.toString(), e);
        }

        return result;
    }

    /**
     * @param reference the reference for which to extract the ID
     * @return the ID of the entity, as it is used in the index
     * @throws SolrIndexException if problems occur
     */
    private String getId(EntityReference reference) throws SolrIndexException
    {
        String result = null;

        SolrMetadataExtractor metadataExtractor = getMetadataExtractor(reference.getType());
        if (metadataExtractor != null) {
            result = metadataExtractor.getId(reference);
        }

        return result;
    }

    @Override
    public void index(EntityReference reference) throws SolrIndexException
    {
        this.index(Arrays.asList(reference));
    }

    @Override
    public void index(List<EntityReference> references) throws SolrIndexException
    {
        addToQueue(references, IndexOperation.INDEX);
    }

    @Override
    public void delete(EntityReference reference) throws SolrIndexException
    {
        this.delete(Arrays.asList(reference));

    }

    @Override
    public void delete(List<EntityReference> references) throws SolrIndexException
    {
        addToQueue(references, IndexOperation.DELETE);
    }

    /**
     * Add a list of references to the index queue, all having the same operation.
     * 
     * @param references the references to add
     * @param operation the operation to assign to the given references
     */
    public void addToQueue(List<EntityReference> references, IndexOperation operation)
    {
        for (EntityReference reference : references) {
            Entry<EntityReference, IndexOperation> queueEntry =
                new SimpleEntry<EntityReference, IndexOperation>(reference, operation);
            indexOperationsQueue.add(queueEntry);
        }
    }
}
