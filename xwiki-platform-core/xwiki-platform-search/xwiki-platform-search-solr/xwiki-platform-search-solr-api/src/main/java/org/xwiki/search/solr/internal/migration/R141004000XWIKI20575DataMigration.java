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
package org.xwiki.search.solr.internal.migration;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServerException;
import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.internal.api.SolrInstance;

import com.xpn.xwiki.store.migration.DataMigration;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration in charge of emptying the Search solr core in order to perform a reindex of all documents after a
 * regression has been fixed.
 *
 * @version $Id$
 * @since 15.0
 * @since 14.10.4
 */
@Component
@Named(R141004000XWIKI20575DataMigration.HINT)
@Singleton
public class R141004000XWIKI20575DataMigration implements DataMigration
{
    /**
     * Hint of the migration.
     */
    public static final String HINT = "R141004000XWIKI20575";

    @Inject
    private SolrInstance solrInstance;

    @Override
    public String getName()
    {
        return HINT;
    }

    @Override
    public String getDescription()
    {
        return "Empty the Solr Search Core to trigger indexing of pages for fixing a regression.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(141004000);
    }

    @Override
    public void migrate() throws DataMigrationException
    {
        try {
            this.solrInstance.deleteByQuery("*:*");
            this.solrInstance.commit();
        } catch (SolrServerException | IOException e) {
            throw new DataMigrationException("Error while performing Solr query to empty the search core", e);
        }
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return true;
    }
}
