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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.index.security.review.ReviewsMap;
import org.xwiki.extension.security.ExtensionSecurityConfiguration;
import org.xwiki.extension.security.analyzer.ReviewsFetcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

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

    @Override
    public Optional<ReviewsMap> fetch() throws ExtensionSecurityException
    {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(buildURI())
            .GET()
            .build();

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, ofString());
            ReviewsMap reviewsMap = new ObjectMapper().readValue(response.body(), ReviewsMap.class);
            return Optional.of(this.reviewMapFilter.filter(reviewsMap));
        } catch (IOException e) {
            throw new ExtensionSecurityException("Failed to fetch the reviews.", e);
        } catch (InterruptedException e) {
            this.logger.warn("Can't finish the reviews fetching as the thread was interrupted. Cause: [{}]",
                getRootCauseMessage(e));
            Thread.currentThread().interrupt();
            return Optional.empty();
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
