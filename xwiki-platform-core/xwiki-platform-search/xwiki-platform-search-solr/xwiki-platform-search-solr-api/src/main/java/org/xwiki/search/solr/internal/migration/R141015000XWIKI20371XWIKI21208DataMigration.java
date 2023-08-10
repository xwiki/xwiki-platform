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
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.search.solr.internal.api.SolrInstance;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

/**
 * Migration in charge of emptying the Search solr core in order to perform a reindex of all documents after an
 * indexing bug has been fixed.
 *
 * @version $Id$
 * @since 14.10.15
 * @since 15.5.1
 * @since 15.7RC1
 */
@Component
@Named(R141015000XWIKI20371XWIKI21208DataMigration.HINT)
@Singleton
// Note that we implement HibernateDataMigration and not DataMigration only because of XWIKI-19399
public class R141015000XWIKI20371XWIKI21208DataMigration implements HibernateDataMigration
{
    /**
     * Hint of the migration.
     */
    public static final String HINT = "R141015000XWIKI20371XWIKI21208";

    @Inject
    private SolrInstance solrInstance;

    @Inject
    private Execution execution;

    /**
     * @return XWikiContext to access the store
     */
    private XWikiContext getXWikiContext()
    {
        ExecutionContext context = this.execution.getContext();
        return (XWikiContext) context.getProperty("xwikicontext");
    }

    @Override
    public String getName()
    {
        return HINT;
    }

    @Override
    public String getDescription()
    {
        return "Clear the index to purge sensitive data from the index.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(141015000);
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
        // We only need to execute this migration once on the main wiki.
        return getXWikiContext().isMainWiki();
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        return null;
    }

    @Override
    public String getLiquibaseChangeLog() throws DataMigrationException
    {
        return null;
    }
}
