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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.diff.DiffException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Default implementation of {@link DataURIConverter}.
 * 
 * @version $Id$
 * @since 11.10.1
 * @since 12.0RC1
 */
@Component
@Singleton
public class DefaultDataURIConverter implements DataURIConverter, Initializable
{
    private static final String HEADER_COOKIE = "Cookie";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private CacheManager cacheManager;

    private Cache<String> cache;

    @Override
    public void initialize() throws InitializationException
    {
        CacheConfiguration cacheConfig = new CacheConfiguration();
        cacheConfig.setConfigurationId("diff.html.dataURI");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(100);
        cacheConfig.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);
        try {
            this.cache = this.cacheManager.createNewCache(cacheConfig);
        } catch (Exception e) {
            throw new InitializationException("Failed to create the Data URI cache.", e);
        }
    }

    @Override
    public String convert(String url) throws DiffException
    {
        if (url.startsWith("data:")) {
            // Already data URI.
            return url;
        }

        String cachedDataURI = this.cache.get(url);
        if (cachedDataURI == null) {
            try {
                cachedDataURI = convert(getAbsoluteURI(url));
                this.cache.set(url, cachedDataURI);
            } catch (IOException | URISyntaxException e) {
                throw new DiffException("Failed to convert [" + url + "] to data URI.", e);
            }
        }

        return cachedDataURI;
    }

    private URL getAbsoluteURI(String relativeURL) throws MalformedURLException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        URL baseURL = xcontext.getURLFactory().getServerURL(xcontext);
        return new URL(baseURL, relativeURL);
    }

    private String convert(URL url) throws IOException, URISyntaxException
    {
        HttpEntity entity = fetch(url.toURI());
        // Remove the content type parameters, such as the charset, so they don't influence the diff.
        String contentType = StringUtils.substringBefore(entity.getContentType().getValue(), ";");
        byte[] content = IOUtils.toByteArray(entity.getContent());
        return String.format("data:%s;base64,%s", contentType, Base64.getEncoder().encodeToString(content));
    }

    private HttpEntity fetch(URI uri) throws IOException
    {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.useSystemProperties();
        httpClientBuilder.setUserAgent("XWikiHTMLDiff");

        CloseableHttpClient httpClient = httpClientBuilder.build();
        HttpGet getMethod = new HttpGet(uri);

        XWikiRequest request = this.xcontextProvider.get().getRequest();
        if (request != null) {
            // Copy the cookies from the current request.
            getMethod.setHeader(HEADER_COOKIE, request.getHeader(HEADER_COOKIE));
        }

        CloseableHttpResponse response = httpClient.execute(getMethod);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            return response.getEntity();
        } else {
            throw new IOException(statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
        }
    }
}
