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
package org.xwiki.search.solr.internal.job;

import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrServerException;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.job.DiffDocumentIterator.Action;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

/**
 * Provide progress information and store logging of an advanced indexing.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(IndexerJob.JOBTYPE)
public class IndexerJob extends AbstractJob<IndexerRequest, DefaultJobStatus<IndexerRequest>> implements GroupedJob
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "solr.indexer";

    /**
     * All indexers run in the same thread.
     */
    private static final JobGroupPath GROUP = new JobGroupPath(Arrays.asList("solr", "indexer"));

    /**
     * Used to send documents to index or delete to/from Solr index.
     */
    @Inject
    private transient SolrIndexer indexer;

    @Inject
    @Named("database")
    private transient DocumentIterator<String> databaseIterator;

    @Inject
    @Named("solr")
    private transient DocumentIterator<String> solrIterator;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private SolrInstance solrInstance;

    @Inject
    private SolrReferenceResolver solrReferenceResolver;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        if (getRequest().getRootReference() == null) {
            return GROUP;
        } else {
            return new JobGroupPath(this.entityReferenceSerializer.serialize(getRequest().getRootReference()), GROUP);
        }
    }

    @Override
    protected void runInternal() throws Exception
    {
        if (getRequest().isOverwrite()) {
            EntityReference rootReference = getRequest().getRootReference();
            this.logger.info("Index documents in [{}].", rootReference);
            this.indexer.index(rootReference, true);
        } else {
            updateSolrIndex();
        }
    }

    /**
     * Update the Solr index to match the current state of the database.
     */
    private void updateSolrIndex() throws Exception
    {
        DiffDocumentIterator<String> iterator = new DiffDocumentIterator<>(this.solrIterator, this.databaseIterator);
        iterator.setRootReference(getRequest().getRootReference());

        this.progressManager.pushLevelProgress(2, this);

        try {
            // Calculate index progress size

            this.progressManager.startStep(this);
            int progressSize = (int) iterator.size();
            this.progressManager.endStep(this);

            // Index

            this.progressManager.startStep(this);
            updateSolrIndex(progressSize, iterator);
            this.progressManager.endStep(this);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void updateSolrIndex(int progressSize, DiffDocumentIterator<String> iterator) throws Exception
    {
        this.progressManager.pushLevelProgress(progressSize, this);

        try {
            long[] counter = new long[Action.values().length];

            while (iterator.hasNext()) {
                this.progressManager.startStep(this);

                Pair<DocumentReference, Action> entry = iterator.next();
                if (entry.getValue() == Action.ADD || entry.getValue() == Action.UPDATE) {
                    // The database entry has not been indexed or the indexed version doesn't match the latest
                    // version
                    // from the database.
                    this.indexer.index(entry.getKey(), true);
                    counter[entry.getValue().ordinal()]++;
                } else if (entry.getValue() == Action.DELETE && getRequest().isRemoveMissing()
                    // Double-check if the document really doesn't exist.
                    // Removing an actually existing document is much worse than re-indexing an existing one.
                    // The document might have been created and indexed in Solr between the database and the Solr
                    // query, or the pagination of the database query might have gotten messed up due to the
                    // insertion or deletion of documents.
                    // This check may throw an exception that we just propagate as this would indicate a serious
                    // problem with the database.
                    // It doesn't seem like a good idea to just continue removing documents from the Solr index in that
                    // case.
                    && !this.documentAccessBridge.exists(entry.getKey()))
                {
                    // The index entry doesn't exist anymore in the database.
                    this.indexer.delete(entry.getKey(), true);
                    counter[Action.DELETE.ordinal()]++;
                }

                this.progressManager.endStep(this);
            }

            this.logger.info(
                "{} documents added, {} deleted and {} updated during the synchronization of the Solr index.",
                counter[Action.ADD.ordinal()], counter[Action.DELETE.ordinal()], counter[Action.UPDATE.ordinal()]);

            if (getRequest().isCleanInvalid()) {
                // Wait for the indexing to be fully applied
                this.indexer.waitReady().get();

                // Remove invalid entries
                cleanInvalid();
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void cleanInvalid() throws SolrServerException, IOException, SolrIndexerException
    {
        StringBuilder builder = new StringBuilder();

        builder.append('(');

        // All entries which don't have a docId set
        builder.append(notSet(FieldUtils.DOC_ID));

        builder.append(" OR ");

        // All entries which don't have a fullName set
        builder.append(notSet(FieldUtils.FULLNAME));

        // TODO: Remove from the core all entries for which no corresponding DOCUMENT type entry exist (see
        // https://jira.xwiki.org/browse/XWIKI-22949)

        builder.append(')');

        builder.append(" AND ");

        // Filter documents based on the indicated root reference
        builder.append('(');
        builder.append(this.solrReferenceResolver.getQuery(getRequest().getRootReference()));
        builder.append(')');

        // Execute the delete
        this.solrInstance.deleteByQuery(builder.toString());

        // Commit
        this.solrInstance.commit();
    }

    private String notSet(String field)
    {
        return "-" + field + ":*";
    }
}
