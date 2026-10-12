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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.util.NamedList;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
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

    private static final int UNKNOWN_VERSION = -1;

    @Inject
    private SolrConfiguration configuration;

    private HttpSolrClient rootClient;

    private volatile int solrMajorVersion = UNKNOWN_VERSION;

    @Override
    public void initialize() throws InitializationException
    {
        String baseURL = this.configuration.getInstanceConfiguration(TYPE, "baseURL", null);

        // RETRO COMPATIBILITY: the search core used to be configured using "solr.remote.url" property
        String searchCoreURL = this.configuration.getInstanceConfiguration(TYPE, "url", null);
        if (searchCoreURL != null) {
            this.cores.put(SolrClientInstance.CORE_NAME, new DefaultXWikiSolrCore(SolrClientInstance.CORE_NAME,
                toSolrCoreName(SolrClientInstance.CORE_NAME), new HttpSolrClient.Builder(searchCoreURL).build(), -1));

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

        // Gather information about the server, but don't fail: a Solr server which cannot be reached while XWiki
        // starts must not make this component, and all the components which depend on it, unavailable until XWiki is
        // restarted. The information is requested again when a core is needed.
        try {
            initializeServerInformation();
        } catch (SolrException e) {
            this.logger.warn("Failed to access the Solr server at [{}]: [{}]. The connection will be retried when a"
                + " Solr core is needed.", baseURL, ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();

        try {
            this.rootClient.close();
        } catch (IOException e) {
            this.logger.error("Failed to close Solr client", e);
        }
    }

    private Set<String> getCores() throws SolrServerException, IOException
    {
        CoreAdminRequest request = new CoreAdminRequest();
        request.setAction(CoreAdminAction.STATUS);
        CoreAdminResponse response = request.process(this.rootClient);

        Set<String> cores = new HashSet<>();
        for (int i = 0; i < response.getCoreStatus().size(); i++) {
            cores.add(response.getCoreStatus().getName(i));
        }

        return cores;
    }

    HttpSolrClient getRootClient()
    {
        return this.rootClient;
    }

    @Override
    public int getSolrMajorVersion()
    {
        return this.solrMajorVersion;
    }

    @Override
    public XWikiSolrCore getCore(String name) throws SolrException
    {
        initializeServerInformation();

        return super.getCore(name);
    }

    /**
     * Gather information about the Solr server, if not already done. A failure is not remembered: the server is asked
     * again the next time a core is requested.
     *
     * @throws SolrException when failing to get the information from the Solr server
     */
    private void initializeServerInformation() throws SolrException
    {
        if (this.solrMajorVersion == UNKNOWN_VERSION) {
            synchronized (this) {
                if (this.solrMajorVersion == UNKNOWN_VERSION) {
                    this.solrMajorVersion = requestSolrMajorVersion();
                }
            }
        }
    }

    private int requestSolrMajorVersion() throws SolrException
    {
        NamedList<Object> systemInfo;
        try {
            systemInfo =
                this.rootClient.request(new GenericSolrRequest(SolrRequest.METHOD.GET, CommonParams.SYSTEM_INFO_PATH));
        } catch (Exception e) {
            throw new SolrException(
                String.format("Failed to access the Solr server information at [%s]", this.rootClient.getBaseURL()), e);
        }

        String version = (String) systemInfo.findRecursive("lucene", "solr-impl-version");
        if (version == null) {
            throw new SolrException(
                "The Solr server does not give any information about its version in lucene -> solr-impl-version."
                    + " Received [" + systemInfo + "].");
        }

        return Integer.parseInt(StringUtils.substringBefore(version, '.'));
    }

    @Override
    protected XWikiSolrCore getCore(String xwikiCoreName, int solrMajorVersion) throws SolrException
    {
        String solrCoreName = toSolrCoreName(xwikiCoreName, solrMajorVersion);

        // Check if the core exists
        try {
            // Try to find a core designed for the current Solr version
            if (!getCores().contains(solrCoreName)) {
                return null;
            }
        } catch (Exception e) {
            throw new SolrException("Failed to get the list of cores", e);
        }

        // Create the client core handler
        SolrClient solrClient = new HttpSolrClient.Builder(getRootClient().getBaseURL() + '/' + solrCoreName).build();
        return new DefaultXWikiSolrCore(xwikiCoreName, solrCoreName, solrClient, getSolrMajorVersion());
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

        return Strings.CS.removeStart(prefixedCoreName, getCorePrefix());
    }
}
