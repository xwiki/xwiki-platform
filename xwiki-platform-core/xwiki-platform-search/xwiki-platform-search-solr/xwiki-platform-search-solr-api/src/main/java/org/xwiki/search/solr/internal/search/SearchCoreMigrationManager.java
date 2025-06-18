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
package org.xwiki.search.solr.internal.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.SolrSchemaUtils;

/**
 * Class in charge of updating the search code through migrations.
 * 
 * @version $Id$
 * @since 17.8.0RC1
 */
@Component(roles = SearchCoreMigrationManager.class)
@Singleton
public class SearchCoreMigrationManager implements Initializable
{
    /**
     * The name of the version used to store the migration.
     */
    private static final String SOLR_TYPENAME_CMVERSION = "__cmversion";

    @Inject
    private SolrSchemaUtils solrSchema;

    @Inject
    private List<SearchCoreMigration> migrations;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        // Sort the migrations
        this.migrations = new ArrayList<>(this.migrations);
        Collections.sort(this.migrations);
    }

    /**
     * Execute migrations on the search core (if needed).
     * 
     * @param core the core to update
     * @throws SolrException when failing to update the Solr core
     */
    public void update(XWikiSolrCore core) throws SolrException
    {
        // Get the current version
        Long currentVersion = this.solrSchema.getVersion(core, SOLR_TYPENAME_CMVERSION);

        this.logger.info("Current Solr core migration version is [{}]", currentVersion);

        // Execute migrations more recent that the current version
        for (SearchCoreMigration migration : this.migrations) {
            if (currentVersion == null || migration.getVersion() > currentVersion) {
                this.logger.info("Starting the Solr search core migration [{}]", migration.getVersion());

                executeMigration(migration, core);

                this.logger.info("Finished the Solr search core migration [{}]", migration.getVersion());
            }
        }
    }

    private void executeMigration(SearchCoreMigration migration, XWikiSolrCore core) throws SolrException
    {
        // Execute the migration
        migration.migrate(core);

        // Update the version
        this.solrSchema.setVersion(core, SOLR_TYPENAME_CMVERSION, migration.getVersion());
        this.solrSchema.commit(core);
    }
}
