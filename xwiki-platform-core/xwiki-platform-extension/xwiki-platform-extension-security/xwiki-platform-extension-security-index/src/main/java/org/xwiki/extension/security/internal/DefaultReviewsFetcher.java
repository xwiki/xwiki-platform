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
package org.xwiki.extension.security.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.index.security.review.ReviewsMap;
import org.xwiki.extension.repository.http.internal.HttpClientFactory;
import org.xwiki.extension.security.ExtensionSecurityConfiguration;
import org.xwiki.extension.security.analyzer.ReviewsFetcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.apache.http.HttpStatus.SC_OK;

/**
 * Fetches the security vulnerabilities reviews from a remote source providing a json that can be parsed to a
 * {@link ReviewsMap}.
 *
 * @version $Id$
 * @since 15.6RC1
 */
@Component
@Singleton
public class DefaultReviewsFetcher implements ReviewsFetcher
{
    @Inject
    private ExtensionSecurityConfiguration extensionSecurityConfiguration;

    @Inject
    private ReviewMapFilter reviewMapFilter;

    @Inject
    private Logger logger;

    // TODO: replace with a more generic http client factory.
    @Inject
    private HttpClientFactory httpClientFactory;

    @Override
    public ReviewsMap fetch() throws ExtensionSecurityException
    {
        try (CloseableHttpClient httpClient = this.httpClientFactory.createHttpClientBuilder(Map.of()).build()) {
            HttpGet getMethod = new HttpGet(buildURI());
            CloseableHttpResponse execute = httpClient.execute(getMethod);
            StatusLine statusLine = execute.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == SC_OK) {
                HttpEntity entity = execute.getEntity();
                ReviewsMap reviewsMap = new ObjectMapper().readValue(entity.getContent(), ReviewsMap.class);
                return this.reviewMapFilter.filter(reviewsMap);
            } else {
                throw new ExtensionSecurityException(String.format(
                    "Review fetching failed with http code [%d] and message [%s].", statusCode,
                    statusLine.getReasonPhrase()));
            }
        } catch (IOException e) {
            throw new ExtensionSecurityException("Failed to fetch the reviews.", e);
        }
    }

    private URI buildURI() throws ExtensionSecurityException
    {
        try {
            return UriBuilder.fromUri(URI.create(this.extensionSecurityConfiguration.getReviewsURL()))
                .queryParam("version", "1")
                .build();
        } catch (IllegalArgumentException | UriBuilderException e) {
            throw new ExtensionSecurityException("Unable to build the review map source URI.", e);
        }
    }
}
