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
package com.xpn.xwiki.plugin.feed;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherEvent;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.AbstractFeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class XWikiFeedFetcher extends AbstractFeedFetcher
{
    private FeedFetcherCache feedInfoCache;

    private CredentialSupplier credentialSupplier;

    public XWikiFeedFetcher()
    {
        super();
    }

    /**
     * @param cache
     */
    public XWikiFeedFetcher(FeedFetcherCache cache)
    {
        this();
        setFeedInfoCache(cache);
    }

    public XWikiFeedFetcher(FeedFetcherCache cache, CredentialSupplier credentialSupplier)
    {
        this(cache);
        setCredentialSupplier(credentialSupplier);
    }

    /**
     * @return the feedInfoCache.
     */
    public synchronized FeedFetcherCache getFeedInfoCache()
    {
        return feedInfoCache;
    }

    /**
     * @param feedInfoCache the feedInfoCache to set
     */
    public synchronized void setFeedInfoCache(FeedFetcherCache feedInfoCache)
    {
        this.feedInfoCache = feedInfoCache;
    }

    /**
     * @return Returns the credentialSupplier.
     */
    public synchronized CredentialSupplier getCredentialSupplier()
    {
        return credentialSupplier;
    }

    /**
     * @param credentialSupplier The credentialSupplier to set.
     */
    public synchronized void setCredentialSupplier(CredentialSupplier credentialSupplier)
    {
        this.credentialSupplier = credentialSupplier;
    }

    @Override
    public SyndFeed retrieveFeed(URL feedUrl)
        throws IllegalArgumentException, IOException, FeedException, FetcherException
    {
        return retrieveFeed(feedUrl, 0);
    }

    /**
     * @see com.sun.syndication.fetcher.FeedFetcher#retrieveFeed(java.net.URL)
     */
    public SyndFeed retrieveFeed(URL feedUrl, int timeout)
        throws IllegalArgumentException, IOException, FeedException, FetcherException
    {
        if (feedUrl == null) {
            throw new IllegalArgumentException("null is not a valid URL");
        }
        HttpClient client = new HttpClient();
        if (timeout != 0) {
            client.getParams().setSoTimeout(timeout);
            client.getParams().setParameter("http.connection.timeout", new Integer(timeout));
        }

        System.setProperty("http.useragent", getUserAgent());
        client.getParams().setParameter("httpclient.useragent", getUserAgent());

        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        if ((proxyHost != null) && (!proxyHost.equals(""))) {
            int port = 3128;
            if ((proxyPort != null) && (!proxyPort.equals(""))) {
                port = Integer.parseInt(proxyPort);
            }
            client.getHostConfiguration().setProxy(proxyHost, port);
        }

        String proxyUser = System.getProperty("http.proxyUser");
        if ((proxyUser != null) && (!proxyUser.equals(""))) {
            String proxyPassword = System.getProperty("http.proxyPassword");
            Credentials defaultcreds = new UsernamePasswordCredentials(proxyUser, proxyPassword);
            client.getState().setProxyCredentials(AuthScope.ANY, defaultcreds);
        }

        String urlStr = feedUrl.toString();
        FeedFetcherCache cache = getFeedInfoCache();
        if (cache != null) {
            // retrieve feed
            HttpMethod method = new GetMethod(urlStr);
            method.addRequestHeader("Accept-Encoding", "gzip");
            try {
                if (isUsingDeltaEncoding()) {
                    method.setRequestHeader("A-IM", "feed");
                }

                // get the feed info from the cache
                // Note that syndFeedInfo will be null if it is not in the cache
                SyndFeedInfo syndFeedInfo = cache.getFeedInfo(feedUrl);
                if (syndFeedInfo != null) {
                    method.setRequestHeader("If-None-Match", syndFeedInfo.getETag());

                    if (syndFeedInfo.getLastModified() instanceof String) {
                        method.setRequestHeader("If-Modified-Since", (String) syndFeedInfo.getLastModified());
                    }
                }

                method.setFollowRedirects(true);

                int statusCode = client.executeMethod(method);
                fireEvent(FetcherEvent.EVENT_TYPE_FEED_POLLED, urlStr);
                handleErrorCodes(statusCode);

                SyndFeed feed = getFeed(syndFeedInfo, urlStr, method, statusCode);

                syndFeedInfo = buildSyndFeedInfo(feedUrl, urlStr, method, feed, statusCode);

                cache.setFeedInfo(new URL(urlStr), syndFeedInfo);

                // the feed may have been modified to pick up cached values
                // (eg - for delta encoding)
                feed = syndFeedInfo.getSyndFeed();

                return feed;
            } finally {
                method.releaseConnection();
            }
        } else {
            // cache is not in use
            HttpMethod method = new GetMethod(urlStr);
            try {
                method.setFollowRedirects(true);

                int statusCode = client.executeMethod(method);
                fireEvent(FetcherEvent.EVENT_TYPE_FEED_POLLED, urlStr);
                handleErrorCodes(statusCode);

                return getFeed(null, urlStr, method, statusCode);
            } finally {
                method.releaseConnection();
            }
        }
    }

    /**
     * @param feedUrl
     * @param urlStr
     * @param method
     * @param feed
     * @return
     * @throws MalformedURLException
     */
    private SyndFeedInfo buildSyndFeedInfo(URL feedUrl, String urlStr, HttpMethod method, SyndFeed feed, int statusCode)
        throws MalformedURLException
    {
        SyndFeedInfo syndFeedInfo;
        syndFeedInfo = new SyndFeedInfo();

        // this may be different to feedURL because of 3XX redirects
        syndFeedInfo.setUrl(new URL(urlStr));
        syndFeedInfo.setId(feedUrl.toString());

        Header imHeader = method.getResponseHeader("IM");
        if (imHeader != null && imHeader.getValue().indexOf("feed") >= 0 && isUsingDeltaEncoding()) {
            FeedFetcherCache cache = getFeedInfoCache();
            if (cache != null && statusCode == 226) {
                // client is setup to use http delta encoding and the server supports it and has returned a delta
                // encoded response.
                // This response only includes new items
                SyndFeedInfo cachedInfo = cache.getFeedInfo(feedUrl);
                if (cachedInfo != null) {
                    SyndFeed cachedFeed = cachedInfo.getSyndFeed();

                    // set the new feed to be the orginal feed plus the new items
                    feed = combineFeeds(cachedFeed, feed);
                }
            }
        }

        Header lastModifiedHeader = method.getResponseHeader("Last-Modified");
        if (lastModifiedHeader != null) {
            syndFeedInfo.setLastModified(lastModifiedHeader.getValue());
        }

        Header eTagHeader = method.getResponseHeader("ETag");
        if (eTagHeader != null) {
            syndFeedInfo.setETag(eTagHeader.getValue());
        }

        syndFeedInfo.setSyndFeed(feed);

        return syndFeedInfo;
    }

    /**
     * @param urlStr
     * @param method
     * @return
     * @throws IOException
     * @throws HttpException
     * @throws FetcherException
     * @throws FeedException
     */
    private static SyndFeed retrieveFeed(String urlStr, HttpMethod method)
        throws IOException, FetcherException, FeedException
    {

        InputStream stream = null;
        if ((method.getResponseHeader("Content-Encoding") != null) &&
            ("gzip".equalsIgnoreCase(method.getResponseHeader("Content-Encoding").getValue())))
        {
            stream = new GZIPInputStream(method.getResponseBodyAsStream());
        } else {
            stream = method.getResponseBodyAsStream();
        }
        try {
            XmlReader reader = null;
            if (method.getResponseHeader("Content-Type") != null) {
                reader = new XmlReader(stream, method.getResponseHeader("Content-Type").getValue(), true);
            } else {
                reader = new XmlReader(stream, true);
            }
            return new SyndFeedInput().build(reader);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private SyndFeed getFeed(SyndFeedInfo syndFeedInfo, String urlStr, HttpMethod method, int statusCode)
        throws IOException, FetcherException, FeedException
    {

        if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED && syndFeedInfo != null) {
            fireEvent(FetcherEvent.EVENT_TYPE_FEED_UNCHANGED, urlStr);
            return syndFeedInfo.getSyndFeed();
        }

        SyndFeed feed = retrieveFeed(urlStr, method);
        fireEvent(FetcherEvent.EVENT_TYPE_FEED_RETRIEVED, urlStr, feed);
        return feed;
    }

    public interface CredentialSupplier
    {
        public Credentials getCredentials(String realm, String host);
    }
}
