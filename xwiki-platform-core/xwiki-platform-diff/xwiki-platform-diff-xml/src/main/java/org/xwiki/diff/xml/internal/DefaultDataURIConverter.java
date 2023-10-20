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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.diff.DiffException;
import org.xwiki.diff.xml.XMLDiffDataURIConverterConfiguration;
import org.xwiki.url.URLSecurityManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Default Implementation of {@link DataURIConverter} that uses an HTTP client to embed images.
 *
 * @version $Id$
 * @since 11.10.1
 * @since 12.0RC1
 */
@Component
@Singleton
public class DefaultDataURIConverter implements Initializable, Disposable, DataURIConverter
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private URLSecurityManager urlSecurityManager;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private ImageDownloader imageDownloader;

    @Inject
    private XMLDiffDataURIConverterConfiguration configuration;

    private Cache<String> cache;

    private Cache<DiffException> failureCache;

    @Override
    public void initialize() throws InitializationException
    {
        if (!this.configuration.isEnabled()) {
            return;
        }

        CacheConfiguration cacheConfig = new CacheConfiguration();
        cacheConfig.setConfigurationId("diff.html.dataURI");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(100);
        cacheConfig.put(EntryEvictionConfiguration.CONFIGURATIONID, lru);

        CacheConfiguration failureCacheConfiguration = new CacheConfiguration();
        failureCacheConfiguration.setConfigurationId("diff.html.dataURIFailureCache");
        LRUEvictionConfiguration failureLRU = new LRUEvictionConfiguration();
        failureLRU.setMaxEntries(1000);
        // Cache failures for an hour. This is to avoid hammering the server with requests for images that don't
        // exist or are inaccessible or too large.
        failureLRU.setLifespan(3600);
        failureCacheConfiguration.put(EntryEvictionConfiguration.CONFIGURATIONID, failureLRU);

        try {
            this.cache = this.cacheManager.createNewCache(cacheConfig);
            this.failureCache = this.cacheManager.createNewCache(failureCacheConfiguration);
        } catch (Exception e) {
            // Dispose the cache if it has been created.
            if (this.cache != null) {
                this.cache.dispose();
            }
            throw new InitializationException("Failed to create the Data URI cache.", e);
        }
    }

    @Override
    public void dispose()
    {
        if (this.cache != null) {
            this.cache.dispose();
        }
        if (this.failureCache != null) {
            this.failureCache.dispose();
        }
    }

    /**
     * Convert the given URL to an absolute URL using the request URL from the given context.
     *
     * @param url the URL to convert
     * @param xcontext the XWiki context
     * @return the absolute URL
     * @throws DiffException if the URL cannot be converted due to being malformed
     */
    protected URL getAbsoluteURL(String url, XWikiContext xcontext) throws DiffException
    {
        URL absoluteURL;
        try {
            if (xcontext.getRequest() != null) {
                URL requestURL = XWiki.getRequestURL(xcontext.getRequest());
                absoluteURL = new URL(requestURL, url);
            } else {
                absoluteURL = new URL(url);
            }
        } catch (MalformedURLException | XWikiException e) {
            throw new DiffException(String.format("Failed to resolve [%s] to an absolute URL.", url), e);
        }
        return absoluteURL;
    }

    /**
     * Get a data URI for the given content and content type.
     *
     * @param contentType the content type
     * @param content the content
     * @return the data URI
     */
    protected static String getDataURI(String contentType, byte[] content)
    {
        return String.format("data:%s;base64,%s", contentType, Base64.getEncoder().encodeToString(content));
    }

    /**
     * Compute a cache key based on the current user and the URL.
     *
     * @param url the url
     * @return the cache key
     */
    private String getCacheKey(URL url)
    {
        String userPart = this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE);
        // Prepend the length of the user part to avoid any kind of confusion between user and URL.
        return String.format("%d:%s:%s", userPart.length(), userPart, url.toString());
    }

    @Override
    public String convert(String url) throws DiffException
    {
        if (url.startsWith("data:") || !this.configuration.isEnabled()) {
            // Already data URI.
            return url;
        }

        // Convert URL to absolute URL to avoid issues with relative URLs that might reference different images
        // in different subwikis.
        URL absoluteURL = getAbsoluteURL(url, this.xcontextProvider.get());

        String cacheKey = getCacheKey(absoluteURL);

        try {
            String dataURI = this.cache.get(cacheKey);

            if (dataURI == null) {
                DiffException failure = this.failureCache.get(cacheKey);

                if (failure != null) {
                    throw failure;
                }

                dataURI = convert(absoluteURL);
                this.cache.set(cacheKey, dataURI);
            }

            return dataURI;
        } catch (IOException | URISyntaxException e) {
            DiffException diffException = new DiffException("Failed to convert [" + url + "] to data URI.", e);
            this.failureCache.set(cacheKey, diffException);
            throw diffException;
        }
    }

    private String convert(URL url) throws IOException, URISyntaxException
    {
        if (!this.urlSecurityManager.isDomainTrusted(url)) {
            throw new IOException(String.format("The URL [%s] is not trusted.", url));
        }

        ImageDownloader.DownloadResult downloadResult = this.imageDownloader.download(url.toURI());

        return getDataURI(downloadResult.getContentType(), downloadResult.getData());
    }
}
