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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.bridge.internal.DocumentContextExecutor;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.DisposePriority;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.index.IndexException;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.IndexingUserConfig;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.job.IndexerJob;
import org.xwiki.search.solr.internal.job.IndexerRequest;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;
import org.xwiki.search.solr.internal.metadata.XWikiSolrInputDocument;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;
import org.xwiki.store.ReadyIndicator;

import com.google.common.util.concurrent.Uninterruptibles;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.AbstractXWikiRunnable;

/**
 * Default implementation of {@link SolrIndexer}.
 * <p>
 * This implementation does not directly process the given leaf-references, but adds them to a processing queue, in the
 * order they were received. The {@link Runnable} part of this implementation is the one that sequentially reads and
 * processes the queue.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Singleton
// We start the disposal a bit earlier because we want the resolver & indexer threads to finish before the Solr client
// is shutdown. We can't stop the threads immediately because the resolve & index queues may have entries that are being
// processed.
@DisposePriority(500)
public class DefaultSolrIndexer implements SolrIndexer, Initializable, Disposable, Runnable
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
         * The query used to filter entries to delete.
         */
        public String deleteQuery;
        /**
         * The indexing operation to perform.
         */
        public IndexOperation operation;

        /**
         * The ready indicator to indicate that indexing finished until that point.
         */
        private SolrIndexerReadyIndicator readyIndicator;

        /**
         * @param indexReference the reference of the entity to index.
         * @param operation the indexing operation to perform.
         */
        public IndexQueueEntry(EntityReference indexReference, IndexOperation operation)
        {
            this.reference = indexReference;
            this.operation = operation;
        }

        /**
         * @param deleteQuery the query used to filter entries to delete.
         * @param operation the indexing operation to perform.
         */
        public IndexQueueEntry(String deleteQuery, IndexOperation operation)
        {
            this.deleteQuery = deleteQuery;
            this.operation = operation;
        }

        IndexQueueEntry(SolrIndexerReadyIndicator readyIndicator)
        {
            this.readyIndicator = readyIndicator;
            this.operation = IndexOperation.READY_MARKER;
        }

        @Override
        public String toString()
        {
            String str;

            switch (operation) {
                case INDEX:
                    str = "INDEX " + this.reference;
                    break;
                case DELETE:
                    str = "DELETE " + this.deleteQuery;
                    break;
                case STOP:
                    str = "STOP";
                    break;
                case READY_MARKER:
                    str = "READY_MARKER";
                    break;
                default:
                    str = "";
                    break;
            }

            return str;
        }
    }

    /**
     * Resolve queue entry.
     * 
     * @version $Id$
     */
    private static class ResolveQueueEntry
    {
        /**
         * The reference of the entity to index.
         */
        public EntityReference reference;

        /**
         * Also apply operation to reference children.
         */
        public boolean recurse;

        /**
         * The indexing operation to perform.
         */
        public IndexOperation operation;

        /**
         * The ready indicator to indicate that indexing finished until that point.
         */
        private final SolrIndexerReadyIndicator readyIndicator;

        /**
         * @param reference the reference of the entity to index.
         * @param recurse also apply operation to reference children.
         * @param operation the indexing operation to perform.
         */
        public ResolveQueueEntry(EntityReference reference, boolean recurse, IndexOperation operation)
        {
            this.reference = reference;
            this.recurse = recurse;
            this.operation = operation;
            this.readyIndicator = null;
        }

        ResolveQueueEntry(SolrIndexerReadyIndicator readyIndicator)
        {
            this.readyIndicator = readyIndicator;
            this.operation = IndexOperation.READY_MARKER;
        }
    }

    /**
     * Extract children references from passed references and dispatch them to the index queue.
     * 
     * @version $Id$
     */
    private final class Resolver extends AbstractXWikiRunnable
    {
        @Override
        public void runInternal()
        {
            logger.debug("Start SOLR resolver thread");

            while (!Thread.interrupted()) {
                ResolveQueueEntry queueEntry = getQueueEntry();

                if (queueEntry == RESOLVE_QUEUE_ENTRY_STOP) {
                    // Stop the index thread: clear the queue and send the stop signal without blocking.
                    stopIndexerThread();
                    break;
                }

                try {
                    switch (queueEntry.operation) {
                        case READY_MARKER:
                            queueEntry.readyIndicator.switchToIndexQueue();
                            DefaultSolrIndexer.this.indexQueue.put(new IndexQueueEntry(queueEntry.readyIndicator));
                            break;
                        case INDEX:
                            Iterable<EntityReference> references = retrieveReferences(queueEntry);

                            for (EntityReference reference : references) {
                                indexQueue.put(new IndexQueueEntry(reference, queueEntry.operation));
                            }
                            break;
                        default:
                            if (queueEntry.recurse) {
                                indexQueue.put(new IndexQueueEntry(solrRefereceResolver.getQuery(queueEntry.reference),
                                    queueEntry.operation));
                            } else if (queueEntry.reference != null) {
                                indexQueue.put(new IndexQueueEntry(queueEntry.reference, queueEntry.operation));
                            }
                    }
                } catch (Throwable e) {
                    logger.warn("Failed to apply operation [{}] on root reference [{}]", queueEntry.operation,
                        queueEntry.reference, e);
                }
            }

            logger.debug("Stop SOLR resolver thread");
        }

        private Iterable<EntityReference> retrieveReferences(ResolveQueueEntry queueEntry) throws SolrIndexerException
        {
            Iterable<EntityReference> references;
            if (queueEntry.recurse) {
                references = solrRefereceResolver.getReferences(queueEntry.reference);
            } else {
                references = Arrays.asList(queueEntry.reference);
            }
            return references;
        }

        private ResolveQueueEntry getQueueEntry()
        {
            ResolveQueueEntry queueEntry;
            try {
                queueEntry = resolveQueue.take();
                DefaultSolrIndexer.this.resolveQueueRemovalCounter.incrementAndGet();
            } catch (InterruptedException e) {
                logger.warn("The SOLR resolve thread has been interrupted", e);
                queueEntry = RESOLVE_QUEUE_ENTRY_STOP;
            }
            return queueEntry;
        }
    }

    /**
     * Stop resolver thread.
     */
    private static final ResolveQueueEntry RESOLVE_QUEUE_ENTRY_STOP =
        new ResolveQueueEntry(null, false, IndexOperation.STOP);

    /**
     * Stop indexer thread.
     */
    private static final IndexQueueEntry INDEX_QUEUE_ENTRY_STOP =
        new IndexQueueEntry((String) null, IndexOperation.STOP);

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
    private SolrInstance solrInstance;

    /**
     * Extract contained indexable references.
     */
    @Inject
    private SolrReferenceResolver solrRefereceResolver;

    /**
     * Provide a context user for indexing.
     */
    @Inject
    private IndexingUserConfig indexingUserConfig;

    @Inject
    private Execution execution;

    @Inject
    private ExecutionContextManager ecim;

    @Inject
    private JobExecutor jobs;

    @Inject
    private DocumentContextExecutor documentContextExecutor;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    /**
     * The queue of index operation to perform.
     */
    private BlockingQueue<IndexQueueEntry> indexQueue;

    /**
     * The queue of resolve references and add them to the index queue.
     */
    private BlockingQueue<ResolveQueueEntry> resolveQueue;

    /**
     * The executor for Solr client operations.
     */
    private ExecutorService solrClientExecutor;

    /**
     * Future to wait on the last commit operation.
     */
    private Future<Void> commitFuture;

    /**
     * Thread in which the indexUpdater will be executed.
     */
    private Thread indexThread;

    /**
     * Thread in which the provided references children will be resolved.
     */
    private Thread resolveThread;

    /**
     * A counter that tracks how many items have been removed from the index queue since the start.
     * Used to track progress in the index queue.
     */
    private final AtomicLong indexQueueRemovalCounter = new AtomicLong();

    /**
     * A counter that tracks how many items have been removed from the resolve queue since the start.
     * Used to track progress in the resolve queue.
     */
    private final AtomicLong resolveQueueRemovalCounter = new AtomicLong();

    /**
     * Indicate of the component has been disposed.
     */
    private boolean disposed;

    /**
     * The size of the not yet sent batch.
     */
    private volatile int batchSize;

    /**
     * The length of the not yet sent batch in characters.
     */
    private int batchLength;

    /**
     * The size of the batch that is currently being sent and committed.
     */
    private volatile int committingBatchSize;

    @Override
    public void initialize() throws InitializationException
    {
        // Initialize the queues before starting the threads.
        this.resolveQueue = new LinkedBlockingQueue<>();
        this.indexQueue = new LinkedBlockingQueue<>(this.configuration.getIndexerQueueCapacity());

        // Launch the Solr client executor.
        this.solrClientExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("XWiki Solr client thread");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });

        // Launch the resolve thread
        this.resolveThread = new Thread(new Resolver());
        this.resolveThread.setName("XWiki Solr resolve thread");
        this.resolveThread.setDaemon(true);
        this.resolveThread.start();
        this.resolveThread.setPriority(Thread.NORM_PRIORITY - 1);

        // Launch the index thread
        this.indexThread = new Thread(this);
        this.indexThread.setName("XWiki Solr index thread");
        this.indexThread.setDaemon(true);
        this.indexThread.start();
        this.indexThread.setPriority(Thread.NORM_PRIORITY - 1);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // Mark the component as disposed
        this.disposed = true;

        // Stop the resolve thread. Clear the queue and send the stop signal without blocking. We know that the resolve
        // queue will remain empty after the clear call because we set the disposed flag above.
        for (ResolveQueueEntry entry = this.resolveQueue.poll(); entry != null; entry = this.resolveQueue.poll()) {
            if (entry.operation == IndexOperation.READY_MARKER && entry.readyIndicator != null) {
                entry.readyIndicator.completeExceptionally(new IndexException("Indexing stopped."));
            }
        }
        this.resolveQueue.offer(RESOLVE_QUEUE_ENTRY_STOP);

        // Stop the index thread. Clear the queue and send the stop signal without blocking. There should be enough
        // space in the index queue before the special stop entry is added as long the the index queue capacity is
        // greater than 1. In the worse case, the clear call will unblock the resolve thread (which was waiting because
        // the index queue was full) and just one entry will be added to the queue before the special stop entry.
        stopIndexerThread();
    }

    private void stopIndexerThread()
    {
        for (IndexQueueEntry entry = this.indexQueue.poll(); entry != null; entry = this.indexQueue.poll()) {
            if (entry.operation == IndexOperation.READY_MARKER && entry.readyIndicator != null) {
                entry.readyIndicator.completeExceptionally(new IndexException("Indexing stopped."));
            }
        }
        this.indexQueue.offer(INDEX_QUEUE_ENTRY_STOP);
    }

    @Override
    public void run()
    {
        this.logger.debug("Start SOLR indexer thread");

        while (!Thread.interrupted()) {
            // Block until there is at least one entry in the queue
            IndexQueueEntry queueEntry = null;
            try {
                queueEntry = this.indexQueue.take();
            } catch (InterruptedException e) {
                this.logger.warn("The SOLR index thread has been interrupted", e);

                queueEntry = INDEX_QUEUE_ENTRY_STOP;
            }

            // Add to the batch until either the batch size is achieved, the queue gets emptied or the
            // INDEX_QUEUE_ENTRY_STOP is retrieved from the queue.
            if (!processBatch(queueEntry)) {
                break;
            }
        }

        this.logger.debug("Stop SOLR indexer thread");
    }

    /**
     * Process a batch of operations that were just read from the index operations queue. This method also commits the
     * batch when it finishes to process it.
     * 
     * @param queueEntry the batch to process
     * @return {@code true} to wait for another batch, {@code false} to stop the indexing thread
     */
    private boolean processBatch(IndexQueueEntry queueEntry)
    {
        List<SolrInputDocument> solrDocuments = new ArrayList<>();

        for (IndexQueueEntry batchEntry = queueEntry; batchEntry != null; batchEntry = this.indexQueue.poll()) {
            this.indexQueueRemovalCounter.incrementAndGet();

            if (batchEntry == INDEX_QUEUE_ENTRY_STOP) {
                // Handle the shutdown of the executor here to avoid that the executor is shut down before the batch
                // processing finished.
                this.solrClientExecutor.shutdown();
                // Discard the current batch and stop the indexing thread.
                return false;
            }

            // For the current contiguous operations queue, group the changes
            try {
                ExecutionContext executionContext = new ExecutionContext();
                this.ecim.initialize(executionContext);
                XWikiContext xcontext = (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
                xcontext.setUserReference(indexingUserConfig.getIndexingUserReference());

                switch (batchEntry.operation) {
                    case INDEX:
                        XWikiSolrInputDocument solrDocument = getSolrDocument(batchEntry.reference);
                        if (solrDocument != null) {
                            solrDocuments.add(solrDocument);
                            this.batchLength += solrDocument.getLength();
                            ++this.batchSize;
                        }
                        break;
                    case DELETE:
                        // Ensure that all queued additions are processed before any deletions are processed.
                        flushDocuments(solrDocuments);
                        applyDeletion(batchEntry);

                        ++this.batchSize;
                        break;
                    case READY_MARKER:
                        commit(solrDocuments);
                        // Add the completion of the ready indicator to the same queue to ensure that it is processed
                        // after the commit.
                        IndexQueueEntry finalBatchEntry = batchEntry;
                        this.solrClientExecutor.execute(() -> finalBatchEntry.readyIndicator.complete(null));
                        break;
                    default:
                        // Do nothing.
                }
            } catch (Throwable e) {
                this.logger.error("Failed to process entry [{}]", batchEntry, e);
            } finally {
                this.execution.removeContext();
            }

            // Commit the index changes so that they become available to queries. This is a costly operation and that is
            // the reason why we perform it at the end of the batch.
            if (shouldCommit(this.batchLength, this.batchSize)) {
                commit(solrDocuments);
            }
        }

        // Commit what's left
        if (this.batchSize > 0) {
            commit(solrDocuments);
        }

        return true;
    }

    private void applyDeletion(IndexQueueEntry queueEntry) throws SolrIndexerException
    {
        String id = queueEntry.reference == null ? null : this.solrRefereceResolver.getId(queueEntry.reference);
        this.solrClientExecutor.execute(() -> {
            try {
                if (id == null) {
                    this.solrInstance.deleteByQuery(queueEntry.deleteQuery);
                } else {
                    this.solrInstance.delete(id);
                }
            } catch (Exception e) {
                this.logger.error("Failed to delete document [{}] from the Solr server", queueEntry, e);
            }
        });
    }

    private void flushDocuments(List<SolrInputDocument> documents)
    {
        try {
            // Copy the documents to flush to a new list so that we can clear the original list without affecting the
            // documents that are being added to the Solr server.
            List<SolrInputDocument> documentsToFlush = new ArrayList<>(documents);
            this.solrClientExecutor.execute(() -> {
                try {
                    this.solrInstance.add(documentsToFlush);
                } catch (Exception e) {
                    this.logger.error("Failed to add documents to the Solr server", e);
                }
            });
        } finally {
            // Clear the list of documents even when adding them failed to avoid leaking memory and to avoid
            // re-submitting the same documents to the Solr server multiple times.
            documents.clear();
        }
    }

    /**
     * Commit.
     */
    private void commit(List<SolrInputDocument> documents)
    {
        flushDocuments(documents);
        if (this.commitFuture != null) {
            try {
                Uninterruptibles.getUninterruptibly(this.commitFuture);
            } catch (ExecutionException e) {
                this.logger.error("Failed to commit index changes to the Solr server", e);
            }
        }

        // At this point, we can be sure that there is no more commit in the queue - so we can safely store the size
        // of the next commit.
        this.committingBatchSize = this.batchSize;
        this.commitFuture = this.solrClientExecutor.submit(() -> {
            try {
                this.solrInstance.commit();
            } catch (Exception e) {
                this.logger.error("Failed to commit index changes to the Solr server. Rolling back.", e);

                try {
                    this.solrInstance.rollback();
                } catch (Exception ex) {
                    // Just log the failure.
                    this.logger.error("Failed to rollback index changes.", ex);
                }
            }

            this.committingBatchSize = 0;

            return null;
        });
        this.batchSize = 0;
        this.batchLength = 0;
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
     * @param reference the reference to extract metadata from.
     * @return the {@link SolrInputDocument} containing extracted metadata from the passed reference; {@code null} if
     *         the reference type is not supported.
     * @throws SolrIndexerException if problems occur.
     * @throws IllegalArgumentException if there is an incompatibility between a reference and the assigned extractor.
     * @throws ExecutionContextException
     */
    private XWikiSolrInputDocument getSolrDocument(EntityReference reference)
        throws SolrIndexerException, IllegalArgumentException, ExecutionContextException
    {
        SolrMetadataExtractor metadataExtractor = getMetadataExtractor(reference.getType());

        // If the entity type is supported, use the extractor to get the SolrInputDocuent.
        if (metadataExtractor != null) {
            // Set the document that belongs to the entity reference as context document to ensure that the correct
            // settings are loaded for the current document/wiki.
            XWikiContext context = this.xWikiContextProvider.get();
            try {
                XWikiDocument document = context.getWiki().getDocument(reference, context);

                return this.documentContextExecutor.call(() -> metadataExtractor.getSolrDocument(reference), document);
            } catch (SolrIndexerException | IllegalArgumentException e) {
                // Re-throw to avoid wrapping exceptions that are declared in the method signature.
                throw e;
            } catch (Exception e) {
                throw new SolrIndexerException("Error executing the indexer in the context of the document to index",
                    e);
            }
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

    @Override
    public void index(EntityReference reference, boolean recurse)
    {
        addToQueue(reference, recurse, IndexOperation.INDEX);
    }

    @Override
    public void delete(EntityReference reference, boolean recurse)
    {
        addToQueue(reference, recurse, IndexOperation.DELETE);
    }

    /**
     * Add a list of references to the index queue, all having the same operation.
     * 
     * @param reference the references to add
     * @param recurse also apply operation to children
     * @param operation the operation to assign to the given references
     */
    private void addToQueue(EntityReference reference, boolean recurse, IndexOperation operation)
    {
        if (!this.disposed) {
            // Don't block because the capacity of the resolver queue is not limited.
            try {
                this.resolveQueue.put(new ResolveQueueEntry(reference, recurse, operation));
            } catch (InterruptedException e) {
                this.logger.error("Failed to add reference [{}] to Solr indexing queue", reference, e);
            }
        }
    }

    @Override
    public int getQueueSize()
    {
        return this.indexQueue.size() + this.resolveQueue.size() + this.batchSize + this.committingBatchSize;
    }

    @Override
    public IndexerJob startIndex(IndexerRequest request) throws SolrIndexerException
    {
        try {
            return (IndexerJob) this.jobs.execute(IndexerJob.JOBTYPE, request);
        } catch (JobException e) {
            throw new SolrIndexerException("Failed to start index job", e);
        }
    }

    @Override
    public ReadyIndicator waitReady()
    {
        SolrIndexerReadyIndicator readyIndicator = new SolrIndexerReadyIndicator(
            this.resolveQueueRemovalCounter::getAcquire, this.resolveQueue::size,
            this.indexQueueRemovalCounter::getAcquire, this.indexQueue::size,
            this.configuration::getIndexerQueueCapacity
        );

        if (!this.disposed) {
            try {
                this.resolveQueue.put(new ResolveQueueEntry(readyIndicator));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                readyIndicator.completeExceptionally(e);
            }
        } else {
            // The indexer has been stopped and won't become ready again.
            readyIndicator.completeExceptionally(new SolrIndexerException("The indexer has been disposed"));
        }

        return readyIndicator;
    }
}
