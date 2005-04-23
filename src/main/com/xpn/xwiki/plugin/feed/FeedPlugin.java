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
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.cache.impl.OSCacheCache;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.io.IOException;

public class FeedPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
        private static Log mLogger =
                LogFactory.getFactory().getInstance(com.xpn.xwiki.plugin.feed.FeedPlugin.class);

        private XWikiCache feedCache;
        private int refreshPeriod;

        public FeedPlugin(String name, String className, XWikiContext context) {
            super(name, className, context);
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
