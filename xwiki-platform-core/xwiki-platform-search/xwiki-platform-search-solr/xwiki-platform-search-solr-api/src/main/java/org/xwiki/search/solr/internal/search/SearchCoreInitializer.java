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

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.RemoteSolr;
import org.xwiki.search.solr.internal.SolrSchemaUtils;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

/**
 * Take care of the initialization of the search core which can be done through the SolrJ API.
 *
 * @version $Id$
 * @since 17.8.0RC1
 */
@Component
@Named(SearchCoreInitializer.CORE_NAME)
@Singleton
public class SearchCoreInitializer implements SolrCoreInitializer
{
    /**
     * The name of the core containing the XWiki search index.
     */
    public static final String CORE_NAME = "search";

    private static final int MAX_LOGGED_FIELDS = 10;

    @Inject
    private SearchCoreMigrationManager migrations;

    @Inject
    private SolrSchemaUtils solrSchema;

    @Inject
    private Provider<Solr> solrProvider;

    @Inject
    private SolrConfiguration configuration;

    @Inject
    private Logger logger;

    @Override
    public String getCoreName()
    {
        return CORE_NAME;
    }

    @Override
    public boolean isCache()
    {
        // The search index is filled again from the database when it's lost
        return true;
    }

    @Override
    public void initialize(XWikiSolrCore core) throws SolrException
    {
        this.migrations.update(core);

        // Make sure the data of the index can still be updated with the current schema. Only needed for a remote Solr,
        // whose configuration is upgraded independently of its data: the embedded Solr already recreates the search
        // core (configuration and data) when its configuration changes.
        if (RemoteSolr.TYPE.equals(this.configuration.getServerType())) {
            checkIndexCompliance(core);
        }
    }

    private void checkIndexCompliance(XWikiSolrCore core)
    {
        Set<String> nonCompliantFields;
        try {
            nonCompliantFields = this.solrSchema.getNonCompliantFields(core);
        } catch (Exception e) {
            // The check is only a safety net, not being able to do it should not prevent the core from being used
            this.logger.warn("Failed to check if the index of the Solr core [{}] is compliant with its schema: {}",
                core.getSolrName(), ExceptionUtils.getRootCauseMessage(e));

            return;
        }

        if (!nonCompliantFields.isEmpty()) {
            String loggedFields = toLoggedFields(nonCompliantFields);
            String message = "The index of the Solr core [{}] contains fields which are not compatible with their"
                + " definition in the schema {} (usually because the configuration of the core was upgraded), deleting"
                + " its index.";
            if (this.configuration.synchronizeAtStartup()) {
                message += " It will be filled again by the synchronization with the database.";
            } else {
                message += " Since solr.synchronizeAtStartup is disabled, it needs to be filled again by triggering a"
                    + " synchronization with the database manually.";
            }
            this.logger.warn(message, core.getSolrName(), loggedFields);

            deleteIndex(core);
        }
    }

    private String toLoggedFields(Set<String> fields)
    {
        // Each dynamic field of a property counts, so the list can be very long
        StringBuilder builder = new StringBuilder("[");
        builder.append(fields.stream().limit(MAX_LOGGED_FIELDS).collect(Collectors.joining(", ")));
        if (fields.size() > MAX_LOGGED_FIELDS) {
            builder.append(String.format(", and %d more", fields.size() - MAX_LOGGED_FIELDS));
        }
        builder.append(']');

        return builder.toString();
    }

    private void deleteIndex(XWikiSolrCore core)
    {
        try {
            this.solrProvider.get().deleteIndex(core);
        } catch (Exception e) {
            // Don't prevent the core from being used for searching. Retrying at the next initialization would not work:
            // once the documents are deleted, the index does not contain any non-compliant field anymore, but the
            // indexing keeps failing until the core is reloaded.
            this.logger.error("Failed to delete the index of the Solr core [{}]. If its documents were deleted but the"
                + " core could not be reloaded, the core can't be filled again until it's reloaded (with the CoreAdmin"
                + " RELOAD action or by restarting Solr).", core.getSolrName(), e);
        }
    }
}
