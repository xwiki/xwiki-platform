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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
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
import org.xwiki.job.Job;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.job.IndexerJob;
import org.xwiki.search.solr.internal.job.IndexerRequest;
import org.xwiki.search.solr.internal.metadata.LengthSolrInputDocument;
import org.xwiki.search.solr.internal.metadata.SolrMetadataExtractor;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

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
         * @param reference the reference of the entity to index.
         * @param recurse also apply operation to reference children.
         * @param operation the indexing operation to perform.
         */
        public ResolveQueueEntry(EntityReference reference, boolean recurse, IndexOperation operation)
        {
            this.reference = reference;
            this.recurse = recurse;
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
                ResolveQueueEntry queueEntry;
                try {
                    queueEntry = resolveQueue.take();
                } catch (InterruptedException e) {
                    logger.warn("The SOLR resolve thread has been interrupted", e);
                    queueEntry = RESOLVE_QUEUE_ENTRY_STOP;
                }

                if (queueEntry == RESOLVE_QUEUE_ENTRY_STOP) {
                    // Stop the index thread: clear the queue and send the stop signal without blocking.
                    indexQueue.clear();
                    indexQueue.offer(INDEX_QUEUE_ENTRY_STOP);
                    break;
                }

                try {
                    if (queueEntry.operation == IndexOperation.INDEX) {
                        Iterable<EntityReference> references;
                        if (queueEntry.recurse) {
                            references = solrRefereceResolver.getReferences(queueEntry.reference);
                        } else {
                            references = Arrays.asList(queueEntry.reference);
                        }

                        for (EntityReference reference : references) {
                            indexQueue.put(new IndexQueueEntry(reference, queueEntry.operation));
                        }
                    } else {
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
    }

    /**
     * Stop resolver thread.
     */
    private static final ResolveQueueEntry RESOLVE_QUEUE_ENTRY_STOP = new ResolveQueueEntry(null, false,
        IndexOperation.STOP);

    /**
     * Stop indexer thread.
     */
    private static final IndexQueueEntry INDEX_QUEUE_ENTRY_STOP = new IndexQueueEntry((String) null,
        IndexOperation.STOP);

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
    private SolrReferenceResolver solrRefereceResolver;

    @Inject
    private Execution execution;

    @Inject
    private ExecutionContextManager ecim;

    /**
     * The queue of index operation to perform.
     */
    private BlockingQueue<IndexQueueEntry> indexQueue;

    /**
     * The queue of resolve references and add them to the index queue.
     */
    private BlockingQueue<ResolveQueueEntry> resolveQueue;

    /**
     * Indexer jobs.
     */
    // TODO: use JobManager instead when it support several threads
    private ExecutorService indexerJobs;

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
        // Initialize the queues before starting the threads.
        this.resolveQueue = new LinkedBlockingQueue<ResolveQueueEntry>();
        this.indexQueue = new LinkedBlockingQueue<IndexQueueEntry>(this.configuration.getIndexerQueueCapacity());

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

        // Setup indexer job thread
        BasicThreadFactory factory =
            new BasicThreadFactory.Builder().namingPattern("XWiki Solr index job thread").daemon(true)
                .priority(Thread.MIN_PRIORITY).build();
        this.indexerJobs = Executors.newSingleThreadExecutor(factory);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // Mark the component as disposed
        this.disposed = true;

        // Shutdown indexer jobs queue
        this.indexerJobs.shutdownNow();

        // Stop the resolve thread. Clear the queue and send the stop signal without blocking. We know that the resolve
        // queue will remain empty after the clear call because we set the disposed flag above.
        this.resolveQueue.clear();
        this.resolveQueue.offer(RESOLVE_QUEUE_ENTRY_STOP);

        // Stop the index thread. Clear the queue and send the stop signal without blocking. There should be enough
        // space in the index queue before the special stop entry is added as long the the index queue capacity is
        // greater than 1. In the worse case, the clear call will unblock the resolve thread (which was waiting because
        // the index queue was full) and just one entry will be added to the queue before the special stop entry.
        this.indexQueue.clear();
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

            if (queueEntry == INDEX_QUEUE_ENTRY_STOP) {
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
        SolrInstance solrInstance = this.solrInstanceProvider.get();

        int length = 0;
        int size = 0;

        for (IndexQueueEntry batchEntry = queueEntry; batchEntry != null; batchEntry = this.indexQueue.poll()) {
            IndexOperation operation = batchEntry.operation;

            // For the current contiguous operations queue, group the changes
            try {
                this.ecim.initialize(new ExecutionContext());

                if (IndexOperation.INDEX.equals(operation)) {
                    LengthSolrInputDocument solrDocument = getSolrDocument(batchEntry.reference);
                    if (solrDocument != null) {
                        solrInstance.add(solrDocument);
                        length += solrDocument.getLength();
                        ++size;
                    }
                } else if (IndexOperation.DELETE.equals(operation)) {
                    if (batchEntry.reference == null) {
                        solrInstance.deleteByQuery(batchEntry.deleteQuery);
                    } else {
                        solrInstance.delete(this.solrRefereceResolver.getId(batchEntry.reference));
                    }

                    ++size;
                }
            } catch (Throwable e) {
                this.logger.error("Failed to process entry [{}]", batchEntry, e);
            } finally {
                this.execution.removeContext();
            }

            // Commit the index changes so that they become available to queries. This is a costly operation and that is
            // the reason why we perform it at the end of the batch.
            if (shouldCommit(length, size)) {
                commit();
                length = 0;
                size = 0;
            }
        }

        // Commit what's left
        if (size > 0) {
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
     * @param reference the reference to extract metadata from.
     * @return the {@link SolrInputDocument} containing extracted metadata from the passed reference; {@code null} if
     *         the reference type is not supported.
     * @throws SolrIndexerException if problems occur.
     * @throws IllegalArgumentException if there is an incompatibility between a reference and the assigned extractor.
     * @throws ExecutionContextException
     */
    private LengthSolrInputDocument getSolrDocument(EntityReference reference) throws SolrIndexerException,
        IllegalArgumentException, ExecutionContextException
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
            this.resolveQueue.offer(new ResolveQueueEntry(reference, recurse, operation));
        }
    }

    @Override
    public int getQueueSize()
    {
        return this.indexQueue.size() + this.resolveQueue.size();
    }

    @Override
    public IndexerJob startIndex(IndexerRequest request) throws SolrIndexerException
    {
        IndexerJob job;
        try {
            job = this.componentManager.getInstance(Job.class, IndexerJob.JOBTYPE);
        } catch (ComponentLookupException e) {
            throw new SolrIndexerException("Failed to lookup indexer job component", e);
        }

        job.initialize(request);

        this.indexerJobs.execute(job);

        return job;
    }
}
