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
package org.xwiki.platform.notifications.test.po;

import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xwiki.test.ui.TestUtils;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rometools.rome.io.impl.Base64;

/**
 * Used to parse the notification RSS feed.
 *
 * @since 11.5RC1
 * @since 11.4
 * @since 11.3.1
 * @version $Id$
 */
public class NotificationsRSS
{
    private SyndFeed feed;

    private String url;
    private String user;
    private String password;

    /**
     * @param url URL of the RSS feed
     * @param user username to use to connect
     * @param password password of the user
     */
    public NotificationsRSS(String url, String user, String password)
    {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Allow to load the entries but transform first the URL to load them from the real host domain:
     * it might be different than the one from the retrieved URL since this class is not executed inside a docker
     * container, and the servletEngine might not be inside a container either.
     *
     * @param testUtils
     * @since 18.0.0RC1
     * @since 17.10.2
     */
    public void loadEntries(TestUtils testUtils)
    {
        String originalURL = this.url;

        try {
            this.url = testUtils.toHttpClientUri(this.url);
            HttpClient client = HttpClientBuilder.create().build();

            HttpGet request = new HttpGet(url);
            request.addHeader("Authorization", "Basic " + Base64.encode(user + ":" + password));

            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new AssertionError(String.format("Bad status code: [%d].",
                    response.getStatusLine().getStatusCode()));
            }

            SyndFeedInput input = new SyndFeedInput();
            feed = input.build(new XmlReader(response.getEntity().getContent()));
        } catch (Exception e) {
            throw new AssertionError(
                String.format("Error while loading and parsing the RSS feed from URL [%s]. " + "Original URL was [%s].",
                    url, originalURL),
                e);
        }
    }

    /**
     * @return the RSS entries
     */
    public List<SyndEntry> getEntries()
    {
        return feed.getEntries();
    }
}
