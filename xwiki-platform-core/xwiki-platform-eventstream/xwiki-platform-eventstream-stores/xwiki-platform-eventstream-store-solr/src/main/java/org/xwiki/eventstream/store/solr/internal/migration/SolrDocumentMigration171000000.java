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
package org.xwiki.eventstream.store.solr.internal.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.store.solr.internal.EventsSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.XWikiSolrCore;

import jakarta.inject.Singleton;

/**
 * Migration of documents after introducing {@link Event#FIELD_PREFILTERING_DATE}.
 *
 * @version $Id$
 * @since 17.10.0
 */
@Component(roles = SolrDocumentMigration171000000.class)
@Singleton
public class SolrDocumentMigration171000000
{
    /**
     * Number of documents to consider at once for migration.
     */
    private static final int BATCH_MIGRATION_SIZE = 1000;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Logger logger;

    /**
     * Perform migration of documents to fill the field {@link Event#FIELD_PREFILTERING_DATE}.
     *
     * @param core the core for which to perform the migration.
     * @throws SolrException in case of problem when performing the migration.
     */
    public void migrateAllDocuments(XWikiSolrCore core) throws SolrException
    {
        SolrDocumentList documentList;
        int startIndex = 0;
        int totalMigrated = 0;
        long totalNumber = 0;

        SolrQuery solrQuery = new SolrQuery()
            .setFilterQueries(
                String.format("%s:%s",
                    Event.FIELD_PREFILTERED,
                    this.solrUtils.toFilterQueryString(true))
            )
            .setFields(Event.FIELD_ID, Event.FIELD_DATE, Event.FIELD_PREFILTERING_DATE)
            .setStart(startIndex)
            .setRows(BATCH_MIGRATION_SIZE)
            .setSort(Event.FIELD_ID, SolrQuery.ORDER.desc);
        // use cursor-based pagination
        solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
        QueryResponse queryResponse;
        List<SolrInputDocument> documentsToUpdate;
        do {
            try {
                queryResponse = core.getClient().query(solrQuery);
                documentList = queryResponse.getResults();
                totalNumber = queryResponse.getResults().getNumFound();
                if (!documentList.isEmpty()) {
                    documentsToUpdate = this.updateDocuments(documentList);
                    core.getClient().add(documentsToUpdate);
                    startIndex += BATCH_MIGRATION_SIZE;
                    totalMigrated += documentsToUpdate.size();
                    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, queryResponse.getNextCursorMark());
                    logger.info("[{}] Solr events information migrated on [{}].", totalMigrated, totalNumber);
                }
            } catch (SolrServerException | IOException e) {
                throw new SolrException("Error when performing 171000000 Solr events documents migration", e);
            }
        } while (queryResponse.getResults().size() == BATCH_MIGRATION_SIZE);

        if (totalMigrated > 0) {
            // We commit when all documents are migrated.
            try {
                core.getClient().commit();
            } catch (SolrServerException | IOException e) {
                throw new SolrException("Error when committing after performing 171000000 Solr "
                    + "events documents migration.",
                    e);
            }
        }
    }

    private List<SolrInputDocument> updateDocuments(SolrDocumentList documentList)
    {
        List<SolrInputDocument> documentsToUpdate = new ArrayList<>(documentList.size());
        for (SolrDocument solrDocument : documentList) {
            String id = this.solrUtils.getId(solrDocument);
            Date date = this.solrUtils.get(Event.FIELD_DATE, solrDocument);

            SolrInputDocument solrInputDocument = new SolrInputDocument();
            this.solrUtils.set(EventsSolrCoreInitializer.SOLR_FIELD_ID, id, solrInputDocument);
            this.solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, Event.FIELD_PREFILTERING_DATE,
                date, solrInputDocument);
            documentsToUpdate.add(solrInputDocument);
        }
        return documentsToUpdate;
    }
}
