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
package com.xpn.xwiki.plugin.watchlist;

import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

/**
 * Manager for watchlist events RSS feeds.
 * 
 * @version $Id$
 */
@Deprecated
public class WatchListEventFeedManager
{
    /**
     * Constructor.
     * 
     * @param plugin Watchlist plugin instance.
     */
    public WatchListEventFeedManager(WatchListPlugin plugin)
    {
    }

    /**
     * @param user user to get the RSS feed for
     * @param entryNumber number of entries to retrieve
     * @param context the XWiki context
     * @return The watchlist RSS feed for the given user
     * @throws XWikiException if the retrieval of RSS entries fails
     */
    public SyndFeed getFeed(String user, int entryNumber, XWikiContext context) throws XWikiException
    {
        // Delegate to the component.
        org.xwiki.watchlist.internal.api.WatchListEventFeedManager feedManager =
            Utils.getComponent(org.xwiki.watchlist.internal.api.WatchListEventFeedManager.class);
        return feedManager.getFeed(user, entryNumber);
    }
}
