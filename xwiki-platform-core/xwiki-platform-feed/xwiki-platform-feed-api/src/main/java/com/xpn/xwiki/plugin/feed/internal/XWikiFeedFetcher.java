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
package com.xpn.xwiki.plugin.feed.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * Fetches feeds from URLs.
 *
 * @version $Id$
 */
public class XWikiFeedFetcher
{
    private String userAgent = "XWikiFeedFetcher/1.0";

    /**
     * Retrieve the feed from the given URL.
     *
     * @param feedUrl the URL of the feed
     * @return the feed
     * @throws IOException if an error occurs while fetching the feed
     * @throws FeedException if an error occurs while parsing the feed
     */
    public SyndFeed retrieveFeed(URL feedUrl) throws IOException, FeedException
    {
        return retrieveFeed(feedUrl, 0);
    }

    /**
     * @return the user agent string used when fetching feeds
     */
    public String getUserAgent()
    {
        return this.userAgent;
    }

    /**
     * @param userAgent the user agent string to use when fetching feeds
     */
    public void setUserAgent(String userAgent)
    {
        this.userAgent = userAgent;
    }

    /**
     * Retrieve the feed from the given URL.
     *
     * @param feedUrl the URL of the feed
     * @param timeout the timeout in seconds
     * @return the feed
     */
    public SyndFeed retrieveFeed(URL feedUrl, int timeout) throws IOException
    {
        if (feedUrl == null) {
            throw new IllegalArgumentException("null is not a valid URL");
        }

        try (CloseableHttpClient httpclient = HttpClients.createSystem()) {
            HttpGet httpGet = new HttpGet(feedUrl.toString());
            RequestConfig requestConfig =
                RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout, TimeUnit.SECONDS)
                    .setResponseTimeout(timeout, TimeUnit.SECONDS)
                    .build();

            httpGet.setConfig(requestConfig);
            httpGet.setHeader("User-Agent", getUserAgent());

            return httpclient.execute(httpGet, response -> {
                if (response.getCode() != 200) {
                    throw new IOException(
                        "Failed to fetch feed: %d %s".formatted(response.getCode(), response.getReasonPhrase()));
                }

                try (InputStream content = response.getEntity().getContent()) {
                    SyndFeedInput input = new SyndFeedInput();
                    return input.build(new XmlReader(content));
                } catch (FeedException e) {
                    throw new IOException("Failed to parse feed", e);
                }
            });
        }
    }
}
