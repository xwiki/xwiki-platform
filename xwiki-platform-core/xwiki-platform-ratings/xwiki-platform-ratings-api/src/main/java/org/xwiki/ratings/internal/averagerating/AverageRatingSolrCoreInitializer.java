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
package org.xwiki.ratings.internal.averagerating;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

import org.xwiki.ratings.internal.averagerating.AverageRatingManager.AverageRatingQueryField;

/**
 * Solr Core initializer for Average rating data.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Singleton
@Named(AverageRatingSolrCoreInitializer.DEFAULT_AVERAGE_RATING_SOLR_CORE)
public class AverageRatingSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    /**
     * Name of the solr core.
     */
    public static final String DEFAULT_AVERAGE_RATING_SOLR_CORE = "averageRating";

    private static final long CURRENT_VERSION = 120900000;

    @Override
    protected void createSchema() throws SolrException
    {
        this.addStringField(AverageRatingQueryField.MANAGER_ID.getFieldName(), false, false);
        this.addStringField(AverageRatingQueryField.ENTITY_REFERENCE.getFieldName(), false, false);
        this.addStringField(AverageRatingQueryField.PARENTS.getFieldName(), true, false);
        this.addPFloatField(AverageRatingQueryField.AVERAGE_VOTE.getFieldName(), false, false);
        this.addPIntField(AverageRatingQueryField.TOTAL_VOTE.getFieldName(), false, false);
        this.addPIntField(AverageRatingQueryField.SCALE.getFieldName(), false, false);
        this.addPDateField(AverageRatingQueryField.UPDATED_AT.getFieldName(), false, false);
    }

    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        // No migration yet.
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
