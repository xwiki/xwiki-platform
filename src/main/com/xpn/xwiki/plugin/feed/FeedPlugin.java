/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 23 avr. 2005
 * Time: 00:21:43
 */
package com.xpn.xwiki.plugin.feed;

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.calendar.CalendarPluginApi;
import com.xpn.xwiki.plugin.calendar.CalendarPlugin;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.cache.impl.OSCacheCache;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.io.IOException;
import java.util.*;

public class FeedPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
        private static Log mLogger =
                LogFactory.getFactory().getInstance(com.xpn.xwiki.plugin.feed.FeedPlugin.class);

        private XWikiCache feedCache;
        private int refreshPeriod;

      public static class SyndEntryComparator
      implements Comparator {
    public int compare(Object element1, Object element2) {
        SyndEntry entrie1 = (SyndEntry) element1;
        SyndEntry entrie2 = (SyndEntry) element2;

        if ((entrie1.getPublishedDate() == null) &&  (entrie2.getPublishedDate() == null))
            return 0;
        if (entrie1.getPublishedDate() == null)
            return 1;
        if (entrie2.getPublishedDate() == null)
            return -1;
        return (-entrie1.getPublishedDate().compareTo(entrie2.getPublishedDate()));
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
    }

    public void init(XWikiContext context) {
        super.init(context);
        feedCache = new OSCacheCache();
        refreshPeriod = (int) context.getWiki().ParamAsLong("xwiki.plugins.feed.cacherefresh", 3600);
    }

    public SyndFeed getFeeds(String sfeeds, XWikiContext context) throws IOException {
        String[] feeds = sfeeds.split("\\|");
        List entries = new ArrayList();
        SyndFeed outputFeed = new SyndFeedImpl();
        outputFeed.setTitle("XWiki Feeds");
        outputFeed.setAuthor("XWiki Team");
        outputFeed.setEntries(entries);

        for (int i = 0; i < feeds.length; i++)
        {
            SyndFeed feed = getFeed(feeds[i], context);
            if (feed != null)
                entries.addAll(feed.getEntries());
        }
        SyndEntryComparator comp = new SyndEntryComparator();
        Collections.sort(entries, comp);
        return outputFeed;
    }


    public SyndFeed getFeed(String sfeed, XWikiContext context) throws IOException {
        return getFeed(sfeed, false, context);
    }

    public SyndFeed getFeed(String sfeed, boolean force, XWikiContext context) throws IOException {
        SyndFeed feed = null;
        if (!force)
        try {
          feed = (SyndFeed) feedCache.getFromCache(sfeed, refreshPeriod);

        } catch (XWikiCacheNeedsRefreshException e) {
            feedCache.cancelUpdate(sfeed);
        } catch (Exception e) {
        }

        if (feed==null)
         feed = getFeedForce(sfeed, context);

        if (feed!=null)
         feedCache.putInCache(sfeed, feed);

        return feed;
    }

    public SyndFeed getFeedForce(String sfeed, XWikiContext context) throws IOException {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = null;
            URL feedURL = new URL(sfeed);
            try {
                feed = input.build(new XmlReader(feedURL));
            }
            catch (Exception ex) {
                throw new java.io.IOException("Error processing " + feedURL + ": " + ex.getMessage());
            }
            return feed;
        }
}
