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


import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.ratings.internal.migration.SolrDocumentMigration120900000;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

/**
 * Solr core initializer for the rating informations.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Singleton
@Named(RatingSolrCoreInitializer.DEFAULT_RATINGS_SOLR_CORE)
public class RatingSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    /**
     * Name of the Solr core for rating.
     */
    public static final String DEFAULT_RATINGS_SOLR_CORE = "ratings";

    private static final long CURRENT_VERSION = 120900000;

    @Inject
    private SolrDocumentMigration120900000 solrDocumentMigration120900000;

    @Override
    protected void createSchema() throws SolrException
    {
        this.addStringField(RatingsManager.RatingQueryField.MANAGER_ID.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.ENTITY_REFERENCE.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.PARENTS_REFERENCE.getFieldName(), true, false);
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
     *          Field "parents" has been added
     *          Field "managerId" has been added -> constant value
     *          Field "entityType" has been added -> constant value
     *          Field "scale" has been added -> constant value
     */
    private void migrateFrom120700000() throws SolrException
    {
        this.logger.info("Starting migration of ratings solr core schema.");

        // Step 1: Add missing fields (only vote and authors remains identical)
        this.addStringField(RatingsManager.RatingQueryField.MANAGER_ID.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.ENTITY_REFERENCE.getFieldName(), false, false);
        this.addStringField(RatingsManager.RatingQueryField.PARENTS_REFERENCE.getFieldName(), true, false);
        this.addPIntField(RatingsManager.RatingQueryField.SCALE.getFieldName(), false, false);
        this.addPDateField(RatingsManager.RatingQueryField.CREATED_DATE.getFieldName(), false, false);
        this.addPDateField(RatingsManager.RatingQueryField.UPDATED_DATE.getFieldName(), false, false);

        this.logger.info("Ratings Solr Core schema migrated. Starting migration of already existing ratings.");
        // Step 2: Loop on old ratings and migrate them

        // We explicitely don't retrieve scale from config here since the previous ratings
        // were done with a scale of 5.
        int scaleUpperBound = 5;
        this.solrDocumentMigration120900000
            .migrateAllDocumentsFrom1207000000(this.core, scaleUpperBound, RatingsManagerFactory.DEFAULT_APP_HINT);

        // Step 3: Remove old fields (only on the old ratings core, we don't need to perform that change
        this.deleteField("date", false);
        this.deleteField("parent", false);
        this.deleteField("ratingId", false);
        this.commit();
        this.logger.info("Ratings Solr Core migration finished.");
    }




    @Override
    protected long getVersion()
    {
        return CURRENT_VERSION;
    }

    @Override
    protected int getMigrationBatchRows()
    {
        return 10000;
    }
}
