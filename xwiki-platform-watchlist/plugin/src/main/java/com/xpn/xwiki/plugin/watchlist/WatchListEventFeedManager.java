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

import java.util.List;
import org.apache.commons.lang.StringUtils;

import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityStreamPluginApi;

/**
 * Manager for watchlist events RSS feeds.
 * 
 * @version $Id$
 */
public class WatchListEventFeedManager
{
    /**
     * The watchlist plugin instance.
     */
    private final WatchListPlugin plugin;

    /**
     * Constructor.
     * 
     * @param plugin Watchlist plugin instance.
     */
    public WatchListEventFeedManager(WatchListPlugin plugin)
    {
        this.plugin = plugin;
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
        List<String> wikis = plugin.getStore().getWatchedElements(user, WatchListStore.ElementType.WIKI, context);
        List<String> spaces = plugin.getStore().getWatchedElements(user, WatchListStore.ElementType.SPACE, context);
        List<String> pages = plugin.getStore().getWatchedElements(user, WatchListStore.ElementType.DOCUMENT, context);
        ActivityStreamPluginApi asApi =
            (ActivityStreamPluginApi) context.getWiki().getPluginApi("activitystream", context);
        String listItemsJoint = "','";
        List<ActivityEvent> events =
            asApi.searchEvents("act.wiki in ('" + StringUtils.join(wikis, listItemsJoint) + "') or act.space in ('"
                + StringUtils.join(spaces, listItemsJoint) + "') or act.page in ('"
                + StringUtils.join(pages, listItemsJoint) + "')", false, entryNumber, 0);
        String msgPrefix = WatchListPlugin.APP_RES_PREFIX + "rss.";
        SyndFeed feed = asApi.getFeed(events);
        
        feed.setAuthor(context.getMessageTool().get(msgPrefix + "author"));
        feed.setTitle(context.getMessageTool().get(msgPrefix + "title")); 
        feed.setDescription(context.getMessageTool().get(msgPrefix + "description")); 
        feed.setCopyright(context.getWiki().getXWikiPreference("copyright", context));
        feed.setLink(context.getWiki().getExternalURL("xwiki:Main.WebHome", "view", context));

        return feed;
    }
}
