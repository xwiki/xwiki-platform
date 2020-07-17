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
 * Initializer of the Solr core for ratings.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Component
@Named(RatingCoreSolrInitializer.NAME)
@Singleton
public class RatingCoreSolrInitializer extends AbstractSolrCoreInitializer
{
    /**
     * Name of the Solr core.
     */
    public static final String NAME = "rating";

    private static final long CURRENT_VERSION = 120700000;

    @Override
    protected void createSchema() throws SolrException
    {
        this.addPDateField(RatingsManager.RATING_CLASS_FIELDNAME_DATE, false, false);
        this.addStringField(RatingsManager.RATING_CLASS_FIELDNAME_AUTHOR, false, false);
        this.addStringField(RatingsManager.RATING_CLASS_FIELDNAME_PARENT, false, false);
        this.addPIntField(RatingsManager.RATING_CLASS_FIELDNAME_VOTE, false, false);
        this.addStringField(SolrRatingsManager.RATING_ID_FIELDNAME, false, false);
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
}
