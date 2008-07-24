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
 *
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
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

public class FeedPluginApi extends Api
{
    public static final String FEED_PLUGIN_EXCEPTION = "FeedPluginException";

        private FeedPlugin plugin;

        public FeedPluginApi(FeedPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public FeedPlugin getPlugin() {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    public void setPlugin(FeedPlugin plugin) {
        this.plugin = plugin;
    }

    public SyndFeed getFeeds(String sfeeds) throws IOException {
        return plugin.getFeeds(sfeeds, getXWikiContext());
    }

    public SyndFeed getFeeds(String sfeeds, boolean force) throws IOException {
        return plugin.getFeeds(sfeeds, force, getXWikiContext());
    }

    public SyndFeed getFeeds(String sfeeds, boolean ignoreInvalidFeeds, boolean force) throws IOException {
        return plugin.getFeeds(sfeeds, ignoreInvalidFeeds, force, getXWikiContext());
    }

    public SyndFeed getFeed(String sfeed) throws IOException {
        return plugin.getFeed(sfeed, false, getXWikiContext());
    }

    public SyndFeed getFeed(String sfeed, boolean force) throws IOException {
        return plugin.getFeed(sfeed, force, getXWikiContext());
    }

    public SyndFeed getFeed(String sfeed, boolean ignoreInvalidFeeds, boolean force) throws IOException {
        return plugin.getFeed(sfeed, ignoreInvalidFeeds, force, getXWikiContext());
    }

    public int updateFeeds() throws XWikiException {
        return updateFeeds("XWiki.FeedList");
    }

    public int updateFeeds(String feedDoc) throws XWikiException {
        return updateFeeds(feedDoc, true);
    }

    public int updateFeeds(String feedDoc, boolean fullContent) throws XWikiException {
        return updateFeeds(feedDoc, fullContent, true);
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry) throws XWikiException {
        if (hasProgrammingRights())
         return plugin.updateFeeds(feedDoc, fullContent, oneDocPerEntry, getXWikiContext());
        else
         return -1;
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry, boolean force) throws XWikiException {
        if (hasProgrammingRights())
         return plugin.updateFeeds(feedDoc, fullContent, oneDocPerEntry, force, getXWikiContext());
        else
         return -1;
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry, boolean force, String space) throws XWikiException {
        if (hasProgrammingRights())
         return plugin.updateFeeds(feedDoc, fullContent, oneDocPerEntry, force, space, getXWikiContext());
        else
         return -1;
    }

    public int updateFeedsInSpace(String space, boolean fullContent, boolean force) throws XWikiException {
        if (hasProgrammingRights())
         return plugin.updateFeedsInSpace(space, fullContent, true, force, getXWikiContext());
        else
         return -1;
    }

    public int updateFeed(String feedname, String feedurl) {
        if (hasProgrammingRights())
            return plugin.updateFeed(feedname, feedurl, false, true, getXWikiContext());
        else
            return -1;
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent) {
        if (hasProgrammingRights())
         return plugin.updateFeed(feedname, feedurl, fullContent, true, getXWikiContext());
        else
         return -1;
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent, boolean oneDocPerEntry) {
        if (hasProgrammingRights())
         return plugin.updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, getXWikiContext());
        else
         return -1;
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent, boolean oneDocPerEntry, boolean force) {
        if (hasProgrammingRights())
         return plugin.updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, force, getXWikiContext());
        else
         return -1;
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent, boolean oneDocPerEntry, boolean force, String space) {
        if (hasProgrammingRights())
         return plugin.updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, force, space, getXWikiContext());
        else
         return -1;
    }

    public boolean startUpdateFeedsInSpace(String space, int scheduleTimer)  throws XWikiException {
        return plugin.startUpdateFeedsInSpace(space, false, scheduleTimer, context);
    }

    public boolean startUpdateFeedsInSpace(String space, boolean fullContent, int scheduleTimer)  throws XWikiException {
        return plugin.startUpdateFeedsInSpace(space, fullContent, scheduleTimer, context);
    }

    public void stopUpdateFeedsInSpace(String space)  throws XWikiException {
        plugin.stopUpdateFeedsInSpace(space, context);
    }


    public UpdateThread getUpdateThread(String space) {
        return plugin.getUpdateThread(space, context);
    }

    public Collection getActiveUpdateThreads() {
        return plugin.getActiveUpdateThreads();
    }

    /**
     * Tries to instantiate a class implementing the {@link SyndEntrySource} interface using the given parameters
     * 
     * @param className the name of a class implementing {@link SyndEntrySource} interface
     * @param params constructor parameters
     * @return a new SyndEntrySource instance
     */
    public SyndEntrySourceApi getSyndEntrySource(String className, Map params)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            SyndEntrySource source = plugin.getSyndEntrySource(className, params, getXWikiContext());
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
    public SyndEntrySourceApi getSyndEntryDocumentSource(Map params)
    {
        return this.getSyndEntrySource(SyndEntryDocumentSource.class.getName());
    }

    /**
     * @see #getSyndEntryDocumentSource(Map)
     */
    public SyndEntrySourceApi getSyndEntryDocumentSource()
    {
        return getSyndEntryDocumentSource(Collections.EMPTY_MAP);
    }

    /**
     * Instantiates the default strategy for converting articles in feed entries.
     * 
     * @return a new {@link SyndEntrySourceApi} wrapping a {@link SyndEntryArticleSource} object
     */
    public SyndEntrySourceApi getSyndEntryArticleSource()
    {
        return getSyndEntryArticleSource(Collections.EMPTY_MAP);
    }

    /**
     * Instantiates the default strategy for converting articles in feed entries, allowing you to customize it through
     * parameters.
     * 
     * @return a new {@link SyndEntrySourceApi} wrapping a {@link SyndEntryArticleSource} object
     */
    public SyndEntrySourceApi getSyndEntryArticleSource(Map params)
    {
        Map defParams = new HashMap();
        defParams.put(SyndEntryDocumentSource.FIELD_TITLE, "XWiki.ArticleClass_title");
        defParams.put(SyndEntryDocumentSource.FIELD_DESCRIPTION, "XWiki.ArticleClass_content");
        defParams.put(SyndEntryDocumentSource.FIELD_CATEGORIES, "XWiki.ArticleClass_category");
        defParams.put(SyndEntryDocumentSource.CONTENT_LENGTH, new Integer(400));
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
        return plugin.getFeedEntry(getXWikiContext());
    }

    /**
     * Creates an empty feed image
     * 
     * @return a new feed image
     */
    public SyndImage getFeedImage()
    {
        return plugin.getFeedImage(context);
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
        return plugin.getFeed(getXWikiContext());
    }

    /**
     * Computes a new feed from a list of source items and a corresponding strategy for converting them in feed entries
     * 
     * @param list the list of source items
     * @param sourceApi the strategy to use for computing feed entries from source items
     * @param sourceParams strategy parameters
     * @return a new feed
     */
    public SyndFeed getFeed(List list, SyndEntrySourceApi sourceApi, Map sourceParams)
    {
        return getFeed(list, sourceApi, sourceParams, Collections.EMPTY_MAP);
    }

    /**
     * Creates a new feed from a list of documents, using the default strategy for converting documents in feed entries.
     * You can customize this strategy using strategy parameters.
     * 
     * @param list a list of {@link Document} objects, {@link XWikiDocument} objects or document names
     * @param params strategy parameters
     * @return a new feed
     * @see Document
     * @see #getFeed(List, SyndEntrySourceApi, Map)
     * @see SyndEntryDocumentSource
     */
    public SyndFeed getDocumentFeed(List list, Map params)
    {
        return getDocumentFeed(list, params, Collections.EMPTY_MAP);
    }

    /**
     * Creates a new feed from a list of articles, using the default strategy for converting articles in feed entries.
     * By articles we mean any document containing an <code>XWiki.ArticleClass</code> object.
     * 
     * @param list a list of articles
     * @return a new feed
     * @see Document
     * @see #getFeed(List, SyndEntrySourceApi, Map)
     * @see SyndEntryArticleSource
     */
    public SyndFeed getArticleFeed(List list)
    {
        return getArticleFeed(list, Collections.EMPTY_MAP);
    }

    /**
     * Instantiates the default document feed.
     * 
     * @param list a list of {@link Document} objects, {@link XWikiDocument} objects or document names
     * @return a new feed
     * @see #getDocumentFeed(List)
     */
    public SyndFeed getWebFeed(List list)
    {
        return getWebFeed(list, Collections.EMPTY_MAP);
    }

    /**
     * Instantiates the default article feed.
     * 
     * @param list a list of articles (as document instances or document names)
     * @return a new feed
     * @see #getArticleFeed(List)
     */
    public SyndFeed getBlogFeed(List list)
    {
        return getBlogFeed(list, Collections.EMPTY_MAP);
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
    public SyndFeed getFeed(String query, int count, int start, SyndEntrySourceApi sourceApi, Map sourceParams)
    {
        return getFeed(query, count, start, sourceApi, sourceParams, Collections.EMPTY_MAP);
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
     * @see Document
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map)
     * @see SyndEntryDocumentSource
     */
    public SyndFeed getDocumentFeed(String query, int count, int start, Map params)
    {
        return getDocumentFeed(query, count, start, params, Collections.EMPTY_MAP);
    }

    /**
     * Creates a new feed from the result of an HQL query, using the default strategy for converting articles in feed
     * entries. By articles we mean any document containing a <code>XWiki.ArticleClass</code> object.
     * 
     * @param query the HQL query used for retrieving the articles
     * @param count the maximum number of articles to retrieve
     * @param start the start index
     * @return a new feed
     * @see Document
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map)
     * @see SyndEntryArticleSource
     */
    public SyndFeed getArticleFeed(String query, int count, int start)
    {
        return getArticleFeed(query, count, start, Collections.EMPTY_MAP);
    }

    /**
     * Instantiates the default document feed.
     * 
     * @param query the HQL query used for retrieving the documents
     * @param count the maximum number of documents to retrieve
     * @param start the start index
     * @return a new feed
     * @see #getDocumentFeed(String, int, int)
     */
    public SyndFeed getWebFeed(String query, int count, int start)
    {
        return getWebFeed(query, count, start, Collections.EMPTY_MAP);
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
        return getBlogFeed(query, count, start, Collections.EMPTY_MAP);
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
    public SyndFeed getFeed(List list, SyndEntrySourceApi sourceApi, Map sourceParams, Map metadata)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            return plugin.getFeed(list, sourceApi.getSyndEntrySource(), sourceParams, metadata, getXWikiContext());
        } catch (XWikiException e) {
            getXWikiContext().put(FEED_PLUGIN_EXCEPTION, e);
            return null;
        }
    }

    /**
     * Creates a new feed from a list of documents, using the default strategy for converting documents in feed entries,
     * filling in the feed meta data. You can customize the default strategy by using strategy parameters.
     * 
     * @param list a list of {@link Document} objects, {@link XWikiDocument} objects or document names
     * @param params strategy parameters
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see Document
     * @see #getFeed(List, SyndEntrySourceApi, Map, Map)
     * @see SyndEntryDocumentSource
     */
    public SyndFeed getDocumentFeed(List list, Map params, Map metadata)
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
     * @see Document
     * @see #getFeed(List, SyndEntrySourceApi, Map, Map)
     * @see SyndEntryArticleSource
     */
    public SyndFeed getArticleFeed(List list, Map metadata)
    {
        return getFeed(list, getSyndEntryArticleSource(), Collections.EMPTY_MAP, metadata);
    }

    private static boolean keyHasValue(Map map, Object key, Object defaultValue)
    {
        Object value = map.get(key);
        return value != null && !value.equals(defaultValue);
    }

    /**
     * Fills the missing feed meta data fields with default values.
     */
    private Map fillDefaultFeedMetadata(Map metadata)
    {
        XWiki xwiki = getXWikiContext().getWiki();
        XWikiDocument doc = getXWikiContext().getDoc();
        if (metadata.get("author") == null) {
            metadata.put("author", xwiki.getUserName(doc.getAuthor(), null, false, getXWikiContext()));
        }
        if (!keyHasValue(metadata, "copyright", "")) {
            metadata.put("copyright", xwiki.getWebCopyright(getXWikiContext()));
        }
        if (!keyHasValue(metadata, "encoding", "")) {
            metadata.put("encoding", xwiki.getEncoding());
        }
        if (!keyHasValue(metadata, "url", "")) {
            metadata.put("url", "http://" + getXWikiContext().getRequest().getServerName());
        }
        if (!keyHasValue(metadata, "language", "")) {
            metadata.put("language", doc.getDefaultLanguage());
        }
        return metadata;
    }

    private Map fillWebFeedMetadata(Map metadata)
    {
        fillDefaultFeedMetadata(metadata);
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

    private Map fillBlogFeedMetadata(Map metadata)
    {
        fillDefaultFeedMetadata(metadata);
        // these strings should be taken from a resource bundle
        String title = "Personal Wiki Blog";
        String description = title;
        if (!keyHasValue(metadata, "title", "")) {
            metadata.put("title", title);
        }
        if (!keyHasValue(metadata, "description", "")) {
            metadata.put("description", description);
        }
        return metadata;
    }

    /**
     * Instantiates the default document feed.
     * 
     * @param list a list of {@link Document} objects, {@link XWikiDocument} objects or document names
     * @param metadata feed meta data (includes the author, description, copyright, encoding, url, title)
     * @return a new feed
     * @see #getDocumentFeed(List, Map)
     */
    public SyndFeed getWebFeed(List list, Map metadata)
    {
        SyndFeed webFeed = getDocumentFeed(list, Collections.EMPTY_MAP, fillWebFeedMetadata(metadata));
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
    public SyndFeed getBlogFeed(List list, Map metadata)
    {
        SyndFeed blogFeed = getArticleFeed(list, fillBlogFeedMetadata(metadata));
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
    public SyndFeed getFeed(String query, int count, int start, SyndEntrySourceApi sourceApi, Map sourceParams,
        Map metadata)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            return plugin.getFeed(query, count, start, sourceApi.getSyndEntrySource(), sourceParams, metadata,
                getXWikiContext());
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
     * @see Document
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map, Map)
     * @see SyndEntryDocumentSource
     */
    public SyndFeed getDocumentFeed(String query, int count, int start, Map params, Map metadata)
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
     * @see Document
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map, Map)
     * @see SyndEntryArticleSource
     */
    public SyndFeed getArticleFeed(String query, int count, int start, Map metadata)
    {
        return getFeed(query, count, start, getSyndEntryArticleSource(), Collections.EMPTY_MAP, metadata);
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
    public SyndFeed getWebFeed(String query, int count, int start, Map metadata)
    {
        if (query == null) {
            query = "where 1=1 order by doc.date desc";
        }
        SyndFeed webFeed = getDocumentFeed(query, count, start, Collections.EMPTY_MAP, fillWebFeedMetadata(metadata));
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
    public SyndFeed getBlogFeed(String query, int count, int start, Map metadata)
    {
        if (query == null) {
            XWikiRequest request = getXWikiContext().getRequest();
            String category = request.getParameter("category");
            if (category == null || category.equals("")) {
                query =
                    ", BaseObject as obj where obj.name=doc.fullName and obj.className='XWiki.ArticleClass' and obj.name<>'XWiki.ArticleClassTemplate' order by doc.creationDate desc";
            } else {
                query =
                    ", BaseObject as obj, DBStringListProperty as prop join prop.list list where obj.name=doc.fullName and obj.className='XWiki.ArticleClass' and obj.name<>'XWiki.ArticleClassTemplate' and obj.id=prop.id.id and prop.id.name='category' and list = '"
                        + category + "' order by doc.creationDate desc";
            }
        }
        SyndFeed blogFeed = getArticleFeed(query, count, start, fillBlogFeedMetadata(metadata));
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
        return plugin.getFeedOutput(feed, type, getXWikiContext());
    }

    /**
     * @see #getFeedOutput(SyndFeed, String)
     * @see #getFeed(List, SyndEntrySourceApi, Map, Map)
     */
    public String getFeedOutput(List list, SyndEntrySourceApi sourceApi, Map sourceParams, Map metadata, String type)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            return plugin.getFeedOutput(list, sourceApi.getSyndEntrySource(), sourceParams, metadata, type,
                getXWikiContext());
        } catch (XWikiException e) {
            getXWikiContext().put(FEED_PLUGIN_EXCEPTION, e);
            return null;
        }
    }

    /**
     * @see #getFeedOutput(List, SyndEntrySourceApi, Map, Map, String)
     * @see SyndEntryDocumentSource
     */
    public String getDocumentFeedOutput(List list, Map params, Map metadata, String type)
    {
        return getFeedOutput(list, getSyndEntryDocumentSource(), params, metadata, type);
    }

    /**
     * @see #getFeedOutput(List, SyndEntrySourceApi, Map, Map, String)
     * @see SyndEntryArticleSource
     */
    public String getArticleFeedOutput(List list, Map metadata, String type)
    {
        return getFeedOutput(list, getSyndEntryArticleSource(), Collections.EMPTY_MAP, metadata, type);
    }

    /**
     * @see #getWebFeed(List, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getWebFeedOutput(List list, Map metadata, String type)
    {
        return getFeedOutput(getWebFeed(list, metadata), type);
    }

    /**
     * @see #getBlogFeed(List, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getBlogFeedOutput(List list, Map metadata, String type)
    {
        return getFeedOutput(getBlogFeed(list, metadata), type);
    }

    /**
     * @see #getFeedOutput(SyndFeed, String)
     * @see #getFeed(String, int, int, SyndEntrySourceApi, Map, Map)
     */
    public String getFeedOutput(String query, int count, int start, SyndEntrySourceApi sourceApi, Map sourceParams,
        Map metadata, String type)
    {
        getXWikiContext().remove(FEED_PLUGIN_EXCEPTION);
        try {
            return plugin.getFeedOutput(query, count, start, sourceApi.getSyndEntrySource(), sourceParams, metadata,
                type, getXWikiContext());
        } catch (XWikiException e) {
            getXWikiContext().put(FEED_PLUGIN_EXCEPTION, e);
            return null;
        }
    }

    /**
     * @see #getFeedOutput(String, int, int, SyndEntrySourceApi, Map, Map, String)
     * @see SyndEntryDocumentSource
     */
    public String getDocumentFeedOutput(String query, int count, int start, Map params, Map metadata, String type)
    {
        return getFeedOutput(query, count, start, getSyndEntryDocumentSource(), params, metadata, type);
    }

    /**
     * @see #getFeedOutput(String, int, int, SyndEntrySourceApi, Map, Map, String)
     * @see SyndEntryArticleSource
     */
    public String getArticleFeedOutput(String query, int count, int start, Map metadata, String type)
    {
        return getFeedOutput(query, count, start, getSyndEntryArticleSource(), Collections.EMPTY_MAP, metadata, type);
    }

    /**
     * @see #getWebFeed(String, int, int, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getWebFeedOutput(String query, int count, int start, Map metadata, String type)
    {
        return getFeedOutput(getWebFeed(query, count, start, metadata), type);
    }

    /**
     * @see #getBlogFeed(String, int, int, Map)
     * @see #getFeedOutput(SyndFeed, String)
     */
    public String getBlogFeedOutput(String query, int count, int start, Map metadata, String type)
    {
        return getFeedOutput(getBlogFeed(query, count, start, metadata), type);
    }
}
