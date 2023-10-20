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
package org.xwiki.extension.index.internal.migration;

import java.io.IOException;

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
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.index.internal.ExtensionIndexSolrUtil;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.xwiki.extension.Extension.FIELD_ID;
import static org.xwiki.extension.InstalledExtension.FIELD_INSTALLED_NAMESPACES;
import static org.xwiki.extension.index.internal.ExtensionIndexSolrCoreInitializer.NAME;
import static org.xwiki.search.solr.AbstractSolrCoreInitializer.SOLR_FIELD_ID;

/**
 * Update the {@link InstalledExtension#FIELD_INSTALLED_NAMESPACES} field of extensions, because they were not updated
 * on extension upgrade before.
 *
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
 */
@Component
@Singleton
@Named(R150501000XWIKI21136DataMigration.HINT)
public class R150501000XWIKI21136DataMigration implements HibernateDataMigration
{
    /**
     * Hint for this migration.
     */
    public static final String HINT = "R150501000XWIKI21136";

    @Inject
    private Execution execution;

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private ExtensionIndexSolrUtil extensionIndexSolrUtil;

    @Override
    public String getName()
    {
        return HINT;
    }

    @Override
    public String getDescription()
    {
        return "Update the installedNamespaces of indexed extensions, because they were not updated on upgrade "
            + "previously";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(150501000);
    }

    @Override
    public void migrate() throws DataMigrationException
    {
        try {
            migrateInstalledExtensions(this.solr.getClient(NAME));
        } catch (SolrException e) {
            throw new DataMigrationException("Failed to get the extension index Solr core", e);
        }
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // We only need to execute this migration once.
        return getXWikiContext().isMainWiki();
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog()
    {
        return null;
    }

    @Override
    public String getLiquibaseChangeLog()
    {
        return null;
    }

    /**
     * @return XWikiContext to access the store
     */
    private XWikiContext getXWikiContext()
    {
        ExecutionContext context = this.execution.getContext();
        return (XWikiContext) context.getProperty("xwikicontext");
    }

    /**
     * Before XWiki 15.6-rc1/15.5.1, the old version of updated installed extensions was not correctly updated.
     * Therefore, the old version was still considered as installed on some namespace after the upgrade. We need to
     * define a migration to clean up outdated namespaces on old upgraded versions.
     *
     * @throws SolrException in case of issue when updating the extensions
     */
    private void migrateInstalledExtensions(SolrClient client) throws SolrException
    {
        try {
            int batchSize = 10000;
            int start = 0;

            SolrDocumentList results = updateBatch(client, batchSize, start);
            while (results.size() >= batchSize) {
                start = start + 1;
                results = updateBatch(client, batchSize, start);
            }
            client.commit();
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Failed to update the namespaces of installed extensions", e);
        }
    }

    private SolrDocumentList updateBatch(SolrClient client, int batchSize, int start)
        throws SolrServerException, IOException
    {
        SolrQuery solrQuery = new SolrQuery()
            .setRows(batchSize)
            .setStart(start)
            .setFilterQueries(FIELD_INSTALLED_NAMESPACES + ":[* TO *]")
            .setFields(FIELD_ID);
        QueryResponse query = client.query(solrQuery);
        SolrDocumentList results = query.getResults();
        for (SolrDocument doc : results) {
            SolrInputDocument updateDocument = new SolrInputDocument();
            String solrId = this.solrUtils.getId(doc);
            this.solrUtils.set(SOLR_FIELD_ID, solrId, updateDocument);
            this.extensionIndexSolrUtil.updateInstalledState(this.extensionIndexSolrUtil.fromSolrId(solrId),
                updateDocument);

            client.add(updateDocument);
        }
        return results;
    }
}
