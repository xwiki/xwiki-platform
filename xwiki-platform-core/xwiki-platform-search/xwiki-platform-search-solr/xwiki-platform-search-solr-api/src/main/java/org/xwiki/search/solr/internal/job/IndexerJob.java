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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.Request;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.job.internal.DefaultJobStatus;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

import com.xpn.xwiki.XWikiContext;

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
     * The maximum number of documents to query at once.
     */
    private static final int LIMIT = 100;

    /**
     * {@link StreamingResponseCallback} used to remove from the Solr index the XWiki documents that have been deleted
     * from the data base.
     */
    private class RemoveMissingCallback extends StreamingResponseCallback
    {
        /**
         * XWiki context, used to determine if an XWiki document exists or not.
         */
        private XWikiContext xcontext = IndexerJob.this.xcontextProvider.get();

        /**
         * Flag used to know if we pushed the level progress. We need to know this to avoid calling pop if there's no
         * corresponding push.
         */
        private boolean levelProgressPused;

        /**
         * The total number of results found.
         */
        private long numFound;

        /**
         * The offset from where the streaming starts.
         */
        private long start;

        /**
         * The number of results that have been streamed.
         */
        private long count;

        @Override
        public void streamSolrDocument(SolrDocument solrDocument)
        {
            String wiki = (String) solrDocument.get(FieldUtils.WIKI);
            String space = (String) solrDocument.get(FieldUtils.SPACE);
            String name = (String) solrDocument.get(FieldUtils.NAME);
            String locale = (String) solrDocument.get(FieldUtils.DOCUMENT_LOCALE);
            DocumentReference reference = createDocumentReference(wiki, space, name, locale);
            if (!this.xcontext.getWiki().exists(reference, this.xcontext)) {
                IndexerJob.this.indexer.delete(reference, true);
            }
            IndexerJob.this.notifyStepPropress();
            count++;
        }

        @Override
        public void streamDocListInfo(long numFound, long start, Float maxScore)
        {
            this.numFound = numFound;
            this.start = start;
            this.count = 0;

            if (start == 0) {
                IndexerJob.this.notifyPushLevelProgress((int) numFound);
                this.levelProgressPused = true;
            }
        }

        /**
         * @return {@code true} if a new job level progress was pushed
         */
        public boolean isLevelProgressPushed()
        {
            return this.levelProgressPused;
        }

        /**
         * @return {@code true} if there are remaining documents to stream
         */
        public boolean hasNext()
        {
            return start + count < numFound;
        }
    }

    /**
     * Used to resolve Solr document id from reference.
     */
    @Inject
    private SolrReferenceResolver solrResolver;

    /**
     * Used to query the database.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * Used to count the number of XWiki documents in the database to be able to display the job progress.
     */
    @Inject
    @Named("count")
    private QueryFilter countFilter;

    /**
     * Provider for the {@link SolrInstance} that allows communication with the Solr server.
     */
    @Inject
    private Provider<SolrInstance> solrInstanceProvider;

    /**
     * Used to access the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to send documents to index or delete to/from Solr index.
     */
    @Inject
    private SolrIndexer indexer;

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
        if (getRequest().isRemoveMissing()) {
            notifyPushLevelProgress(2);
        }

        try {
            if (getRequest().isRemoveMissing()) {
                // Remove from Solr entities not in DB anymore
                removeMissing();
            }

            // Add in Solr entities in DB and not in Solr
            addMissing();
        } finally {
            if (getRequest().isRemoveMissing()) {
                notifyPopLevelProgress();
            }
        }
    }

    /**
     * Remove Solr documents not in the database anymore.
     * 
     * @throws Exception when failing to clean the Solr index
     */
    private void removeMissing() throws Exception
    {
        this.logger.info("Remove Solr documents not in the database anymore");

        SolrQuery solrQuery = new SolrQuery(this.solrResolver.getQuery(getRequest().getRootReference()));
        // We need this fields in order to create the document reference.
        solrQuery.setFields(FieldUtils.NAME, FieldUtils.SPACE, FieldUtils.WIKI, FieldUtils.DOCUMENT_LOCALE);
        solrQuery.addFilterQuery(FieldUtils.TYPE + ':' + EntityType.DOCUMENT.name());
        // Speed up the query by disabling the faceting and highlighting.
        solrQuery.set(FacetParams.FACET, false);
        solrQuery.set(HighlightParams.HIGHLIGHT, false);
        // Don't fetch all the indexed documents in one request. The index can be pretty big.
        solrQuery.setRows(LIMIT);
        solrQuery.setStart(0);

        // Iterate over the indexed documents and remove those for which the corresponding XWiki document doesn't exist.
        SolrInstance solrInstance = this.solrInstanceProvider.get();
        RemoveMissingCallback callback = new RemoveMissingCallback();
        try {
            do {
                solrInstance.queryAndStreamResponse(solrQuery, callback);
                solrQuery.setStart(solrQuery.getStart() + LIMIT);
                // This loop can take a while if there are many indexed documents so be nice with the CPU.
                Thread.yield();
            } while (callback.hasNext());
        } finally {
            if (callback.isLevelProgressPushed()) {
                notifyPopLevelProgress();
            }
        }
    }

    /**
     * @param wiki the wiki part of the reference
     * @param space the space part of the reference
     * @param name the name part of the reference
     * @param localeString the locale part of the reference as String
     * @return the complete document reference
     */
    private DocumentReference createDocumentReference(String wiki, String space, String name, String localeString)
    {
        if (localeString == null || localeString.isEmpty()) {
            return new DocumentReference(wiki, space, name);
        } else {
            return new DocumentReference(wiki, space, name, LocaleUtils.toLocale(localeString));
        }
    }

    /**
     * Index documents not yet indexed in the whole farm.
     * 
     * @throws Exception when failing to index new documents
     */
    private void addMissing() throws Exception
    {
        if (getRequest().isOverwrite()) {
            this.logger.info("Index documents in [{}]", getRequest().getRootReference());

            this.indexer.index(getRequest().getRootReference(), true);
        } else {
            this.logger.info("Index documents in [{}] not yet indexed", getRequest().getRootReference());

            EntityReference rootReference = getRequest().getRootReference();
            if (rootReference == null) {
                XWikiContext xcontext = this.xcontextProvider.get();

                List<String> wikis = xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext);

                notifyPushLevelProgress(wikis.size());

                try {
                    for (String wiki : wikis) {
                        addMissing(wiki);

                        notifyStepPropress();
                    }
                } finally {
                    notifyPopLevelProgress();
                }
            } else {
                EntityReference wikiReference = rootReference.extractReference(EntityType.WIKI);
                addMissing(wikiReference.getName());
            }
        }
    }

    /**
     * Index document (versions) not yet indexed in the passed wiki.
     * 
     * @param wiki the wiki where to search for documents to index
     * @throws Exception when failing to index new documents
     */
    private void addMissing(String wiki) throws Exception
    {
        this.logger.info("Index documents not yet indexed in wiki [{}]", wiki);

        Query[] queries = buildAddMissingQueries(getRequest().getRootReference());
        Query countQuery = queries[0];
        Query selectQuery = queries[1];

        // We need the count query only to be able to display the job progress.
        Long documentCount = (Long) countQuery.setWiki(wiki).execute().get(0);
        notifyPushLevelProgress(documentCount.intValue());

        SolrInstance solrInstance = this.solrInstanceProvider.get();
        selectQuery.setWiki(wiki).setLimit(LIMIT).setOffset(0);
        List<Object[]> documents;
        try {
            do {
                documents = selectQuery.<Object[]> execute();
                for (Object[] document : documents) {
                    addMissing(wiki, document, solrInstance);
                    notifyStepPropress();
                }
                selectQuery.setOffset(selectQuery.getOffset() + LIMIT);
                // This loop can take a while if there are many documents in the database so be nice with the CPU.
                Thread.yield();
            } while (documents.size() == LIMIT);
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * Builds the count and select queries that will be used to index the XWiki documents contained by the entity with
     * the given reference.
     * 
     * @param rootReference specifies the entity whose documents should be indexed if they are not already indexed
     * @return the count and select queries
     * @throws QueryException if building the queries fails
     */
    private Query[] buildAddMissingQueries(EntityReference rootReference) throws QueryException
    {
        EntityReference spaceReference = null;
        EntityReference documentReference = null;
        if (rootReference != null) {
            spaceReference = rootReference.extractReference(EntityType.SPACE);
            documentReference = rootReference.extractReference(EntityType.DOCUMENT);
        }

        String selectFrom = "select doc.name, doc.space, doc.language, doc.version from Document doc";
        String whereClause = "";
        List<Object> parameters = new ArrayList<Object>();
        if (spaceReference != null) {
            whereClause += " where doc.space = ?";
            parameters.add(spaceReference.getName());
            if (documentReference != null) {
                whereClause += " and doc.name = ?";
                parameters.add(documentReference.getName());
            }
        }

        Query countQuery = this.queryManager.createQuery(whereClause, Query.XWQL);
        countQuery.bindValues(parameters).addFilter(countFilter);

        Query selectQuery = this.queryManager.createQuery(selectFrom + whereClause, Query.XWQL);
        selectQuery.bindValues(parameters);

        return new Query[] {countQuery, selectQuery};
    }

    /**
     * Index document (versions) not yet indexed in the passed wiki.
     * 
     * @param wiki the wiki where to search for documents to index
     * @param document the document found
     * @param solrInstance used to start indexing of a document
     * @throws Exception when failing to index new documents
     */
    private void addMissing(String wiki, Object[] document, SolrInstance solrInstance) throws Exception
    {
        String name = (String) document[0];
        String space = (String) document[1];
        String localeString = (String) document[2];
        String version = (String) document[3];

        DocumentReference reference = createDocumentReference(wiki, space, name, localeString);

        String id = ClientUtils.escapeQueryChars(this.solrResolver.getId(reference));
        SolrQuery solrQuery = new SolrQuery(FieldUtils.ID + ':' + id);
        solrQuery.addFilterQuery(FieldUtils.VERSION + ':' + ClientUtils.escapeQueryChars(version));
        solrQuery.setFields(FieldUtils.ID);
        // Speed up the query by disabling the faceting and highlighting.
        solrQuery.set(FacetParams.FACET, false);
        solrQuery.set(HighlightParams.HIGHLIGHT, false);
        // We need at most one result (i.e. stop after the first matching document is found).
        solrQuery.setRows(1);
        solrQuery.setStart(0);

        QueryResponse response = solrInstance.query(solrQuery);
        if (response.getResults().getNumFound() == 0) {
            this.indexer.index(reference, true);
        }
    }
}
