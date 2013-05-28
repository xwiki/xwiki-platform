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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrIndexException;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;

import com.xpn.xwiki.util.AbstractXWikiRunnable;

/**
 * Default implementation of {@link SolrIndexer}.
 * <p/>
 * This implementation does not directly process the given leaf-references, but adds them to a processing queue, in the
 * order they were received. The {@link Runnable} part of this implementation is the one that sequentially reads and
 * processes the queue.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Singleton
public class DefaultSolrIndexer extends AbstractXWikiRunnable implements SolrIndexer, Initializable, Disposable
{
    /**
     * Index queue entry.
     * 
     * @version $Id$
     */
    private static class IndexQueueEntry
    {
        /**
         * The reference of the entity to index.
         */
        public EntityReference reference;

        /**
         * The indexing operation to perform.
         */
        public IndexOperation operation;

        /**
         * @param reference the reference of the entity to index.
         * @param operation the indexing operation to perform.
         */
        public IndexQueueEntry(EntityReference reference, IndexOperation operation)
        {
            this.reference = reference;
            this.operation = operation;
        }
    }

    /**
     * Stop indexing thread.
     */
    private static final IndexQueueEntry STOP = new IndexQueueEntry(null, IndexOperation.STOP);

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
    private SolrConfiguration configuration;

    /**
     * Communication with the Solr instance.
     */
    @Inject
    private SolrInstance solrInstanceProvider;

    /**
     * Extract contained indexable references.
     */
    @Inject
    private IndexableReferenceExtractor indexableReferenceExtractor;

    /**
     * The queue of index operation to perform.
     */
    private BlockingQueue<IndexQueueEntry> indexOperationsQueue;

    /**
     * Thread in which the indexUpdater will be executed.
     */
    private Thread indexThread;

    /**
     * Indicate of the component has been disposed.
     */
    private boolean disposed;

    @Override
    public void initialize() throws InitializationException
    {
        // Launch the index thread that runs the indexUpdater.
        this.indexThread = new Thread(this);
        this.indexThread.start();
        this.indexThread.setPriority(Thread.NORM_PRIORITY - 1);

        // Initialize the queue
        this.indexOperationsQueue =
            new LinkedBlockingQueue<IndexQueueEntry>(this.configuration.getIndexerQueueCapacity());
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // Mark the component as disposed
        this.disposed = true;

        // Empty the queue
        this.indexOperationsQueue.clear();

        this.indexOperationsQueue.add(STOP);

        // Wait until the thread actually die
        try {
            this.indexThread.join();
        } catch (InterruptedException e) {
            this.logger.error("The index thread was unexpectedly interrupted", e);
        }
    }

    @Override
    protected void runInternal()
    {
        this.logger.debug("Start indexer thread");

        while (!Thread.interrupted()) {
            // Block until there is at least one entry in the queue
            IndexQueueEntry queueEntry = null;
            try {
                queueEntry = this.indexOperationsQueue.take();
            } catch (InterruptedException e) {
                this.logger.warn("The thread has been interrupted", e);

                queueEntry = STOP;
            }

            if (queueEntry == STOP) {
                break;
            }

            // Add to the batch until either the batch size is achieved or the queue gets emptied
            List<IndexQueueEntry> batchList = new ArrayList<IndexQueueEntry>();

            int tempBatchSize = this.configuration.getIndexerBatchSize() - 1;
            do {
                // Add the entry to the batch.
                batchList.add(queueEntry);

                tempBatchSize--;

                if (tempBatchSize > 0) {
                    queueEntry = this.indexOperationsQueue.poll();
                } else {
                    queueEntry = null;
                }
            } while (queueEntry != null);

            // Process the current batch
            processBatch(batchList);
        }

        this.logger.debug("Stop indexer thread");
    }

    /**
     * Process a batch of operations that were just read from the index operations queue. This method also commits the
     * batch when it finishes to process it.
     * 
     * @param batchList the batch to process
     */
    private void processBatch(List<IndexQueueEntry> batchList)
    {
        // To improve performance, we group contiguous index or delete operations and issue them only when a
        // different type of operation is encountered.
        List<SolrInputDocument> solrDocumentsToIndex = new ArrayList<SolrInputDocument>();
        List<String> solrDocumentIDsToDelete = new ArrayList<String>();

        IndexQueueEntry previousBatchEntry = null;

        for (IndexQueueEntry batchEntry : batchList) {
            EntityReference reference = batchEntry.reference;
            IndexOperation operation = batchEntry.operation;

            try {
                // Issue add/delete operations to the server in batches whenever the contiguity stops
                checkContiguity(previousBatchEntry, operation, solrDocumentsToIndex, solrDocumentIDsToDelete);

                // For the current contiguous operations queue, group the changes
                if (IndexOperation.INDEX.equals(operation)) {
                    SolrInputDocument solrDocument = getSolrDocument(reference);
                    if (solrDocument != null) {
                        solrDocumentsToIndex.add(solrDocument);
                    }
                } else if (IndexOperation.DELETE.equals(operation)) {
                    String id = getId(reference);
                    solrDocumentIDsToDelete.add(id);
                }
            } catch (Exception e) {
                this.logger.error("Failed to process entity [{}] for the [{}] operation", reference, operation, e);
            }

            previousBatchEntry = batchEntry;
        }

        // Commit the index changes so that they become available to queries. This is a costly operation and that is
        // the reason why we perform it at the end of the batch.
        try {
            this.solrInstanceProvider.commit();
        } catch (Exception e) {
            this.logger.error("Failed to commit index changes to the Solr server. Rolling back.", e);

            try {
                this.solrInstanceProvider.rollback();
            } catch (Exception ex) {
                // Just log the failure.
                this.logger.error("Failed to rollback index changes.", ex);
            }
        }
    }

    /**
     * @param previousBatchEntry the previous batch entry
     * @param operation the current operation
     * @param solrDocumentsToIndex the documents stored for {@link IndexOperation#INDEX}
     * @param solrDocumentIDsToDelete the documents stored for {@link IndexOperation#DELETE}
     * @throws SolrServerException when fail to apply operation
     * @throws IOException when fail to apply operation
     */
    private void checkContiguity(IndexQueueEntry previousBatchEntry, IndexOperation operation,
        List<SolrInputDocument> solrDocumentsToIndex, List<String> solrDocumentIDsToDelete) throws SolrServerException,
        IOException
    {
        if (previousBatchEntry != null) {
            IndexOperation previousOperation = previousBatchEntry.operation;

            // 1) If previous operation is different
            // 2) If previous entry is an attachment send it right away to be safer regarding memory
            if (operation != previousOperation || previousBatchEntry.reference.getType() == EntityType.ATTACHMENT) {
                if (IndexOperation.INDEX.equals(previousOperation)) {
                    this.solrInstanceProvider.add(solrDocumentsToIndex);

                    // Clear the just processed contiguous inner-batch.
                    solrDocumentsToIndex.clear();
                } else if (IndexOperation.DELETE.equals(previousOperation)) {
                    this.solrInstanceProvider.delete(solrDocumentIDsToDelete);

                    // Clear the just processed contiguous inner-batch.
                    solrDocumentIDsToDelete.clear();
                }
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
            result = this.componentManager.getInstance(SolrMetadataExtractor.class, entityType.name().toLowerCase());
        } catch (ComponentLookupException e) {
            this.logger.warn("Unsupported entity type: [{}]", entityType.toString(), e);
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
     * @throws SolrIndexException when failed to resolve passed references
     */
    private void addToQueue(List<EntityReference> references, IndexOperation operation) throws SolrIndexException
    {
        if (!this.disposed) {
            // Build the list of references to index directly
            List<EntityReference> indexableReferences = getUniqueIndexableEntityReferences(references);

            for (EntityReference reference : indexableReferences) {
                this.indexOperationsQueue.add(new IndexQueueEntry(reference, operation));
            }
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

            List<EntityReference> containedReferences = this.indexableReferenceExtractor.getReferences(reference);
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
}
