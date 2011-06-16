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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ConstantTransformer;
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
    @SuppressWarnings("unchecked")
    public SyndFeed getFeed(String user, int entryNumber, XWikiContext context) throws XWikiException
    {
        List<String> wikis = plugin.getStore().getWatchedElements(user, WatchListStore.ElementType.WIKI, context);
        List<String> spaces = plugin.getStore().getWatchedElements(user, WatchListStore.ElementType.SPACE, context);
        List<String> documents = 
            plugin.getStore().getWatchedElements(user, WatchListStore.ElementType.DOCUMENT, context);
        List<Object> parameters = new ArrayList<Object>(); 
        ActivityStreamPluginApi asApi =
            (ActivityStreamPluginApi) context.getWiki().getPluginApi("activitystream", context);

        parameters.addAll(wikis);
        parameters.addAll(spaces);
        parameters.addAll(documents);

        Transformer transformer = new ConstantTransformer("?");
        List<String> wikisPlaceholders = ListUtils.transformedList(new ArrayList<String>(), transformer);
        wikisPlaceholders.addAll(wikis);
        List<String> spacesPlaceholders = ListUtils.transformedList(new ArrayList<String>(), transformer);
        spacesPlaceholders.addAll(spaces);
        List<String> documentsPlaceholders = ListUtils.transformedList(new ArrayList<String>(), transformer);
        documentsPlaceholders.addAll(documents);

        String listItemsJoint = ",";
        String concatWiki = " or concat(act.wiki,'";
        String query = "1=0";
        if (!wikis.isEmpty()) {
            query += " or act.wiki in (" + StringUtils.join(wikisPlaceholders, listItemsJoint) + ')';
        }
        if (!spaces.isEmpty()) {
            query += concatWiki + WatchListStore.WIKI_SPACE_SEP + "',act.space) in ("
                + StringUtils.join(spacesPlaceholders, listItemsJoint) + ')';
        }
        if (!documents.isEmpty()) {
            query += concatWiki + WatchListStore.WIKI_SPACE_SEP + "',act.page) in ("
                + StringUtils.join(documentsPlaceholders, listItemsJoint) + ')';
        }
        List<ActivityEvent> events = asApi.searchEvents(query, false, true, entryNumber, 0, parameters);

        SyndFeed feed = asApi.getFeed(events);
        setFeedMetaData(feed, context);

        return feed;
    }

    /**
     * Set the standard feed metadata values, based on static translated messages and wiki configuration.
     *
     * @param feed the feed to configure
     * @param context the current request context
     * @throws XWikiException if the wiki can't be properly accessed
     */
    private void setFeedMetaData(SyndFeed feed, XWikiContext context) throws XWikiException
    {
        String msgPrefix = WatchListPlugin.APP_RES_PREFIX + "rss.";
        feed.setAuthor(context.getMessageTool().get(msgPrefix + "author"));
        feed.setTitle(context.getMessageTool().get(msgPrefix + "title")); 
        feed.setDescription(context.getMessageTool().get(msgPrefix + "description")); 
        feed.setCopyright(context.getWiki().getXWikiPreference("copyright", context));
        feed.setLink(context.getWiki().getExternalURL("xwiki:Main.WebHome", "view", context));
    }
}
