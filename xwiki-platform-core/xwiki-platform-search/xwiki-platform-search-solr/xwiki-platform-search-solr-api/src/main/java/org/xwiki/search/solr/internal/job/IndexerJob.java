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

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.Request;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
public class IndexerJob extends AbstractJob<IndexerRequest, DefaultJobStatus<IndexerRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "solr.indexer";

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

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected IndexerRequest castRequest(Request request)
    {
        IndexerRequest indexerRequest;
        if (request instanceof IndexerRequest) {
            indexerRequest = (IndexerRequest) request;
        } else {
            indexerRequest = new IndexerRequest(request);
        }

        return indexerRequest;
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
        DiffDocumentIterator<String> iterator = new DiffDocumentIterator<String>(solrIterator, databaseIterator);
        iterator.setRootReference(getRequest().getRootReference());

        notifyPushLevelProgress((int) iterator.size());

        try {
            // Solr 4.0 doesn't support cursor pagination so it's best if we don't modify the index while iterating it.
            // Let's collect the documents that need to be added to or removed from the index.
            List<DocumentReference> documentsToIndex = new LinkedList<DocumentReference>();
            List<DocumentReference> documentsToDelete = new LinkedList<DocumentReference>();
            while (iterator.hasNext()) {
                Pair<DocumentReference, Action> entry = iterator.next();
                if (entry.getValue() == Action.ADD || entry.getValue() == Action.UPDATE) {
                    // The database entry has not been indexed or the indexed version doesn't match the latest version
                    // from the database.
                    documentsToIndex.add(entry.getKey());
                } else if (entry.getValue() == Action.DELETE && getRequest().isRemoveMissing()) {
                    // The index entry doesn't exist anymore in the database.
                    documentsToDelete.add(entry.getKey());
                }
                notifyStepPropress();
            }
            for (DocumentReference documentReference : documentsToDelete) {
                this.indexer.delete(documentReference, true);
            }
            for (DocumentReference documentReference : documentsToIndex) {
                this.indexer.index(documentReference, true);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }
}
