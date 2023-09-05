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
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.diff.DiffException;
import org.xwiki.diff.xml.XMLDiffDataURIConverterConfiguration;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.URLSecurityManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultDataURIConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultDataURIConverterTest
{
    private static final String CURRENT_USER = "XWiki.CurrentUser";

    private static final String CACHE_PREFIX = CURRENT_USER.length() + ":" + CURRENT_USER + ":";

    private static final String URL_PREFIX = "http://localhost:8080";

    @MockComponent
    private ImageDownloader imageDownloader;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @MockComponent
    private URLSecurityManager urlSecurityManager;

    private Cache<String> cache;

    private Cache<DiffException> failureCache;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private XMLDiffDataURIConverterConfiguration configuration;

    @InjectMockComponents
    private DefaultDataURIConverter converter;

    @BeforeComponent
    public void configureCacheManager() throws CacheException
    {
        this.cache = mock();
        this.failureCache = mock();

        when(this.configuration.isEnabled()).thenReturn(true);

        when(this.cacheManager.createNewCache(any())).then(invocationOnMock -> {
            CacheConfiguration cacheConfiguration = invocationOnMock.getArgument(0);
            if ("diff.html.dataURI".equals(cacheConfiguration.getConfigurationId())) {
                return this.cache;
            } else if ("diff.html.dataURIFailureCache".equals(cacheConfiguration.getConfigurationId())) {
                return this.failureCache;
            }

            return null;
        });
    }

    @BeforeEach
    public void setUp()
    {
        when(this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn(CURRENT_USER);

        XWikiContext xwikiContext = mock();
        when(this.xwikiContextProvider.get()).thenReturn(xwikiContext);
        XWikiServletRequestStub request = new XWikiServletRequestStub.Builder()
            .setHeaders(Map.of("forwarded", List.of("host=localhost:8080;proto=http")))
            .build();
        request.setRequestURI("/xwiki");
        when(xwikiContext.getRequest()).thenReturn(request);
    }

    @Test
    void returnsURLWhenDisabled() throws DiffException
    {
        when(this.configuration.isEnabled()).thenReturn(false);
        String url = "http://www.example.com";
        assertEquals(url, this.converter.convert(url));
    }

    @Test
    void dataURIIsKept() throws DiffException
    {
        String dataURI = "data:image/png;base64,abc";
        assertEquals(dataURI, this.converter.convert(dataURI));
    }

    @Test
    void throwsExceptionWhenURLIsMalFormed() throws IOException
    {
        String url = "http://w w w.example.com";
        when(this.urlSecurityManager.isDomainTrusted(new URL(url))).thenReturn(true);
        DiffException exception = assertThrows(DiffException.class, () -> this.converter.convert(url));
        assertEquals(getFailureMessage(url), exception.getMessage());
        assertEquals("Illegal character in authority at index 7: http://w w w.example.com",
            exception.getCause().getMessage());

        verify(this.imageDownloader, never()).download(any());
        verify(this.cache, never()).set(any(), any());
        verify(this.failureCache).set(CACHE_PREFIX + url,
            exception);
    }

    @Test
    void usesCacheWhenAvailable() throws DiffException
    {
        String dataURI = "data:image/png;base64,def";
        String url = "/image.png";
        when(this.cache.get(CACHE_PREFIX + URL_PREFIX + url)).thenReturn(dataURI);

        assertEquals(dataURI, this.converter.convert(url));
    }

    @Test
    void throwsCachedFailure()
    {
        String url = "/image.png";
        DiffException exception = new DiffException("Failed to convert url to absolute URL.");
        when(this.failureCache.get(CACHE_PREFIX + URL_PREFIX + url)).thenReturn(exception);

        DiffException thrown = assertThrows(DiffException.class, () -> this.converter.convert(url));
        assertEquals(exception, thrown);
    }

    @Test
    void throwsWhenURLIsNotTrusted() throws MalformedURLException
    {
        String url = "http://example.com/image.png";
        when(this.urlSecurityManager.isDomainTrusted(new URL(url))).thenReturn(false);

        DiffException thrown = assertThrows(DiffException.class, () -> this.converter.convert(url));
        assertEquals(getFailureMessage(url), thrown.getMessage());
        assertEquals(String.format("The URL [%s] is not trusted.", url), thrown.getCause().getMessage());

        // Make sure that the failure is cached.
        verify(this.failureCache).set(CACHE_PREFIX + url, thrown);
    }

    @Test
    void throwsExceptionWhenImageDownloaderFails() throws URISyntaxException, IOException
    {
        String url = "/image.png";
        URI uri = new URI(URL_PREFIX + url);
        when(this.urlSecurityManager.isDomainTrusted(uri.toURL())).thenReturn(true);
        IOException exception = new IOException("Failed to download image.");
        when(this.imageDownloader.download(uri)).thenThrow(exception);

        DiffException thrown = assertThrows(DiffException.class, () -> this.converter.convert(url));
        assertEquals(getFailureMessage(url), thrown.getMessage());
        assertEquals(exception, thrown.getCause());

        // Make sure the failure is cached.
        verify(this.failureCache).set(CACHE_PREFIX + URL_PREFIX + url, thrown);
        // Make sure nothing is stored in the data cache.
        verify(this.cache, never()).set(any(), any());
    }

    @Test
    void returnsDataURIForReturnedDataAndMimeType() throws IOException, DiffException
    {
        String url = "/image.png";
        URI uri = URI.create(URL_PREFIX + url);
        when(this.urlSecurityManager.isDomainTrusted(uri.toURL())).thenReturn(true);
        String dataURI = "data:image/jpeg;base64,ZGVm";
        when(this.imageDownloader.download(uri))
            .thenReturn(new ImageDownloader.DownloadResult(new byte[] { 'd', 'e', 'f' }, "image/jpeg"));

        assertEquals(dataURI, this.converter.convert(url));

        // Make sure the data is cached.
        verify(this.cache).set(CACHE_PREFIX + URL_PREFIX + url, dataURI);
        // Make sure nothing is stored in the failure cache.
        verify(this.failureCache, never()).set(any(), any());
    }

    private static String getFailureMessage(String url)
    {
        return String.format("Failed to convert [%s] to data URI.", url);
    }
}
