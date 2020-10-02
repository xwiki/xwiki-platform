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
package org.xwiki.ratings.internal;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

/**
 * Solr core initializer for the rating informations.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Singleton
@Named(RatingSolrCoreInitializer.DEFAULT_RATING_SOLR_CORE)
public class RatingSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    /**
     * Name of the Solr core for ranking.
     */
    public static final String DEFAULT_RATING_SOLR_CORE = "rating";

    private static final long CURRENT_VERSION = 120900000;

    /**
     * Number of documents to consider at once for migration.
     */
    private static final int BATCH_MIGRATION_SIZE = 100;

    /**
     * Used to migrate all data from like solr core.
     */
    private static final String LIKE_SOLR_CORE = "like";

    /**
     * Old fields name needed for migrations.
     */
    private static final String OLD_PARENT_FIELD = "parent";
    private static final String OLD_DATE_FIELD = "date";

    /**
     * Needed for migrating likes.
     */
    @Inject
    private Solr solr;

    @Inject
    private SolrUtils solrUtils;

    @Override
    protected void createSchema() throws SolrException
    {
        this.addStringField(RatingsManager.RatingQueryField.MANAGER_ID.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.ENTITY_REFERENCE.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.ENTITY_TYPE.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.USER_REFERENCE.getFieldName(), false, false);
        this.addPIntField(RatingsManager.RatingQueryField.VOTE.getFieldName(), false, false);
        this.addPIntField(RatingsManager.RatingQueryField.SCALE.getFieldName(), false, false);
        this.addPDateField(RatingsManager.RatingQueryField.CREATED_DATE.getFieldName(), false, false);
        this.addPDateField(RatingsManager.RatingQueryField.UPDATED_DATE.getFieldName(), false, false);
    }


    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        if (cversion == 120700000) {
            migrateFrom120700000();
        }
    }

    /**
     * This will perform migration of Ratings stored in Solr Core from 12.7.
     *
     *  Changes to perform:
     *          Field "date" became "createdDate" and "updatedDate"
     *          Field "parent" became "reference"
     *          Field "ratingId" has been removed
     *          Field "managerId" has been added -> constant value
     *          Field "entityType" has been added -> constant value
     *          Field "scale" has been added -> constant value
     */
    private void migrateFrom120700000() throws SolrException
    {
        // Step 1: Add missing fields (only vote and authors remains identical)
        this.addStringField(RatingsManager.RatingQueryField.MANAGER_ID.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.ENTITY_REFERENCE.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.ENTITY_TYPE.getFieldName(), false, false);
        this.addPIntField(RatingsManager.RatingQueryField.SCALE.getFieldName(), false, false);
        this.addPDateField(RatingsManager.RatingQueryField.CREATED_DATE.getFieldName(), false, false);
        this.addPDateField(RatingsManager.RatingQueryField.UPDATED_DATE.getFieldName(), false, false);

        // Step 2: Loop on old ratings and migrate them
        this.migrateAllDocumentsFrom1207000000(this.client, 5, RatingsManagerFactory.DEFAULT_APP_HINT);

        // Step 3: Loop on old Likes and migrate them
        // (note that we copy them in the actual ratings core in order to migrate them)
        SolrClient likeClient = this.solr.getClient(LIKE_SOLR_CORE);
        // likeClient could be null here if for some reason the core was never initialized or if it has been removed.
        // TODO: Check in case it's a remote Solr core and it has not been created, it could be not null but still not
        // exist.
        if (likeClient != null) {
            this.migrateAllDocumentsFrom1207000000(likeClient, 1, LIKE_SOLR_CORE);
        }

        // Step 4: Remove old fields (only on the old ratings core, we don't need to perform that change
        this.deleteField(OLD_DATE_FIELD, false);
        this.deleteField(OLD_PARENT_FIELD, false);
        this.deleteField("ratingId", false);
        this.commit();
    }

    private void migrateAllDocumentsFrom1207000000(SolrClient solrClient, int scale, String managerId)
        throws SolrException
    {
        SolrDocumentList documentList;
        int startIndex = 0;
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
            } catch (SolrServerException | IOException e) {
                throw new SolrException("Error when executing query to perform 120700000 documents migration", e);
            }
        } while (!documentList.isEmpty());

        // We commit when all documents are migrated.
        try {
            this.client.commit();
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
            this.client.add(solrInputDocument);
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error when adding new document for performing 120700000 document migration", e);
        }
    }


    @Override
    protected long getVersion()
    {
        return CURRENT_VERSION;
    }
}
