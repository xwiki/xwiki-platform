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
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
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
     * 
     * @since 13.3RC1
     * @since 12.10.7
     */
    public static final String DEFAULT_BASE_URL = "http://localhost:8983/solr";

    /**
     * The name of the core containing the XWiki search index.
     */
    public static final String DEFAULT_CORE_PREFIX = "xwiki";

    /**
     * Only used by unit test to disable the server version gathering at init.
     */
    private static final boolean REQUEST_VERSION =
        Boolean.parseBoolean(System.getProperty("xwiki.solr.remote.requestVersion", "true"));

    @Inject
    private SolrConfiguration configuration;

    private HttpSolrClient rootClient;

    private int solrMajorVersion;

    @Override
    public void initialize() throws InitializationException
    {
        String baseURL = this.configuration.getInstanceConfiguration(TYPE, "baseURL", null);

        // RETRO COMPATIBILITY: the search core used to be configured using "solr.remote.url" property
        String searchCoreURL = this.configuration.getInstanceConfiguration(TYPE, "url", null);
        if (searchCoreURL != null) {
            this.cores.put(SolrClientInstance.CORE_NAME, new DefaultXWikiSolrCore(SolrClientInstance.CORE_NAME,
                toSolrCoreName(SolrClientInstance.CORE_NAME), new HttpSolrClient.Builder(searchCoreURL).build()));

            // If the base URL is not provided try to guess it from the search core URL
            if (baseURL == null) {
                baseURL = searchCoreURL.substring(0, searchCoreURL.lastIndexOf('/'));

                this.logger.warn("[solr.remote.url] property in xwiki.properties file is deprecated, "
                    + "use [solr.remote.baseURL] instead");
            }
        }

        // Fallback on the default base URL
        if (baseURL == null) {
            baseURL = DEFAULT_BASE_URL;
        }

        // Create the root client
        this.rootClient = new HttpSolrClient.Builder(baseURL).build();

        if (REQUEST_VERSION) {
            // Gather information about the server
            NamedList<Object> systemInfo;
            try {
                systemInfo = this.rootClient
                    .request(new GenericSolrRequest(SolrRequest.METHOD.GET, CommonParams.SYSTEM_INFO_PATH));
            } catch (Exception e) {
                throw new InitializationException("Failed to access the Solr server information", e);
            }
            String version = (String) systemInfo.findRecursive("lucene", "solr-impl-version");
            if (version != null) {
                this.solrMajorVersion = Integer.parseInt(StringUtils.substringBefore(version, '.'));
            } else {
                throw new InitializationException(
                    "The Solr server does not give any information about its version in lucene -> solr-impl-version."
                        + " Received [" + systemInfo + "].");
            }
        }
    }

    HttpSolrClient getRootClient()
    {
        return this.rootClient;
    }

    @Override
    protected int getSolrMajorVersion()
    {
        return this.solrMajorVersion;
    }

    @Override
    protected SolrClient getInternalSolrClient(String solrCoreName)
    {
        return new HttpSolrClient.Builder(getRootClient().getBaseURL() + '/' + solrCoreName).build();
    }

    private String getCorePrefix()
    {
        return this.configuration.getInstanceConfiguration(TYPE, "corePrefix", DEFAULT_CORE_PREFIX);
    }

    @Override
    protected String toSolrCoreName(String xwikiCoreName, int majorVersion)
    {
        StringBuilder builder = new StringBuilder();

        // Prefix Solr cores to avoid collision with other non-xwiki cores
        builder.append(getCorePrefix());

        // In Solr 8 the name of the search core was just "<prefix>"
        if (majorVersion > 8 || !xwikiCoreName.equals(SolrClientInstance.CORE_NAME)) {
            builder.append('_');

            builder.append(super.toSolrCoreName(xwikiCoreName, majorVersion));
        }

        return builder.toString();
    }

    @Override
    protected String toXWikiCoreName(String solrCoreName)
    {
        String prefixedCoreName = super.toXWikiCoreName(solrCoreName);

        return StringUtils.removeStart(prefixedCoreName, getCorePrefix());
    }

    @Override
    protected SolrClient createSolrClient(String solrCoreName, boolean isCache) throws SolrException
    {
        CoreAdminRequest coreAdminRequest = new CoreAdminRequest.Create();

        coreAdminRequest.setCoreName(solrCoreName);

        try {
            coreAdminRequest.process(getRootClient());
        } catch (Exception e) {
            throw new SolrException("Failed to create a new core", e);
        }

        return getInternalSolrClient(solrCoreName);
    }
}
