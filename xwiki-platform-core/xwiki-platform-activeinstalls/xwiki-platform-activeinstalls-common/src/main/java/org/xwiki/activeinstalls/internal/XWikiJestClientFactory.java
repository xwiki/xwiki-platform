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
package org.xwiki.activeinstalls.internal;

import org.apache.http.impl.client.HttpClientBuilder;
import org.xwiki.activeinstalls.ActiveInstallsConfiguration;

import io.searchbox.client.JestClientFactory;

/**
 * Ensure that Jest works through an HTTP Proxy.
 *
 * @version $Id$
 * @since 6.1
 */
public class XWikiJestClientFactory extends JestClientFactory
{
    private ActiveInstallsConfiguration configuration;

    /**
     * @param configuration the object from which to extract the user agent to use when sending HTTP request (pings) to
     *            the remote server
     */
    public XWikiJestClientFactory(ActiveInstallsConfiguration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder)
    {
        HttpClientBuilder modifiedBuilder = builder.useSystemProperties();
        modifiedBuilder.setUserAgent(this.configuration.getUserAgent());
        return modifiedBuilder;
    }
}
