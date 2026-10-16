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
package org.xwiki.search.solr.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.search.SearchCoreInitializer;

/**
 * A wrapper around the new {@link Solr} API for the search core.
 * <p>
 * The search core is requested during the initialization of this component. With a remote Solr server, a failure does
 * not fail the initialization: the search core is then requested again when needed, so that a Solr server which cannot
 * be reached while XWiki starts does not make this component, and the components depending on it (event listeners for
 * example), unavailable until XWiki is restarted. With the embedded Solr, a search core which cannot be loaded still
 * fails the initialization.
 * 
 * @version $Id$
 * @since 12.2
 */
@Component
@Singleton
public class SolrClientInstance extends AbstractSolrInstance
{
    /**
     * The name of the core containing the XWiki search index.
     */
    public static final String CORE_NAME = SearchCoreInitializer.CORE_NAME;

    /**
     * The search core, requested during the initialization of this component and again when needed if it was not
     * available then.
     */
    @Inject
    private LazySolrCore searchCore;

    @Override
    public void initialize() throws InitializationException
    {
        // Get the search core now so that it is created, migrated and initialized during the startup. With a remote
        // Solr server, a failure is not fatal: the search core is requested again when needed.
        try {
            this.server = this.searchCore.initialize(CORE_NAME);
        } catch (SolrException e) {
            throw new InitializationException(String.format("Failed to get the Solr core [%s]", CORE_NAME), e);
        }
    }

    @Override
    protected SolrClient getClient() throws SolrServerException
    {
        XWikiSolrCore solrCore = this.server;

        if (solrCore == null) {
            try {
                solrCore = this.searchCore.get();
            } catch (SolrException e) {
                throw new SolrServerException(e.getMessage(), e);
            }

            this.server = solrCore;
        }

        return solrCore.getClient();
    }
}
