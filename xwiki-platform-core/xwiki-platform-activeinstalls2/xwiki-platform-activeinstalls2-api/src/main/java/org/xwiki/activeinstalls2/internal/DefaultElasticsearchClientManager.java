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
package org.xwiki.activeinstalls2.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.xwiki.activeinstalls2.ActiveInstallsConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * Factory to get a singleton {@link co.elastic.clients.elasticsearch.ElasticsearchClient} instance since it's
 * threadsafe. The URL to connect to is defined in the Active Install configuration.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@Component
@Singleton
public class DefaultElasticsearchClientManager implements ElasticsearchClientManager, Initializable, Disposable
{
    @Inject
    private Logger logger;

    @Inject
    private ActiveInstallsConfiguration configuration;

    private ElasticsearchClient client;

    private ElasticsearchTransport transport;

    @Override
    public void initialize() throws InitializationException
    {
        String pingURL = this.configuration.getPingInstanceURL();
        if (!StringUtils.isEmpty(pingURL)) {
            // Create the low-level client
            RestClientBuilder restClientBuilder = RestClient.builder(getPingHost(pingURL));
            String path = getPingPath(pingURL);
            if (StringUtils.isNotEmpty(path)) {
                restClientBuilder = restClientBuilder.setPathPrefix(path);
            }
            RestClient restClient = restClientBuilder
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                    .useSystemProperties()
                    .setUserAgent(this.configuration.getUserAgent()))
                .build();
            // Create the transport with a Jackson mapper
            this.transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            // And create the API client
            this.client = new ElasticsearchClient(this.transport);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.transport != null) {
            try {
                // Closes the REST Client too.
                this.transport.close();
            } catch (IOException e) {
                throw new ComponentLifecycleException("Failed to close the Active Installs transport layer", e);
            }
        }
    }

    @Override
    public ElasticsearchClient getClient()
    {
        return this.client;
    }

    private HttpHost getPingHost(String pingURLString) throws InitializationException
    {
        URI pingURL = getPingURL(pingURLString);
        return new HttpHost(pingURL.getHost(), pingURL.getPort(), pingURL.getScheme());
    }

    private String getPingPath(String pingURLString) throws InitializationException
    {
        URI pingURL = getPingURL(pingURLString);
        return pingURL.getPath();
    }

    private URI getPingURL(String pingURLString) throws InitializationException
    {
        try {
            return new URI(pingURLString);
        } catch (URISyntaxException e) {
            throw new InitializationException(String.format("Invalid Active Installs URL: [%s]", pingURLString), e);
        }
    }
}
