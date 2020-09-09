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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.ratings.RatingsManager;
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
@Named(RatingSolrCoreInitializer.DEFAULT_RATING_SOLR_CORE)
public class RatingSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    /**
     * Name of the Solr core for ranking.
     */
    public static final String DEFAULT_RATING_SOLR_CORE = "rating";

    private static final long CURRENT_VERSION = 120900000;

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
        // TODO: Right proper migrations for old ratings + like cores
        // Changes to perform:
        // Field "date" became "createdDate" and "updatedDate"
        // Field "parent" became "reference"
        // Field "ratingId" has been removed (need to double check if the ID need to be computed back or not)
        // Field "managerId" has been added -> constant value
        // Field "entityType" has been added -> constant value
        // Field "scale" has been added -> constant value
    }

    @Override
    protected long getVersion()
    {
        return CURRENT_VERSION;
    }
}
