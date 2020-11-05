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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

/**
 * Remote Solr instance communicating over HTTP.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(RemoteSolr.TYPE)
@Singleton
public class RemoteSolr extends AbstractSolr implements Initializable
{
    /**
     * Solr instance type for this implementation.
     */
    public static final String TYPE = "remote";

    /**
     * Default URL to use when none is specified.
     */
    public static final String DEFAULT_REMOTE_URL = "http://localhost:8983/solr/";

    /**
     * The name of the core containing the XWiki search index.
     */
    public static final String DEFAULT_CORE_PREFIX = "xwiki";

    @Inject
    private SolrConfiguration configuration;

    private HttpSolrClient rootClient;

    @Override
    public void initialize() throws InitializationException
    {
        String baseURL = this.configuration.getInstanceConfiguration(TYPE, "baseURL", DEFAULT_REMOTE_URL);

        this.rootClient = new HttpSolrClient.Builder(baseURL).build();

        // RETRO COMPATIBILITY: the seach core used to be configured using "solr.remote.url" property
        String searchCoreURL = this.configuration.getInstanceConfiguration(TYPE, "url", null);
        if (searchCoreURL != null) {
            this.clients.put(SolrClientInstance.CORE_NAME, new HttpSolrClient.Builder(searchCoreURL).build());
        }
    }

    @Override
    protected SolrClient getInternalSolrClient(String coreName)
    {
        // Prefix Solr cores to avoid collision with other non-xwiki cores

        StringBuilder corePath =
            new StringBuilder(this.configuration.getInstanceConfiguration(TYPE, "corePrefix", DEFAULT_CORE_PREFIX));

        if (!coreName.equals(SolrClientInstance.CORE_NAME)) {
            corePath.append('_');
            corePath.append(coreName);
        }

        return new HttpSolrClient.Builder(this.rootClient.getBaseURL() + '/' + corePath).build();
    }

    @Override
    protected SolrClient createCore(String coreName, Map<String, String> parameters) throws SolrException
    {
        CoreAdminRequest coreAdminRequest = new CoreAdminRequest.Create();

        coreAdminRequest.setCoreName(coreName);

        try {
            coreAdminRequest.process(this.rootClient);
        } catch (Exception e) {
            throw new SolrException("Failed to create a new core", e);
        }

        return getInternalSolrClient(coreName);
    }
}
