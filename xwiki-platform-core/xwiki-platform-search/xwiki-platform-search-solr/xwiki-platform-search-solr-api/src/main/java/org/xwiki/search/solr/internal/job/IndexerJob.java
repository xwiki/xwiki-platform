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

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
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
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.job.DiffDocumentIterator.Action;

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
    private void updateSolrIndex()
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

    private void updateSolrIndex(int progressSize, DiffDocumentIterator<String> iterator)
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
                } else if (entry.getValue() == Action.DELETE && getRequest().isRemoveMissing()) {
                    // The index entry doesn't exist anymore in the database.
                    this.indexer.delete(entry.getKey(), true);
                }

                counter[entry.getValue().ordinal()]++;

                this.progressManager.endStep(this);
            }

            this.logger.info(
                "{} documents added, {} deleted and {} updated during the synchronization of the Solr index.",
                counter[Action.ADD.ordinal()], counter[Action.DELETE.ordinal()], counter[Action.UPDATE.ordinal()]);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }
}
