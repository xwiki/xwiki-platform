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

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import com.sun.syndication.io.impl.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.io.IOUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Used to parse the notification RSS feed.
 *
 * @since 11.5RC1
 * @version $Id$
 */
public class NotificationsRSS
{
    private SyndFeed feed;

    /**
     * @param user username to use to connect
     * @param password password of the user
     * @throws Exception if an error happens while fetching and parsing RSS
     */
    public NotificationsRSS(String user, String password) throws Exception
    {
        HttpClient client = HttpClientBuilder.create().build();

        HttpGet request = new HttpGet("http://localhost:8080/xwiki/bin/get/XWiki/Notifications/Code/"
                + "NotificationRSSService?outputSyntax=plain");
        request.addHeader("Authorization", "Basic " + Base64.encode(user + ":" + password));

        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception(String.format("Bad status code: [%d].", response.getStatusLine().getStatusCode()));
        }

        String content = IOUtils.toString(response.getEntity().getContent());


        System.out.println("============== CONTENT ===============");
        System.out.println(content);

        SyndFeedInput input = new SyndFeedInput();
        feed = input.build(new XmlReader(new ByteArrayInputStream(content.getBytes())));
    }

    /**
     * @return the RSS entries
     */
    public List<SyndEntry> getEntries()
    {
        return feed.getEntries();
    }
}
