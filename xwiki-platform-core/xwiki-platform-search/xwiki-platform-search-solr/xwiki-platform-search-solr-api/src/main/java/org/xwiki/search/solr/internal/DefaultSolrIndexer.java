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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

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
import org.xwiki.search.solr.internal.metadata.LengthSolrInputDocument;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;
import org.xwiki.search.solr.internal.reference.SolrDocumentReferenceResolver;

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
     * Extract children references from passed references and dispatch them to the index queue.
     * 
     * @version $Id$
     */
    private class Resolver extends AbstractXWikiRunnable
    {
        @Override
        public void runInternal()
        {
            logger.debug("Start SOLR resolver thread");

            while (!Thread.interrupted()) {
                IndexQueueEntry queueEntry;
                try {
                    queueEntry = resolveQueue.take();
                } catch (InterruptedException e) {
                    logger.warn("The SOLR resolve thread has been interrupted", e);

                    queueEntry = QUEUE_ENTRY_STOP;
                }

                if (queueEntry == QUEUE_ENTRY_STOP) {
                    break;
                }

                try {
                    // FIXME: it's not very clean to load all the reference in memory in the case of the wiki for
                    // example. Would be better to stream or cut that a bit instead.
                    List<EntityReference> references = solrDocumentRefereceResolver.getReferences(queueEntry.reference);

                    for (EntityReference reference : references) {
                        indexQueue.offer(new IndexQueueEntry(reference, queueEntry.operation));
                    }
                } catch (Throwable e) {
                    logger.warn("Failed to index root reference [{}]", queueEntry.reference, e);
                }
            }

            logger.debug("Stop SOLR resolver thread");
        }
    }

    /**
     * Stop indexing thread.
     */
    private static final IndexQueueEntry QUEUE_ENTRY_STOP = new IndexQueueEntry(null, IndexOperation.STOP);

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
    private Provider<SolrInstance> solrInstanceProvider;

    /**
     * Extract contained indexable references.
     */
    @Inject
    private SolrDocumentReferenceResolver solrDocumentRefereceResolver;

    /**
     * The queue of index operation to perform.
     */
    private BlockingQueue<IndexQueueEntry> indexQueue;

    /**
     * The queue of resolve references and add them to the index queue.
     */
    private BlockingQueue<IndexQueueEntry> resolveQueue;

    /**
     * Thread in which the indexUpdater will be executed.
     */
    private Thread indexThread;

    /**
     * Thread in which the provided references children will be resolved.
     */
    private Thread resolveThread;

    /**
     * Indicate of the component has been disposed.
     */
    private boolean disposed;

    @Override
    public void initialize() throws InitializationException
    {
        // Launch the resolve thread that runs the indexUpdater.
        this.resolveThread = new Thread(new Resolver());
        this.resolveThread.start();
        this.resolveThread.setPriority(Thread.NORM_PRIORITY - 1);

        // Launch the index thread that runs the indexUpdater.
        this.indexThread = new Thread(this);
        this.indexThread.start();
        this.indexThread.setPriority(Thread.NORM_PRIORITY - 1);

        // Initialize the queue
        this.resolveQueue = new LinkedBlockingQueue<IndexQueueEntry>();
        this.indexQueue = new LinkedBlockingQueue<IndexQueueEntry>(this.configuration.getIndexerQueueCapacity());
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // Mark the component as disposed
        this.disposed = true;

        // Empty the queue
        this.indexQueue.clear();

        this.indexQueue.add(QUEUE_ENTRY_STOP);

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
        this.logger.debug("Start SOLR indexer thread");

        while (!Thread.interrupted()) {
            // Block until there is at least one entry in the queue
            IndexQueueEntry queueEntry = null;
            try {
                queueEntry = this.indexQueue.take();
            } catch (InterruptedException e) {
                this.logger.warn("The SOLR index thread has been interrupted", e);

                queueEntry = QUEUE_ENTRY_STOP;
            }

            if (queueEntry == QUEUE_ENTRY_STOP) {
                break;
            }

            // Add to the batch until either the batch size is achieved or the queue gets emptied
            processBatch(queueEntry);
        }

        this.logger.debug("Stop SOLR indexer thread");
    }

    /**
     * Process a batch of operations that were just read from the index operations queue. This method also commits the
     * batch when it finishes to process it.
     * 
     * @param queueEntry the batch to process
     */
    private void processBatch(IndexQueueEntry queueEntry)
    {
        // To improve performance, we group contiguous index or delete operations and issue them only when a
        // different type of operation is encountered.
        List<SolrInputDocument> documentsToIndex = new LinkedList<SolrInputDocument>();
        List<String> documentIDsToDelete = new LinkedList<String>();

        IndexQueueEntry batchEntry = queueEntry;
        IndexQueueEntry previousBatchEntry = null;

        int length = 0;
        int size = 0;

        for (; batchEntry != null; previousBatchEntry = batchEntry, batchEntry = this.indexQueue.poll()) {
            EntityReference reference = batchEntry.reference;
            IndexOperation operation = batchEntry.operation;

            // Issue add/delete operations to the server in batches whenever the contiguity stops
            checkContiguity(previousBatchEntry, operation, documentsToIndex, documentIDsToDelete);

            // For the current contiguous operations queue, group the changes
            try {
                if (IndexOperation.INDEX.equals(operation)) {
                    LengthSolrInputDocument solrDocument = getSolrDocument(reference);
                    if (solrDocument != null) {
                        documentsToIndex.add(solrDocument);
                        length += solrDocument.getLength();
                        ++size;
                    }
                } else if (IndexOperation.DELETE.equals(operation)) {
                    String id = getId(reference);
                    documentIDsToDelete.add(id);
                    ++size;
                }
            } catch (Exception e) {
                this.logger.error("Failed to process entity [{}] for the [{}] operation", reference, operation, e);
            }

            // Commit the index changes so that they become available to queries. This is a costly operation and that is
            // the reason why we perform it at the end of the batch.
            if (shouldCommit(length, size)) {
                flush(documentsToIndex, documentIDsToDelete);
                commit();
                length = 0;
                size = 0;
            }
        }

        // Commit what's left
        if (size > 0) {
            flush(documentsToIndex, documentIDsToDelete);
            commit();
        }
    }

    /**
     * Commit.
     */
    private void commit()
    {
        SolrInstance solrInstance = this.solrInstanceProvider.get();

        try {
            solrInstance.commit();
        } catch (Exception e) {
            this.logger.error("Failed to commit index changes to the Solr server. Rolling back.", e);

            try {
                solrInstance.rollback();
            } catch (Exception ex) {
                // Just log the failure.
                this.logger.error("Failed to rollback index changes.", ex);
            }
        }
    }

    /**
     * Check various constraints to know if the batch should be committed.
     * 
     * @param length the current length
     * @param size the current size
     * @return true if the batch should be sent
     */
    private boolean shouldCommit(int length, int size)
    {
        // If the length is above the configured maximum
        if (length >= this.configuration.getIndexerBatchMaxLengh()) {
            return true;
        }

        // If the size is above the configured maximum
        return size >= this.configuration.getIndexerBatchSize();
    }

    /**
     * @param previousBatchEntry the previous batch entry
     * @param operation the current operation
     * @param documentsToIndex the documents stored for {@link IndexOperation#INDEX}
     * @param documentIDsToDelete the documents stored for {@link IndexOperation#DELETE}
     */
    private void checkContiguity(IndexQueueEntry previousBatchEntry, IndexOperation operation,
        List<SolrInputDocument> documentsToIndex, List<String> documentIDsToDelete)
    {
        if (previousBatchEntry != null) {
            IndexOperation previousOperation = previousBatchEntry.operation;

            if (previousOperation != operation) {
                flush(documentsToIndex, documentIDsToDelete);
            }
        }
    }

    /**
     * @param documentsToIndex the list of document to index
     * @param documentIDsToDelete the list of documents to remove from the index
     */
    private void flush(List<SolrInputDocument> documentsToIndex, List<String> documentIDsToDelete)
    {
        try {
            SolrInstance solrInstance = this.solrInstanceProvider.get();

            if (!documentsToIndex.isEmpty()) {
                solrInstance.add(documentsToIndex);

                // Clear the just processed contiguous inner-batch.
                documentsToIndex.clear();
            }

            if (!documentIDsToDelete.isEmpty()) {
                solrInstance.delete(documentIDsToDelete);

                // Clear the just processed contiguous inner-batch.
                documentIDsToDelete.clear();
            }
        } catch (Exception e) {
            this.logger.error("Failed to add/delete entities", e);
        }
    }

    /**
     * @param reference the reference to extract metadata from.
     * @return the {@link SolrInputDocument} containing extracted metadata from the passed reference; {@code null} if
     *         the reference type is not supported.
     * @throws SolrIndexException if problems occur.
     * @throws IllegalArgumentException if there is an incompatibility between a reference and the assigned extractor.
     */
    private LengthSolrInputDocument getSolrDocument(EntityReference reference) throws SolrIndexException,
        IllegalArgumentException
    {
        SolrMetadataExtractor metadataExtractor = getMetadataExtractor(reference.getType());

        // If the entity type is supported, use the extractor to get the SolrInputDocuent.
        if (metadataExtractor != null) {
            return metadataExtractor.getSolrDocument(reference);
        }

        return null;
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
     */
    private void addToQueue(List<EntityReference> references, IndexOperation operation)
    {
        if (!this.disposed) {
            for (EntityReference reference : references) {
                this.resolveQueue.offer(new IndexQueueEntry(reference, operation));
            }
        }
    }
}
