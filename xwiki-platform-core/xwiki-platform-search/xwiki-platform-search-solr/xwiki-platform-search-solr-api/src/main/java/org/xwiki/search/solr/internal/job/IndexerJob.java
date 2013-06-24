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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
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
import org.xwiki.query.QueryManager;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrIndexerException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.search.solr.internal.reference.SolrReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

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

                notifyStepPropress();
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
     * @throws SolrIndexerException when failing to clean the Solr index
     * @throws SolrServerException when failing to clean the Solr index
     * @throws IllegalArgumentException when failing to clean the Solr index
     */
    private void removeMissing() throws SolrIndexerException, SolrServerException, IllegalArgumentException
    {
        this.logger.info("Remove Solr documents not in the database anymore");

        SolrInstance solrInstance = this.solrInstanceProvider.get();

        // Clean existing index
        SolrQuery solrQuery = new SolrQuery(this.solrResolver.getQuery(getRequest().getRootReference()));
        solrQuery.setRows(100);
        solrQuery.setFields(FieldUtils.NAME, FieldUtils.SPACE, FieldUtils.WIKI, FieldUtils.LOCALE);
        solrQuery.set(FieldUtils.TYPE, EntityType.DOCUMENT.toString().toLowerCase());

        // TODO: be nicer with the memory when there is a lot of indexed documents and do smaller batches or stream the
        // results
        QueryResponse response = solrInstance.query(solrQuery);

        SolrDocumentList results = response.getResults();

        notifyPushLevelProgress((int) results.getNumFound());

        try {
            for (SolrDocument solrDocument : results) {
                DocumentReference reference =
                    createDocumentReference((String) solrDocument.get(FieldUtils.WIKI),
                        (String) solrDocument.get(FieldUtils.SPACE), (String) solrDocument.get(FieldUtils.NAME),
                        (String) solrDocument.get(FieldUtils.LOCALE));
                try {
                    if (!exists(reference)) {
                        this.indexer.delete(reference, true);
                    }
                } catch (QueryException e) {
                    this.logger.error("Failed to check if document [{}] exists", e);
                }

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * Check if provided document exists (including Locale).
     * 
     * @param documentReference the document reference
     * @return true of the document exists
     * @throws QueryException when failing to execute exists request
     */
    // TODO: replace with XWiki#exists when locale support is added
    private boolean exists(DocumentReference documentReference) throws QueryException
    {
        XWikiDocument document = new XWikiDocument(documentReference);

        Query query = this.queryManager.createQuery("select doc.id from Document doc where doc.id=:id", Query.XWQL);
        query.setWiki(documentReference.getWikiReference().getName());
        query.bindValue("id", document.getId());

        return !query.execute().isEmpty();
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
     * @throws QueryException when failing to index new documents
     * @throws XWikiException when failing to index new documents
     * @throws SolrIndexerException when failing to index new documents
     * @throws IllegalArgumentException when failing to index new documents
     * @throws SolrServerException when failing to index new documents
     */
    private void addMissing() throws QueryException, XWikiException, SolrIndexerException, IllegalArgumentException,
        SolrServerException
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
                for (String wiki : wikis) {
                    addMissing(wiki);
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
     * @throws QueryException when failing to index new documents
     * @throws SolrIndexerException when failing to index new documents
     * @throws IllegalArgumentException when failing to index new documents
     * @throws SolrServerException when failing to index new documents
     */
    private void addMissing(String wiki) throws QueryException, SolrIndexerException, IllegalArgumentException,
        SolrServerException
    {
        this.logger.info("Index documents not yet indexed in wiki [{}]", wiki);

        SolrInstance solrInstance = this.solrInstanceProvider.get();

        EntityReference spaceReference;
        EntityReference documentReference;

        EntityReference rootReference = getRequest().getRootReference();
        if (rootReference != null) {
            spaceReference = rootReference.extractReference(EntityType.SPACE);
            documentReference = rootReference.extractReference(EntityType.DOCUMENT);
        } else {
            spaceReference = null;
            documentReference = null;
        }

        String q = "select doc.name, doc.space, doc.language, doc.version from Document doc";
        if (spaceReference != null) {
            q += " where doc.space=:space";
        }
        if (documentReference != null) {
            q += ", doc.name=:name";
        }

        // TODO: be nicer with the memory when there is a lot of documents and do smaller batches
        Query query = this.queryManager.createQuery(q, Query.XWQL);
        query.setWiki(wiki);
        if (spaceReference != null) {
            query.bindValue("space", spaceReference.getName());
        }
        if (documentReference != null) {
            query.bindValue("name", documentReference.getName());
        }

        List<Object[]> documents = query.<Object[]> execute();

        notifyPushLevelProgress(documents.size());

        try {
            for (Object[] document : documents) {
                addMissing(wiki, document, solrInstance);

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * Index document (versions) not yet indexed in the passed wiki.
     * 
     * @param wiki the wiki where to search for documents to index
     * @param document the document found
     * @param solrInstance used to start indexing of a document
     * @throws SolrIndexerException when failing to index new documents
     * @throws IllegalArgumentException when failing to index new documents
     * @throws SolrServerException when failing to index new documents
     */
    private void addMissing(String wiki, Object[] document, SolrInstance solrInstance) throws SolrIndexerException,
        IllegalArgumentException, SolrServerException
    {
        String name = (String) document[0];
        String space = (String) document[1];
        String localeString = (String) document[2];
        String version = (String) document[3];

        DocumentReference reference = createDocumentReference(wiki, space, name, localeString);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setFields(FieldUtils.ID);
        solrQuery.set(FieldUtils.ID, this.solrResolver.getId(reference));
        solrQuery.set(FieldUtils.VERSION, version);
        QueryResponse response = solrInstance.query(solrQuery);
        if (response.getResults().getNumFound() == 0) {
            this.indexer.index(reference, true);
        }
    }
}
