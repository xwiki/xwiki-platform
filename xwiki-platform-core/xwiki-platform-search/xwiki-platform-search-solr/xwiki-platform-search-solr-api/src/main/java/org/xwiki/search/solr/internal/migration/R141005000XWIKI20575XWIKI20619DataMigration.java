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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.search.solr.internal.SolrClientInstance;
import org.xwiki.search.solr.internal.SolrSchemaUtils;
import org.xwiki.search.solr.internal.api.SolrInstance;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

/**
 * Migration in charge of emptying the Search solr core in order to perform a reindex of all documents after a
 * regression has been fixed.
 *
 * @version $Id$
 * @since 15.1RC
 * @since 14.10.5
 */
@Component
@Named(R141005000XWIKI20575XWIKI20619DataMigration.HINT)
@Singleton
// Note that we implement HibernateDataMigration and not DataMigration only because of XWIKI-19399
public class R141005000XWIKI20575XWIKI20619DataMigration implements HibernateDataMigration
{
    /**
     * Hint of the migration.
     */
    public static final String HINT = "R141005000XWIKI20575XWIKI20619";

    private static final String DYNAMIC_FIELD_ROOT_NAME = "*__";

    private static final String DYNAMIC_FIELD_PTBR_NAME = "*_pt_BR";

    @Inject
    private SolrInstance solrInstance;

    @Inject
    private Solr solr;

    @Inject
    private Execution execution;

    @Inject
    private SolrSchemaUtils solrSchemaUtils;

    @Inject
    private Logger logger;

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
        return "Add potentially missing dynamic field and empty the Solr Search Core to trigger indexing of pages"
            + " for fixing a regression.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(141005000);
    }

    @Override
    public void migrate() throws DataMigrationException
    {
        try {
            XWikiSolrCore core = this.solr.getCore(SolrClientInstance.CORE_NAME);

            Map<String, Map<String, Object>> dynamicFields = this.solrSchemaUtils.getDynamicFields(core, true);

            // Make sure the dynamic field for Local.ROOT is registered
            maybeAddField(core, DYNAMIC_FIELD_ROOT_NAME, DefaultSolrUtils.SOLR_TYPE_TEXT_GENERAL, dynamicFields);

            // Make sure the dynamic field for Local.ROOT is registered
            maybeAddField(core, DYNAMIC_FIELD_PTBR_NAME, "text_pt_BR", dynamicFields);

            this.solrInstance.deleteByQuery("*:*");
            this.solrInstance.commit();
        } catch (SolrServerException | IOException | SolrException e) {
            throw new DataMigrationException("Error while performing Solr query to empty the search core", e);
        }
    }

    private void maybeAddField(XWikiSolrCore core, String name, String type,
        Map<String, Map<String, Object>> dynamicFields) throws SolrException
    {
        if (!dynamicFields.containsKey(name)) {
            this.solrSchemaUtils.setField(core, name, type, true, "multiValued", true, "stored", true, "indexed", true);
            this.solrSchemaUtils.commit(core);

            this.logger.info("Missing dynamic field [{}] has been added.", name);
        } else {
            this.logger.info("Dynamic field [{}] was already defined.", name);
        }
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // We only need to execute this migration once.
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
