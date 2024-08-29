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
import java.net.URI;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.xwiki.component.annotation.Component;
import org.xwiki.diff.xml.XMLDiffDataURIConverterConfiguration;
import org.xwiki.security.authentication.AuthenticationConfiguration;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Component for downloading images from a URL with the given cookies.
 *
 * @since 14.10.15
 * @since 15.5.1
 * @since 15.6
 * @version $Id$
 */
@Component(roles = ImageDownloader.class)
@Singleton
public class ImageDownloader
{
    private static final String COOKIE_DOMAIN_PREFIX = ".";

    private static final String HEADER_COOKIE = "Cookie";

    @Inject
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private XMLDiffDataURIConverterConfiguration configuration;

    @Inject
    private AuthenticationConfiguration authenticationConfiguration;

    /**
     * The result of a download request.
     */
    public static class DownloadResult
    {
        private final byte[] data;

        private final String contentType;

        /**
         * @param data the downloaded data
         * @param contentType the MIME type of the downloaded data
         */
        public DownloadResult(byte[] data, String contentType)
        {
            this.data = data;
            this.contentType = contentType;
        }

        /**
         * @return the downloaded data
         */
        public byte[] getData()
        {
            return this.data;
        }

        /**
         * @return the MIME type of the downloaded data
         */
        public String getContentType()
        {
            return this.contentType;
        }
    }

    /**
     * Download the image from the given URL with the cookies from the current request.
     *
     * @param uri the URL of the image
     * @return the image as a byte array
     * @throws IOException if there was an error downloading the image
     */
    public DownloadResult download(URI uri) throws IOException
    {
        HttpClientBuilder httpClientBuilder = this.httpClientBuilderFactory.create();

        HttpGet getMethod = initializeGetMethod(uri);

        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            return httpClient.execute(getMethod, response -> handleResponse(uri, response));
        }
    }

    private DownloadResult handleResponse(URI uri, ClassicHttpResponse response) throws IOException
    {
        if (response.getCode() == HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();
            // Remove the content type parameters, such as the charset, so they don't influence the diff.
            String contentType = entity.getContentType();
            contentType = StringUtils.substringBefore(contentType, ";");

            if (!StringUtils.startsWith(contentType, "image/")) {
                throw new IOException(String.format("The content of [%s] is not an image.", uri));
            }

            long maximumSize = this.configuration.getMaximumContentSize();
            if (maximumSize > 0 && entity.getContentLength() > maximumSize) {
                throw new IOException(String.format("The content length of [%s] is too big.", uri));
            }

            byte[] content;
            if (maximumSize > 0) {
                // The content length is not always available (then it is negative), so we need to use a bounded
                // input stream to make sure we don't read more than the maximum size.
                try (BoundedInputStream boundedInputStream = new BoundedInputStream(entity.getContent(),
                    maximumSize))
                {
                    content = IOUtils.toByteArray(boundedInputStream);
                }

                if (content.length == maximumSize) {
                    throw new IOException(String.format("The content of [%s] is too big.", uri));
                }
            } else {
                content = IOUtils.toByteArray(entity.getContent());
            }

            return new DownloadResult(content, contentType);
        } else {
            throw new IOException(response.getCode() + " " + response.getReasonPhrase());
        }
    }

    private HttpGet initializeGetMethod(URI uri)
    {
        HttpGet getMethod = new HttpGet(uri);

        XWikiRequest request = this.xcontextProvider.get().getRequest();
        if (request != null && matchesCookieDomain(uri.getHost(), request)) {
            // Copy the cookie header from the current request.
            getMethod.setHeader(HEADER_COOKIE, request.getHeader(HEADER_COOKIE));
        }

        return getMethod;
    }

    /**
     * @return if the host matches the cookie domain of the current request
     */
    private boolean matchesCookieDomain(String host, HttpServletRequest request)
    {
        String serverName = request.getServerName();
        // Add a leading dot to avoid matching domains that are longer versions of the cookie domain and to ensure
        // that the cookie domain itself is matched as the cookie domain also contains the leading dot. Always add
        // the dot as two dots will still match.
        String prefixedServerName = COOKIE_DOMAIN_PREFIX + serverName;

        Optional<String> cookieDomain =
            this.authenticationConfiguration.getCookieDomains().stream()
                .filter(prefixedServerName::endsWith)
                .findFirst();

        // If there is a cookie domain, check if the host also matches it.
        return cookieDomain.map((COOKIE_DOMAIN_PREFIX + host)::endsWith)
            // If no cookie domain is configured, check for an exact match with the server name as no domain is sent in
            // this case and thus the cookie isn't valid for subdomains.
            .orElseGet(() -> host.equals(serverName));
    }

}
