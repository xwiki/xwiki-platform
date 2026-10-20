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

import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.SolrSchemaUtils;
import org.xwiki.search.solr.internal.api.FieldUtils;

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

    /**
     * The identifier of the document indicating the version of the core configuration ({@code __cversion}) for which
     * the data of the index was produced.
     */
    static final String INDEX_VERSION_ID = SolrSchemaUtils.SOLR_TYPENAME_CVERSION;

    /**
     * The field containing the version in the index version document. A dynamic field which has the same definition
     * in all the versions of the search core configuration, so that the document can always be indexed.
     */
    static final String INDEX_VERSION_FIELD = "cversion_long";

    @Inject
    private SearchCoreMigrationManager migrations;

    @Inject
    private SolrSchemaUtils solrSchema;

    @Inject
    private Provider<Solr> solrProvider;

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
        // Make sure the data of the index matches the configuration of the core
        checkIndexVersion(core);

        this.migrations.update(core);
    }

    private void checkIndexVersion(XWikiSolrCore core) throws SolrException
    {
        Long configurationVersion = this.solrSchema.getVersion(core, SolrSchemaUtils.SOLR_TYPENAME_CVERSION);

        // The index version document did not exist before the 18.10 configuration of the search core
        Long expectedVersion = configurationVersion != null
            && configurationVersion > AbstractSolrCoreInitializer.SCHEMA_VERSION_16_6 ? configurationVersion : null;

        Long indexVersion = getIndexVersion(core);

        if (!Objects.equals(expectedVersion, indexVersion)) {
            if (!isEmpty(core)) {
                this.logger.warn("The data of the Solr core [{}] was produced for a different version of its"
                    + " configuration ([{}] instead of [{}]), deleting its index. The index will be filled again by"
                    + " the synchronization with the database (if solr.synchronizeAtStartup is enabled, which is the"
                    + " default, otherwise it needs to be triggered manually).", core.getSolrName(), indexVersion,
                    expectedVersion);

                this.solrProvider.get().deleteIndex(core);
            }

            if (expectedVersion != null) {
                setIndexVersion(core, expectedVersion);
            }
        }
    }

    private Long getIndexVersion(XWikiSolrCore core) throws SolrException
    {
        SolrDocument document;
        try {
            document = core.getClient().getById(INDEX_VERSION_ID);
        } catch (Exception e) {
            throw new SolrException("Failed to get the index version of the Solr core [" + core.getSolrName() + "]",
                e);
        }

        if (document != null) {
            Object value = document.getFirstValue(INDEX_VERSION_FIELD);
            if (value instanceof Number number) {
                return number.longValue();
            }
        }

        return null;
    }

    private void setIndexVersion(XWikiSolrCore core, long version) throws SolrException
    {
        SolrInputDocument document = new SolrInputDocument();
        document.setField(FieldUtils.ID, INDEX_VERSION_ID);
        document.setField(INDEX_VERSION_FIELD, version);

        try {
            core.getClient().add(document);
            core.getClient().commit();
        } catch (Exception e) {
            throw new SolrException("Failed to set the index version of the Solr core [" + core.getSolrName() + "]",
                e);
        }
    }

    private boolean isEmpty(XWikiSolrCore core) throws SolrException
    {
        SolrQuery query = new SolrQuery("*:*");
        query.setRows(0);

        try {
            return core.getClient().query(query).getResults().getNumFound() == 0;
        } catch (Exception e) {
            throw new SolrException("Failed to check the content of the Solr core [" + core.getSolrName() + "]", e);
        }
    }
}
