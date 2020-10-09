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
package org.xwiki.ratings.internal.migration;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.internal.RatingSolrCoreInitializer;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

/**
 * A dedicated component for performing migration of Ratings Solr document before XWiki 12.9.
 * Note that this migration is used also for Likes documents since they are stored as Ratings.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component(roles = SolrDocumentMigration120900000.class)
@Singleton
public class SolrDocumentMigration120900000
{
    /**
     * Number of documents to consider at once for migration.
     */
    private static final int BATCH_MIGRATION_SIZE = 100;

    /**
     * Old fields name needed for migrations.
     */
    private static final String OLD_PARENT_FIELD = "parent";
    private static final String OLD_DATE_FIELD = "date";

    @Inject
    private Logger logger;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Solr solr;

    private SolrClient getClient() throws SolrException
    {
        return this.solr.getClient(RatingSolrCoreInitializer.DEFAULT_RATING_SOLR_CORE);
    }

    /**
     * Migrate all SolrDocument existing in the given client to the new Ratings Solr core.
     * This method should only be used in case of SolrDocument creating between XWiki 12.7 and 12.9 in Like and Ratings
     * applications.
     *
     * @param solrClient a client containing old Ratings SolrDocument
     * @param scale the default scale that was used (should be 5 for standard ratings and 1 for Likes)
     * @param managerId the name of the application to store the new ratings.
     * @throws SolrException in case of problem during the migration of the data.
     */
    public void migrateAllDocumentsFrom1207000000(SolrClient solrClient, int scale, String managerId)
        throws SolrException
    {
        SolrDocumentList documentList;
        int startIndex = 0;
        int totalMigrated = 0;
        do {
            SolrQuery solrQuery = new SolrQuery("*")
                .setStart(startIndex)
                .setRows(BATCH_MIGRATION_SIZE)
                .setSort(OLD_DATE_FIELD, SolrQuery.ORDER.asc);
            try {
                QueryResponse queryResponse = solrClient.query(solrQuery);
                documentList = queryResponse.getResults();
                for (SolrDocument solrDocument : documentList) {
                    this.migrateDocumentFrom120700000(solrDocument, scale, managerId);
                }
                startIndex += BATCH_MIGRATION_SIZE;
                totalMigrated += documentList.size();
                logger.info("[{}] {} information migrated.", totalMigrated, managerId);
            } catch (SolrServerException | IOException e) {
                throw new SolrException("Error when executing query to perform 120700000 documents migration", e);
            }
        } while (!documentList.isEmpty());

        // We commit when all documents are migrated.
        try {
            this.getClient().commit();
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error when committing after performing 120700000 documents migration.", e);
        }
    }

    private void migrateDocumentFrom120700000(SolrDocument solrDocument, int scale, String managerId)
        throws SolrException
    {
        String id = this.solrUtils.getId(solrDocument);
        Date date = this.solrUtils.get(OLD_DATE_FIELD, solrDocument);
        String documentReference = this.solrUtils.get(OLD_PARENT_FIELD, solrDocument);
        String entityType = EntityType.DOCUMENT.getLowerCase();
        int vote = this.solrUtils.get(RatingsManager.RatingQueryField.VOTE.getFieldName(), solrDocument);
        String author = this.solrUtils.get(RatingsManager.RatingQueryField.USER_REFERENCE.getFieldName(), solrDocument);

        SolrInputDocument solrInputDocument = new SolrInputDocument();
        this.solrUtils.setId(id, solrInputDocument);
        this.solrUtils.set(RatingsManager.RatingQueryField.MANAGER_ID.getFieldName(), managerId, solrInputDocument);
        this.solrUtils.set(RatingsManager.RatingQueryField.ENTITY_REFERENCE.getFieldName(), documentReference,
            solrInputDocument);
        this.solrUtils.set(RatingsManager.RatingQueryField.ENTITY_TYPE.getFieldName(), entityType, solrInputDocument);
        this.solrUtils.set(RatingsManager.RatingQueryField.SCALE.getFieldName(), scale, solrInputDocument);

        this.solrUtils.set(RatingsManager.RatingQueryField.VOTE.getFieldName(), vote, solrInputDocument);
        this.solrUtils.set(RatingsManager.RatingQueryField.USER_REFERENCE.getFieldName(), author, solrInputDocument);

        // Here the data is not completely accurate since the old "date" is more an updated date, but without more
        // information it's better to rely on it than having a null data.
        this.solrUtils.set(RatingsManager.RatingQueryField.CREATED_DATE.getFieldName(), date, solrInputDocument);
        this.solrUtils.set(RatingsManager.RatingQueryField.UPDATED_DATE.getFieldName(), date, solrInputDocument);

        // We use this.client here since we want to copy all documents in the current core, even if they come from
        // like core.
        try {
            this.getClient().add(solrInputDocument);
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error when adding new document for performing 120700000 document migration", e);
        }
    }
}
