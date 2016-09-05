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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.web.XWikiRequest;

public class FeedPluginApi extends PluginApi<FeedPlugin>
{
    private static final String BLOG_POST_CLASS_NAME = "Blog.BlogPostClass";

    private static final String BLOG_POST_TEMPLATE_NAME = "Blog.BlogPostTemplate";

    private static final Map<String, Object> BLOG_FIELDS_MAPPING;

    public static final String FEED_PLUGIN_EXCEPTION = "FeedPluginException";

    static {
        BLOG_FIELDS_MAPPING = new HashMap<String, Object>();
        BLOG_FIELDS_MAPPING.put(SyndEntryDocumentSource.FIELD_TITLE, "Blog.BlogPostClass_title");
        BLOG_FIELDS_MAPPING.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, "Blog.BlogPostClass_content");
        BLOG_FIELDS_MAPPING.put(SyndEntryDocumentSource.FIELD_CATEGORIES, "Blog.BlogPostClass_category");
        BLOG_FIELDS_MAPPING.put(SyndEntryDocumentSource.FIELD_PUBLISHED_DATE, "Blog.BlogPostClass_publishDate");
        BLOG_FIELDS_MAPPING.put(SyndEntryDocumentSource.CONTENT_LENGTH, 400);
    }

    public FeedPluginApi(FeedPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Return the inner plugin object, if the user has the required programming rights.
     * 
     * @return The wrapped plugin object.
     * @deprecated Use {@link PluginApi#getInternalPlugin()}
     */
    @Deprecated
    public FeedPlugin getPlugin()
    {
        return super.getInternalPlugin();
    }

    public SyndFeed getFeeds(String sfeeds) throws IOException
    {
        return getProtectedPlugin().getFeeds(sfeeds, getXWikiContext());
    }

    public SyndFeed getFeeds(String sfeeds, boolean force) throws IOException
    {
        return getProtectedPlugin().getFeeds(sfeeds, force, getXWikiContext());
    }

    public SyndFeed getFeeds(String sfeeds, boolean ignoreInvalidFeeds, boolean force) throws IOException
    {
        return getProtectedPlugin().getFeeds(sfeeds, ignoreInvalidFeeds, force, getXWikiContext());
    }

    public SyndFeed getFeed(String sfeed) throws IOException
    {
        return getProtectedPlugin().getFeed(sfeed, false, getXWikiContext());
    }

    public SyndFeed getFeed(String sfeed, boolean force) throws IOException
    {
        return getProtectedPlugin().getFeed(sfeed, force, getXWikiContext());
    }

    public SyndFeed getFeed(String sfeed, boolean ignoreInvalidFeeds, boolean force) throws IOException
    {
        return getProtectedPlugin().getFeed(sfeed, ignoreInvalidFeeds, force, getXWikiContext());
    }

    public int updateFeeds() throws XWikiException
    {
        return updateFeeds("XWiki.FeedList");
    }

    public int updateFeeds(String feedDoc) throws XWikiException
    {
        return updateFeeds(feedDoc, true);
    }

    public int updateFeeds(String feedDoc, boolean fullContent) throws XWikiException
    {
        return updateFeeds(feedDoc, fullContent, true);
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeeds(feedDoc, fullContent, oneDocPerEntry, getXWikiContext());
        } else {
            return -1;
        }
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry, boolean force)
        throws XWikiException
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeeds(feedDoc, fullContent, oneDocPerEntry, force, getXWikiContext());
        } else {
            return -1;
        }
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry, boolean force, String space)
        throws XWikiException
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeeds(feedDoc, fullContent, oneDocPerEntry, force, space,
                getXWikiContext());
        } else {
            return -1;
        }
    }

    public int updateFeedsInSpace(String spaceReference, boolean fullContent, boolean force) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeedsInSpace(spaceReference, fullContent, true, force, getXWikiContext());
        } else {
            return -1;
        }
    }

    public int updateFeed(String feedname, String feedurl)
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeed(feedname, feedurl, false, true, getXWikiContext());
        } else {
            return -1;
        }
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent)
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeed(feedname, feedurl, fullContent, true, getXWikiContext());
        } else {
            return -1;
        }
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent, boolean oneDocPerEntry)
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, getXWikiContext());
        } else {
            return -1;
        }
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent, boolean oneDocPerEntry, boolean force)
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, force,
                getXWikiContext());
        } else {
            return -1;
        }
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent, boolean oneDocPerEntry, boolean force,
        String space)
    {
        if (hasProgrammingRights()) {
            return getProtectedPlugin().updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, force, space,
                getXWikiContext());
        } else {
            return -1;
        }
    }

    public boolean startUpdateFeedsInSpace(String space, int scheduleTimer) throws XWikiException
    {
        return getProtectedPlugin().startUpdateFeedsInSpace(space, false, scheduleTimer, this.context);
    }

    public boolean startUpdateFeedsInSpace(String space, boolean fullContent, int scheduleTimer) throws XWikiException
    {
        return getProtectedPlugin().startUpdateFeedsInSpace(space, fullContent, scheduleTimer, this.context);
    }

    public void stopUpdateFeedsInSpace(String space) throws XWikiException
    {
        getProtectedPlugin().stopUpdateFeedsInSpace(space, this.context);
    }

    public UpdateThread getUpdateThread(String space)
    {
        return getProtectedPlugin().getUpdateThread(space, this.context);
    }

    public Collection<String> getActiveUpdateThreads()
    {
        return getProtectedPlugin().getActiveUpdateThreads();
    }

    /**
     * Tries to instantiate a class implementing the {@link SyndEntrySource} interface using the given parameters
     * 
     * @param className the name of a class implementing {@link SyndEntrySource} interface
     * @param params constructor parameters
     * @return a new SyndEntrySource instance
     */
    public SyndEntrySourceApi getSyndEntrySource(String className, Map<String, Object> params)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            SyndEntrySource source = getProtectedPlugin().getSyndEntrySource(className, params, getXWikiContext());
            return new SyndEntrySourceApi(source, getXWikiContext());
        } catch (XWikiException e) {
            getXWikiContext().put(FEED_PLUGIN_EXCEPTION, e);
            return null;
        }
    }

    /**
     * @see #getSyndEntrySource(String, Map)
     */
    public SyndEntrySourceApi getSyndEntrySource(String className)
    {
        return this.getSyndEntrySource(className, null);
    }

    /**
     * Instantiates the default strategy for converting documents in feed entries.
     * 
     * @param params strategy parameters
     * @return a new {@link SyndEntrySourceApi} wrapping a {@link SyndEntryDocumentSource} object
     */
    public SyndEntrySourceApi getSyndEntryDocumentSource(Map<String, Object> params)
    {
        return this.getSyndEntrySource(SyndEntryDocumentSource.class.getName());
    }

    /**
     * @see #getSyndEntryDocumentSource(Map)
     */
    public SyndEntrySourceApi getSyndEntryDocumentSource()
    {
        Map<String, Object> params = Collections.emptyMap();
        return getSyndEntryDocumentSource(params);
    }

    /**
     * Instantiates the default strategy for converting articles in feed entries.
     * 
     * @return a new {@link SyndEntrySourceApi}
     */
    public SyndEntrySourceApi getSyndEntryArticleSource()
    {
        Map<String, Object> params = Collections.emptyMap();
        return getSyndEntryArticleSource(params);
    }

    /**
     * Instantiates the default strategy for converting articles in feed entries, allowing you to customize it through
     * parameters.
     * 
     * @return a new {@link SyndEntrySourceApi}
     */
    public SyndEntrySourceApi getSyndEntryArticleSource(Map<String, Object> params)
    {
        Map<String, Object> defParams = new HashMap<String, Object>();
        defParams.put(SyndEntryDocumentSource.FIELD_TITLE, "XWiki.ArticleClass_title");
        defParams.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, "XWiki.ArticleClass_content");
        defParams.put(SyndEntryDocumentSource.FIELD_CATEGORIES, "XWiki.ArticleClass_category");
        defParams.put(SyndEntryDocumentSource.CONTENT_LENGTH, 400);
        defParams.putAll(params);
        return this.getSyndEntrySource(SyndEntryDocumentSource.class.getName(), defParams);
    }

    /**
     * Creates an empty feed entry
     * 
     * @return a new feed entry
     */
    public SyndEntry getFeedEntry()
    {
        return getProtectedPlugin().getFeedEntry(getXWikiContext());
    }

    /**
     * Creates an empty feed image
     * 
     * @return a new feed image
     */
    public SyndImage getFeedImage()
    {
        return getProtectedPlugin().getFeedImage(this.context);
    }

    /**
     * Creates a new feed image having the given properties.
     * 
     * @param url image URL
     * @param title image title
     * @param description image description
     * @return a new feed image
     */
    public SyndImage getFeedImage(String url, String link, String title, String description)
    {
        SyndImage image = getFeedImage();
        image.setUrl(url);
        image.setLink(link);
        image.setTitle(title);
        image.setDescription(description);
        return image;
    }

    /**
     * Creates a new instance of the default feed image. The default image file name is taken from the <i>logo</i> skin
     * preference. If this preference is missing, <i>logo.png</i> is used instead.
     * 
     * @return a new feed image
     */
    public SyndImage getDefaultFeedImage()
    {
        // Currently, getSkinFile method returns relative (internal) URLs. I couldn't find a way to
        // get the external URL for a skin file. Is this something forbidden? I've noticed that we
        // actually compute the full URL but we strip it with urlFactory.getURL(url, context). So
        // what do you think of overloading the getSkinFile method by adding a absoluteURL flag?
        XWiki xwiki = getXWikiContext().getWiki();
        String fileName = xwiki.getSkinPreference("logo", "logo.png", getXWikiContext());
        String url = xwiki.getSkinFile(fileName, getXWikiContext());
        String port = "";
        XWikiRequest request = getXWikiContext().getRequest();
        if (("http".equals(request.getScheme()) && request.getServerPort() != 80)
            || ("https".equals(request.getScheme()) && request.getServerPort() != 443)) {
            port = ":" + request.getServerPort();
        }
        url = request.getScheme() + "://" + request.getServerName() + port + url;
        String link = "http://" + request.getServerName();
        return getFeedImage(url, link, "XWiki Logo", "XWiki Logo");
    }

    /**
     * Creates an empty feed
     * 
     * @return a new feed
     */
    public SyndFeed getFeed()
    {
        return getProtectedPlugin().getFeed(getXWikiContext());
    }

    /**
     * Computes a new feed from a list of source items and a corresponding strategy for converting them in feed entries
     * 
     * @param list the list of source items
     * @param sourceApi the strategy to use for computing feed entries from source items
     * @param sourceParams strategy parameters
     * @return a new feed
     */
    public SyndFeed getFeed(List<Object> list, SyndEntrySourceApi sourceApi, Map<String, Object> sourceParams)
    {
        Map<String, Object> metadata = Collections.emptyMap();
        return getFeed(list, sourceApi, sourceParams, metadata);
    }

    /**
     * Creates a new feed from a list of documents, using the default strategy for converting documents in feed entries.
     * You can customize this strategy using strategy parameters.
     * 
     * @param list a list of {@link com.xpn.xwiki.api.Document} objects, {@link com.xpn.xwiki.doc.XWikiDocument} objects or document names
     * @param params strategy parameters
     * @return a new feed
     * @see com.xpn.xwiki.api.Document
     * @see #getFeed(List, SyndEntrySourceApi, Map)
     * @see SyndEntryDocumentSource
     */
    public SyndFeed getDocumentFeed(List<Object> list, Map<String, Object> params)
    {
        Map<String, Object> metadata = Collections.emptyMap();
        return getDocumentFeed(list, params, metadata);
    }

    /**
     * Creates a new feed from a list of articles, using the default strategy for converting articles in feed entries.
     * By articles we mean any document containing an <code>XWiki.ArticleClass</code> object.
     * 
     * @param list a list of articles
     * @return a new feed
     * @see com.xpn.xwiki.api.Document
     * @see #getFeed(List, SyndEntrySourceApi, Map)
     * @see SyndEntrySourceApi
     */
    public SyndFeed getArticleFeed(List<Object> list)
    {
        Map<String, Object> params = Collections.emptyMap();
        return getArticleFeed(list, params);
    }

    /**
     * Instantiates the default document feed.
     * 
     * @param list a list of {@link com.xpn.xwiki.api.Document} objects, {@link com.xpn.xwiki.doc.XWikiDocument} objects or document names
     * @return a new feed
     * @see #getDocumentFeed(List, Map)
     */
    public SyndFeed getWebFeed(List<Object> list)
    {
        Map<String, Object> metadata = new HashMap<String, Object>();
        return getWebFeed(list, metadata);
    }

    /**
     * Instantiates the default article feed.
     * 
     * @param list a list of articles (as document instances or document names)
     * @return a new feed
     * @see #getArticleFeed(List)
     */
    public SyndFeed getBlogFeed(List<Object> list)
    {
        Map<String, Object> params = Collections.emptyMap();
        return getBlogFeed(list, params);
    }

    /**
     * Creates a new feed from the result of an HQL query and a corresponding strategy for converting the retrieved
     * documents in feed entries.
     * 
     * @param query the HQL query used for retrieving the documents
     * @param count the maximum number of documents to retrieve
     * @param start the start index
     * @param sourceApi the strategy to use for computing feed entries from source items
     * @param sourceParams strategy parameters
     * @return a new feed
     */
    public SyndFeed getFeed(String query, int count, int start, SyndEntrySourceApi sourceApi,
        Map<String, Object> sourceParams)
    {
        Map<String, Object> metadata = Collections.emptyMap();
        return getFeed(query, count, start, sourceApi, sourceParams, metadata);
    }

    /**
     * Creates a new feed from the result of an HQL query, using the default strategy for converting documents in feed
     * entries. You can customize this strategy using strategy parameters.
     * 
     * @param query the HQL query used for retrieving the documents
     * @param count the maximum number of documents to retrieve
     * @param start the start index
     * @param params strategy parameters
     * @return a new feed
     * @see com.xpn.xwiki.api.Document
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map)
     * @see SyndEntryDocumentSource
     */
    public SyndFeed getDocumentFeed(String query, int count, int start, Map<String, Object> params)
    {
        Map<String, Object> metadata = Collections.emptyMap();
        return getDocumentFeed(query, count, start, params, metadata);
    }

    /**
     * Creates a new feed from the result of an HQL query, using the default strategy for converting articles in feed
     * entries. By articles we mean any document containing a <code>XWiki.ArticleClass</code> object.
     * 
     * @param query the HQL query used for retrieving the articles
     * @param count the maximum number of articles to retrieve
     * @param start the start index
     * @return a new feed
     * @see com.xpn.xwiki.api.Document
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map)
     * @see SyndEntrySourceApi
     */
    public SyndFeed getArticleFeed(String query, int count, int start)
    {
        Map<String, Object> params = Collections.emptyMap();
        return getArticleFeed(query, count, start, params);
    }

    /**
     * Instantiates the default document feed.
     * 
     * @param query the HQL query used for retrieving the documents
     * @param count the maximum number of documents to retrieve
     * @param start the start index
     * @return a new feed
     * @see #getDocumentFeed(String, int, int, Map)
     */
    public SyndFeed getWebFeed(String query, int count, int start)
    {
        Map<String, Object> metadata = new HashMap<String, Object>();
        return getWebFeed(query, count, start, metadata);
    }

    /**
     * Instantiates the default article feed.
     * 
     * @param query the HQL query used for retrieving the articles
     * @param count the maximum number of articles to retrieve
     * @param start the start index
     * @return a new feed
     * @see #getArticleFeed(String, int, int)
     */
    public SyndFeed getBlogFeed(String query, int count, int start)
    {
        return getBlogFeed(query, count, start, Collections.<String, Object> emptyMap());
    }

    /**
     * Computes a new feed from a list of source items and a corresponding strategy for converting them in feed entries,
     * filling in the feed meta data.
     * 
     * @param list the list of source items
     * @param sourceApi the strategy to use for computing feed entries from source items
     * @param sourceParams strategy parameters
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     */
    public SyndFeed getFeed(List<Object> list, SyndEntrySourceApi sourceApi, Map<String, Object> sourceParams,
        Map<String, Object> metadata)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            return getProtectedPlugin().getFeed(list, sourceApi.getSyndEntrySource(), sourceParams, metadata,
                getXWikiContext());
        } catch (XWikiException e) {
            getXWikiContext().put(FEED_PLUGIN_EXCEPTION, e);
            return null;
        }
    }

    /**
     * Creates a new feed from a list of documents, using the default strategy for converting documents in feed entries,
     * filling in the feed meta data. You can customize the default strategy by using strategy parameters.
     * 
     * @param list a list of {@link com.xpn.xwiki.api.Document} objects, {@link com.xpn.xwiki.doc.XWikiDocument} objects or document names
     * @param params strategy parameters
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see com.xpn.xwiki.api.Document
     * @see #getFeed(List, SyndEntrySourceApi, Map, Map)
     * @see SyndEntryDocumentSource
     */
    public SyndFeed getDocumentFeed(List<Object> list, Map<String, Object> params, Map<String, Object> metadata)
    {
        return getFeed(list, getSyndEntryDocumentSource(), params, metadata);
    }

    /**
     * Creates a new feed from a list of articles, using the default strategy for converting articles in feed entries,
     * filling in the feed meta data. By articles we mean any document containing an <code>XWiki.ArticleClass</code>
     * object.
     * 
     * @param list a list of articles
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see com.xpn.xwiki.api.Document
     * @see #getFeed(List, SyndEntrySourceApi, Map, Map)
     * @see SyndEntrySourceApi
     */
    public SyndFeed getArticleFeed(List<Object> list, Map<String, Object> metadata)
    {
        Map<String, Object> params = Collections.emptyMap();
        return getFeed(list, getSyndEntryArticleSource(), params, metadata);
    }

    private static boolean keyHasValue(Map<String, Object> map, String key, Object defaultValue)
    {
        Object value = map.get(key);
        return value != null && !value.equals(defaultValue);
    }

    private Map<String, Object> fillWebFeedMetadata(Map<String, Object> metadata)
    {
        // these strings should be taken from a resource bundle
        String title = "Feed for document changes";
        String description = title;
        if (!keyHasValue(metadata, "title", "")) {
            metadata.put("title", title);
        }
        if (!keyHasValue(metadata, "description", "")) {
            metadata.put("description", description);
        }
        return metadata;
    }

    private Map<String, Object> fillBlogFeedMetadata(Map<String, Object> metadata)
    {
        // Make sure that we don't have an immutable Map
        Map<String, Object> result = new HashMap<String, Object>(metadata);

        // these strings should be taken from a resource bundle
        String title = "Personal Wiki Blog";
        String description = title;
        if (!keyHasValue(result, "title", "")) {
            result.put("title", title);
        }
        if (!keyHasValue(result, "description", "")) {
            result.put("description", description);
        }
        return result;
    }

    /**
     * Instantiates the default document feed.
     * 
     * @param list a list of {@link com.xpn.xwiki.api.Document} objects, {@link com.xpn.xwiki.doc.XWikiDocument} objects or document names
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see #getDocumentFeed(List, Map)
     */
    public SyndFeed getWebFeed(List<Object> list, Map<String, Object> metadata)
    {
        Map<String, Object> params = Collections.emptyMap();
        SyndFeed webFeed = getDocumentFeed(list, params, fillWebFeedMetadata(metadata));
        if (webFeed != null) {
            webFeed.setImage(getDefaultFeedImage());
        }
        return webFeed;
    }

    /**
     * Instantiates the default article feed.
     * 
     * @param list a list of articles (as document instances or document names)
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see #getArticleFeed(List, Map)
     */
    public SyndFeed getBlogFeed(List<Object> list, Map<String, Object> metadata)
    {
        Map<String, Object> params = Collections.emptyMap();
        SyndFeed blogFeed =
            getFeed(list, getSyndEntrySource(SyndEntryDocumentSource.class.getName(), BLOG_FIELDS_MAPPING), params,
                fillBlogFeedMetadata(metadata));
        if (blogFeed != null) {
            blogFeed.setImage(getDefaultFeedImage());
        }
        return blogFeed;
    }

    /**
     * Creates a new feed from the result of an HQL query and a corresponding strategy for converting the retrieved
     * documents in feed entries, filling in the feed meta data.
     * 
     * @param query the HQL query used for retrieving the documents
     * @param count the maximum number of documents to retrieve
     * @param start the start index
     * @param sourceApi the strategy to use for computing feed entries from source items
     * @param sourceParams strategy parameters
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     */
    public SyndFeed getFeed(String query, int count, int start, SyndEntrySourceApi sourceApi,
        Map<String, Object> sourceParams, Map<String, Object> metadata)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            return getProtectedPlugin().getFeed(query, count, start, sourceApi.getSyndEntrySource(), sourceParams,
                metadata, getXWikiContext());
        } catch (XWikiException e) {
            getXWikiContext().put(FEED_PLUGIN_EXCEPTION, e);
            return null;
        }
    }

    /**
     * Creates a new feed from the result of an HQL query, using the default strategy for converting documents in feed
     * entries, filling in the feed meta data. You can customize the default strategy by using strategy parameters.
     * 
     * @param query the HQL query used for retrieving the documents
     * @param count the maximum number of documents to retrieve
     * @param start the start index
     * @param params strategy parameters
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see com.xpn.xwiki.api.Document
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map, Map)
     * @see SyndEntryDocumentSource
     */
    public SyndFeed getDocumentFeed(String query, int count, int start, Map<String, Object> params,
        Map<String, Object> metadata)
    {
        return getFeed(query, count, start, getSyndEntryDocumentSource(), params, metadata);
    }

    /**
     * Creates a new feed from the result of an HQL query, using the default strategy for converting articles in feed
     * entries, filling in the feed meta data. By articles we mean any document containing a
     * <code>XWiki.ArticleClass</code> object.
     * 
     * @param query the HQL query used for retrieving the articles
     * @param count the maximum number of articles to retrieve
     * @param start the start index
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see com.xpn.xwiki.api.Document
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map, Map)
     * @see SyndEntrySourceApi
     */
    public SyndFeed getArticleFeed(String query, int count, int start, Map<String, Object> metadata)
    {
        Map<String, Object> params = Collections.emptyMap();
        return getFeed(query, count, start, getSyndEntryArticleSource(), params, metadata);
    }

    /**
     * Instantiates the default document feed.
     * 
     * @param query the HQL query used for retrieving the documents
     * @param count the maximum number of documents to retrieve
     * @param start the start index
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see #getDocumentFeed(String, int, int, Map)
     */
    public SyndFeed getWebFeed(String query, int count, int start, Map<String, Object> metadata)
    {
        if (query == null) {
            query = "where 1=1 order by doc.date desc";
        }
        Map<String, Object> params = new HashMap<String, Object>();
        SyndFeed webFeed = getDocumentFeed(query, count, start, params, fillWebFeedMetadata(metadata));
        if (webFeed != null) {
            webFeed.setImage(getDefaultFeedImage());
        }
        return webFeed;
    }

    /**
     * Instantiates the default article feed.
     * 
     * @param query the HQL query used for retrieving the articles
     * @param count the maximum number of articles to retrieve
     * @param start the start index
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see #getArticleFeed(String, int, int, Map)
     */
    public SyndFeed getBlogFeed(String query, int count, int start, Map<String, Object> metadata)
    {
        return getBlogFeed(query, count, start, null, metadata);
    }

    /**
     * Instantiates the default article feed.
     * 
     * @param query the HQL query used for retrieving the articles
     * @param count the maximum number of articles to retrieve
     * @param start the start index
     * @param blogPostClassName The name of the Blog Class the data are retrieved from. If null the Default Blog
     *            Application is used.
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see #getArticleFeed(String, int, int, Map)
     */
    public SyndFeed getBlogFeed(String query, int count, int start, String blogPostClassName,
        Map<String, Object> metadata)
    {
        if (query == null) {
            XWikiRequest request = getXWikiContext().getRequest();
            String category = request.getParameter("category");
            if (category == null || category.equals("")) {
                query =
                    ", BaseObject as obj where obj.name=doc.fullName and obj.className='" + BLOG_POST_CLASS_NAME
                        + "' and obj.name<>'" + BLOG_POST_TEMPLATE_NAME + "' order by doc.creationDate desc";
            } else {
                query =
                    ", BaseObject as obj, DBStringListProperty as prop join prop.list list where obj.name=doc.fullName and obj.className='"
                        + BLOG_POST_CLASS_NAME + "' and obj.name<>'" + BLOG_POST_TEMPLATE_NAME
                        + "' and obj.id=prop.id.id and prop.id.name='category' and list = '" + category
                        + "' order by doc.creationDate desc";
            }
        }
        Map<String, Object> params = Collections.emptyMap();
        Map<String, Object> blogMappings = null;
        if (blogPostClassName == null) {
            blogMappings = BLOG_FIELDS_MAPPING;
        } else {
            blogMappings = new HashMap<String, Object>();
            blogMappings.put(SyndEntryDocumentSource.FIELD_TITLE, blogPostClassName + "_title");
            blogMappings.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, blogPostClassName + "_content");
            blogMappings.put(SyndEntryDocumentSource.FIELD_CATEGORIES, blogPostClassName + "_category");
            blogMappings.put(SyndEntryDocumentSource.FIELD_PUBLISHED_DATE, blogPostClassName + "_publishDate");
            blogMappings.put(SyndEntryDocumentSource.CONTENT_LENGTH, new Integer(400));
        }
        SyndFeed blogFeed =
            getFeed(query, count, start, getSyndEntrySource(SyndEntryDocumentSource.class.getName(), blogMappings),
                params, fillBlogFeedMetadata(metadata));
        if (blogFeed != null) {
            blogFeed.setImage(getDefaultFeedImage());
        }
        return blogFeed;
    }

    /**
     * Converts a feed into its string representation using the specified syntax
     * 
     * @param feed any type of feed, implementing the {@link SyndFeed} interface
     * @param type the feed type (syntax) to use, <b>null</b> if none. It can be any version of RSS or Atom. Some
     *            possible values are "rss_1.0", "rss_2.0" and "atom_1.0"
     * @return the string representation of the given feed using the syntax associated with the specified feed type
     */
    public String getFeedOutput(SyndFeed feed, String type)
    {
        return getProtectedPlugin().getFeedOutput(feed, type, getXWikiContext());
    }

    /**
     * @see #getFeedOutput(SyndFeed, String)
     * @see #getFeed(List, SyndEntrySourceApi, Map, Map)
     */
    public String getFeedOutput(List<Object> list, SyndEntrySourceApi sourceApi, Map<String, Object> sourceParams,
        Map<String, Object> metadata, String type)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            return getProtectedPlugin().getFeedOutput(list, sourceApi.getSyndEntrySource(), sourceParams, metadata,
                type, getXWikiContext());
        } catch (XWikiException e) {
            getXWikiContext().put(FEED_PLUGIN_EXCEPTION, e);
            return null;
        }
    }

    /**
     * @see #getFeedOutput(List, SyndEntrySourceApi, Map, Map, String)
     * @see SyndEntryDocumentSource
     */
    public String getDocumentFeedOutput(List<Object> list, Map<String, Object> params, Map<String, Object> metadata,
        String type)
    {
        return getFeedOutput(list, getSyndEntryDocumentSource(), params, metadata, type);
    }

    /**
     * @see #getFeedOutput(List, SyndEntrySourceApi, Map, Map, String)
     * @see SyndEntrySourceApi
     */
    public String getArticleFeedOutput(List<Object> list, Map<String, Object> metadata, String type)
    {
        Map<String, Object> params = Collections.emptyMap();
        return getFeedOutput(list, getSyndEntryArticleSource(), params, metadata, type);
    }

    /**
     * @see #getWebFeed(List, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getWebFeedOutput(List<Object> list, Map<String, Object> metadata, String type)
    {
        return getFeedOutput(getWebFeed(list, metadata), type);
    }

    /**
     * @see #getBlogFeed(List, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getBlogFeedOutput(List<Object> list, Map<String, Object> metadata, String type)
    {
        return getFeedOutput(getBlogFeed(list, metadata), type);
    }

    /**
     * @see #getFeedOutput(SyndFeed, String)
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map, Map)
     */
    public String getFeedOutput(String query, int count, int start, SyndEntrySourceApi sourceApi,
        Map<String, Object> sourceParams, Map<String, Object> metadata, String type)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            return getProtectedPlugin().getFeedOutput(query, count, start, sourceApi.getSyndEntrySource(),
                sourceParams, metadata, type, getXWikiContext());
        } catch (XWikiException e) {
            getXWikiContext().put(FEED_PLUGIN_EXCEPTION, e);
            return null;
        }
    }

    /**
     * @see #getFeedOutput(String, int, int, SyndEntrySourceApi, Map, Map, String)
     * @see SyndEntryDocumentSource
     */
    public String getDocumentFeedOutput(String query, int count, int start, Map<String, Object> params,
        Map<String, Object> metadata, String type)
    {
        return getFeedOutput(query, count, start, getSyndEntryDocumentSource(), params, metadata, type);
    }

    /**
     * @see #getFeedOutput(String, int, int, SyndEntrySourceApi, Map, Map, String)
     * @see SyndEntrySourceApi
     */
    public String getArticleFeedOutput(String query, int count, int start, Map<String, Object> metadata, String type)
    {
        Map<String, Object> params = Collections.emptyMap();
        return getFeedOutput(query, count, start, getSyndEntryArticleSource(), params, metadata, type);
    }

    /**
     * @see #getWebFeed(String, int, int, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getWebFeedOutput(String query, int count, int start, Map<String, Object> metadata, String type)
    {
        return getFeedOutput(getWebFeed(query, count, start, metadata), type);
    }

    /**
     * @see #getBlogFeed(String, int, int, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getBlogFeedOutput(String query, int count, int start, Map<String, Object> metadata, String type)
    {
        return getFeedOutput(getBlogFeed(query, count, start, metadata), type);
    }

    /**
     * @see #getBlogFeed(String, int, int, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getBlogFeedOutput(String query, int count, int start, String blogPostClassName,
        Map<String, Object> metadata, String type)
    {
        SyndFeed feed = getBlogFeed(query, count, start, blogPostClassName, metadata);
        String ret = getFeedOutput(feed, type);
        return ret;
    }
}
