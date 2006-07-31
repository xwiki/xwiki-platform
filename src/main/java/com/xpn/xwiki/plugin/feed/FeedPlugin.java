/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author ludovic
 * @author jeremi
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.feed;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class FeedPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
        private static Log mLogger =
                LogFactory.getFactory().getInstance(com.xpn.xwiki.plugin.feed.FeedPlugin.class);

        private XWikiCache feedCache;
        private int refreshPeriod;

      public static class SyndEntryComparator implements Comparator {

          public int compare(Object element1, Object element2) {
            SyndEntry entry1 = (SyndEntry) element1;
            SyndEntry entry2 = (SyndEntry) element2;

            if ((entry1.getPublishedDate() == null) &&  (entry2.getPublishedDate() == null))
                return 0;
            if (entry1.getPublishedDate() == null)
                return 1;
            if (entry2.getPublishedDate() == null)
                return -1;
            return (-entry1.getPublishedDate().compareTo(entry2.getPublishedDate()));
        }
    }


      public static class EntriesComparator implements Comparator {

          public int compare(Object element1, Object element2) {
            BaseObject entry1 = ((com.xpn.xwiki.api.Object) element1).getXWikiObject();
            BaseObject entry2 = ((com.xpn.xwiki.api.Object) element2).getXWikiObject();

            if ((entry1.getDateValue("date") == null) &&  (entry2.getDateValue("date") == null))
                return 0;
            if (entry1.getDateValue("date") == null)
                return 1;
            if (entry2.getDateValue("date") == null)
                return -1;
            return (-entry1.getDateValue("date").compareTo(entry2.getDateValue("date")));
        }
    }

    public FeedPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
        init(context);
    }

    public String getName() {
        return "feed";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new FeedPluginApi((FeedPlugin) plugin, context);
    }

    public void flushCache() {
        if (feedCache!=null)
            feedCache.flushAll();
        feedCache = null;
    }

    public void init(XWikiContext context) {
        super.init(context);
        prepareCache(context);
        refreshPeriod = (int) context.getWiki().ParamAsLong("xwiki.plugins.feed.cacherefresh", 3600);
    }


    public void initCache(XWikiContext context) throws XWikiException {
        int iCapacity = 100;
        try {
            String capacity = context.getWiki().Param("xwiki.plugins.feed.cache.capacity");
            if (capacity != null)
                iCapacity = Integer.parseInt(capacity);
        } catch (Exception e) {}

        initCache(iCapacity, context);
    }

    public void initCache(int iCapacity, XWikiContext context) throws XWikiException {
            feedCache = context.getWiki().getCacheService().newLocalCache(iCapacity);
    }

    protected void prepareCache(XWikiContext context) {
        try {
            if (feedCache==null)
                initCache(context);
        } catch (XWikiException e) {
        }
    }

    public SyndFeed getFeeds(String sfeeds, XWikiContext context) throws IOException {
            return getFeeds(sfeeds, false, true, context);
    }

    public SyndFeed getFeeds(String sfeeds, boolean force, XWikiContext context) throws IOException {
            return getFeeds(sfeeds, false, force, context);
    }

    public SyndFeed getFeeds(String sfeeds, boolean ignoreInvalidFeeds, boolean force, XWikiContext context) throws IOException {
        String[] feeds;
        if (sfeeds.indexOf("\n") != -1)
            feeds = sfeeds.split("\n");
        else
            feeds = sfeeds.split("\\|");
        List entries = new ArrayList();
        SyndFeed outputFeed = new SyndFeedImpl();
        if (context.getDoc() != null)
        {
            outputFeed.setTitle(context.getDoc().getFullName());
            try {
                outputFeed.setUri(context.getWiki().getURL(context.getDoc().getFullName(), "view", context));
            } catch (XWikiException e) {
                e.printStackTrace();
            }
            outputFeed.setAuthor(context.getDoc().getAuthor());
        }
        else
        {
            outputFeed.setTitle("XWiki Feeds");
            outputFeed.setAuthor("XWiki Team");
        }
        outputFeed.setEntries(entries);
        for (int i = 0; i < feeds.length; i++)
        {
            SyndFeed feed = getFeed(feeds[i], ignoreInvalidFeeds, force, context);
            if (feed != null)
                entries.addAll(feed.getEntries());
        }
        SyndEntryComparator comp = new SyndEntryComparator();
        Collections.sort(entries, comp);
        return outputFeed;
    }


    public SyndFeed getFeed(String sfeed, XWikiContext context) throws IOException {
        return getFeed(sfeed, true, false, context);
    }

    public SyndFeed getFeed(String sfeed, boolean force, XWikiContext context) throws IOException {
        return getFeed(sfeed, true, force, context);
    }

    public SyndFeed getFeed(String sfeed, boolean ignoreInvalidFeeds, boolean force, XWikiContext context) throws IOException {
        SyndFeed feed = null;
        prepareCache(context);
        if (!force) {
            try {
                feed = (SyndFeed) feedCache.getFromCache(sfeed, refreshPeriod);

            } catch (XWikiCacheNeedsRefreshException e) {
                feedCache.cancelUpdate(sfeed);
            } catch (Exception e) {
            }
        }

        if (feed==null)
         feed = getFeedForce(sfeed, ignoreInvalidFeeds, context);

        if (feed!=null)
         feedCache.putInCache(sfeed, feed);

        return feed;
    }

    public SyndFeed getFeedForce(String sfeed, boolean ignoreInvalidFeeds, XWikiContext context) throws IOException {
            try {
                //SyndFeedInput input = new SyndFeedInput();
                //SyndFeed feed = null;
                URL feedURL = new URL(sfeed);

                FeedFetcher feedFetcher = new HttpURLFeedFetcher();
                feedFetcher.setUserAgent(context.getWiki().Param("xwiki.plugins.feed.useragent", "XWikiBot"));
                SyndFeed feed = feedFetcher.retrieveFeed(feedURL);

                //feed = input.build(new XmlReader(feedURL));
                return feed;
            }
            catch (Exception ex) {
                if (ignoreInvalidFeeds) {
                    Map map = (Map) context.get("invalidFeeds");
                    if (map==null) {
                        map = new HashMap();
                        context.put("invalidFeeds", map);
                    }
                    map.put(sfeed, ex);
                    return null;
                }

                throw new java.io.IOException("Error processing " + sfeed + ": " + ex.getMessage());
            }
        }

    public void updateFeeds(XWikiContext context) throws XWikiException {
        updateFeeds("XWiki.FeedList", context);
    }

    public void updateFeeds(String feedDoc, XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getWiki().getDocument(feedDoc, context);
        Vector objs = doc.getObjects("XWiki.AggregatorURLClass");
        Iterator it = objs.iterator();
        SyndFeed feed = null;
        while(it.hasNext())
        {
            try {
                BaseObject obj = (BaseObject) it.next();
                String url = obj.getStringValue("url");
                String guid = obj.getStringValue("guid");
                try {
                    feed = getFeedForce(url, true, context);
                } catch (IOException e) {}
                if (feed != null)
                    saveFeed(guid, feed, context);
            }
            catch(Exception e)
            {
                Map map = (Map) context.get("updateFeedError");
                if (map==null) {
                    map = new HashMap();
                    context.put("updateFeedError", map);
                }
                map.put(feedDoc, e);
            }
        }
    }

    private void saveFeed(String guid, SyndFeed feed,XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getWiki().getDocument("XWiki.feed_" + guid, context);

        Vector objs = doc.getObjects("XWiki.FeedEntryClass");

        Iterator it = feed.getEntries().iterator();
        while (it.hasNext())
        {
            SyndEntry entry = (SyndEntry) it.next();
            if (!postExist(objs, entry))
                saveEntry(guid, entry, doc, context);

        }

        context.getWiki().saveDocument(doc, context);
    }

    private void saveEntry(String guid, SyndEntry entry, XWikiDocument doc, XWikiContext context) throws XWikiException {
        int id = doc.createNewObject("XWiki.FeedEntryClass", context);
        BaseObject obj = doc.getObject("XWiki.FeedEntryClass", id);
        obj.setStringValue("feedguid", guid);
        obj.setStringValue("title", entry.getTitle());
        List categList = entry.getCategories();
        StringBuffer categs = new StringBuffer("");
        if (categList != null)
        {
            Iterator it = categList.iterator();
            while(it.hasNext())
            {
                SyndCategory categ = (SyndCategory) it.next();
                if (categs.length() != 0)
                    categs.append(", ");
                categs.append(categ.getName());
            }
        }
        obj.setStringValue("category", categs.toString());

        List contentList = entry.getContents();
        StringBuffer contents = new StringBuffer("");
        if (contentList != null && contentList.size() > 0)
        {
            Iterator it = contentList.iterator();
            while(it.hasNext())
            {
                SyndContent content =  (SyndContent) it.next();
                if (contents.length() != 0)
                    contents.append("\n");
                contents.append(content.getValue());
            }
        }
        obj.setLargeStringValue("content", contents.toString());


        obj.setDateValue("date", entry.getPublishedDate());

        obj.setStringValue("url", entry.getUri());

    }

    private boolean postExist(Vector objs, SyndEntry entry)
    {
        if (objs == null)
            return false;
        Iterator it = objs.iterator();
        String title = entry.getTitle();
        while (it.hasNext())
        {
            BaseObject obj = (BaseObject) it.next();
            if (obj != null)
            {
                if (obj.getStringValue("title").compareTo(title) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public List search(String query, XWikiContext context) throws XWikiException {
        String[] queryTab = query.split(" ");

        if (queryTab.length > 0)
        {
            String sql = "select distinct obj.number, obj.name from BaseObject as obj, StringProperty as prop , LargeStringProperty as lprop " +
                    "where obj.className='XWiki.FeedEntryClass' and obj.id=prop.id.id and obj.id=lprop.id.id ";

            for (int i = 0; i < queryTab.length; i++)
                sql += " and (prop.value LIKE '%" + queryTab[i] + "%' or lprop.value LIKE '%" + queryTab[i] + "%')";
            List res = context.getWiki().search(sql, context);

            if (res == null)
                return null;

            Iterator it = res.iterator();
            List apiObjs = new ArrayList();
            while (it.hasNext())
            {
                try {
                    Object obj[] = (Object[]) it.next();
                    XWikiDocument doc = context.getWiki().getDocument((String) obj[1], context);
                    if (context.getWiki().getRightService().checkAccess("view", doc, context))
                    {
                        BaseObject bObj = doc.getObject("XWiki.FeedEntryClass", ((Integer)obj[0]).intValue());
                        com.xpn.xwiki.api.Object apiObj = new com.xpn.xwiki.api.Object(bObj, context);
                        apiObjs.add(apiObj);
                    }
                }
                catch(Exception e)
                {
                    Map map = (Map) context.get("searchFeedError");
                    if (map==null) {
                        map = new HashMap();
                        context.put("searchFeedError", map);
                    }
                    map.put(query, e);
                }
            }

            Collections.sort(apiObjs, new EntriesComparator());
            return apiObjs;
        }
        return null;
    }

    public com.xpn.xwiki.api.Object getFeedInfosbyGuid(String guid, XWikiContext context) throws XWikiException {
        return getFeedInfosbyGuid("XWiki.FeedList", guid, context);      
    }

    public com.xpn.xwiki.api.Object getFeedInfosbyGuid(String feedDoc, String guid, XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getWiki().getDocument(feedDoc, context);
        Vector objs = doc.getObjects("XWiki.AggregatorURLClass");
        Iterator it = objs.iterator();
        while(it.hasNext())
        {
            BaseObject obj = (BaseObject) it.next();
            if (guid.compareTo(obj.getStringValue("guid")) == 0)
                return new com.xpn.xwiki.api.Object(obj, context);
        }
        return null;
    }
}
