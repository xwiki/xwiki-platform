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
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrInstance;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

/**
 * Migration in charge of removing from the Solr search core all documents with objects so that they are re-indexed
 * properly.
 *
 * @version $Id$
 * @since 17.10.9
 * @since 18.5.0RC1
 */
@Component
@Named(R171009000XWIKI24390DataMigration.HINT)
@Singleton
// Note that we implement HibernateDataMigration and not DataMigration only because of XWIKI-19399
public class R171009000XWIKI24390DataMigration implements HibernateDataMigration
{
    /**
     * Hint of the migration.
     */
    public static final String HINT = "R171009000XWIKI24390";

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
        return "Remove from the Solr search core index all the documents containing xobjects"
            + " so that they are reindexed properly.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // Change to 171009000 for 17.10.9.
        return new XWikiDBVersion(180500000);
    }

    @Override
    public void migrate() throws DataMigrationException
    {
        try {
            // Only select documents with objects to limit the number of documents to reindex
            this.solrInstance.deleteByQuery(FieldUtils.CLASS + ":[* TO *]");
            this.solrInstance.commit();
        } catch (SolrServerException | IOException e) {
            throw new DataMigrationException("Error while performing Solr query to empty the search core", e);
        }
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        XWikiDBVersion ltsVersion = new XWikiDBVersion(171009000);
        XWikiDBVersion afterLTSVersion = new XWikiDBVersion(180000000);
        // Execute the migration if the version is either before the LTS version or equal to or larger than the
        // afterLTSVersion and before the version of this migration.
        // We only need to execute this migration once on the main wiki.
        return getXWikiContext().isMainWiki() && (startupVersion.compareTo(ltsVersion) < 0
            || (startupVersion.compareTo(afterLTSVersion) >= 0 && startupVersion.compareTo(getVersion()) < 0));
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
