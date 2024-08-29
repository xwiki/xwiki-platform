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
package org.xwiki.diff.xml.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.xwiki.component.annotation.Component;
import org.xwiki.diff.xml.XMLDiffDataURIConverterConfiguration;

/**
 * Simple factory for HttpClientBuilder to help testing and set basic properties including user agent and timeouts.
 *
 * @since 14.10.15
 * @since 15.5.1
 * @since 15.6
 * @version $Id$
 */
@Component(roles = HttpClientBuilderFactory.class)
@Singleton
public class HttpClientBuilderFactory
{
    @Inject
    private XMLDiffDataURIConverterConfiguration configuration;

    /**
     * @return a new HTTPClientBuilder
     */
    public HttpClientBuilder create()
    {
        HttpClientBuilder result = HttpClientBuilder.create();
        result.useSystemProperties();
        result.setUserAgent("XWikiHTMLDiff");

        // Set the connection timeout.
        Timeout timeout = Timeout.ofSeconds(this.configuration.getHTTPTimeout());
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .build();

        BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
        cm.setConnectionConfig(connectionConfig);
        result.setConnectionManager(cm);

        // Set the response timeout.
        result.setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(timeout).build());

        return result;
    }
}
