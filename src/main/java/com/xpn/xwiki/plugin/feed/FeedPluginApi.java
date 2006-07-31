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
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.feed;

import java.io.IOException;

import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class FeedPluginApi extends Api {
        private FeedPlugin plugin;

        public FeedPluginApi(FeedPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public FeedPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(FeedPlugin plugin) {
        this.plugin = plugin;
    }

    public SyndFeed getFeeds(String sfeeds) throws IOException {
        return plugin.getFeeds(sfeeds, context);
    }

    public SyndFeed getFeeds(String sfeeds, boolean force) throws IOException {
        return plugin.getFeeds(sfeeds, force, context);
    }

    public SyndFeed getFeeds(String sfeeds, boolean ignoreInvalidFeeds, boolean force) throws IOException {
        return plugin.getFeeds(sfeeds, ignoreInvalidFeeds, force, context);
    }

    public SyndFeed getFeed(String sfeed) throws IOException {
        return plugin.getFeed(sfeed, false, context);
    }

    public SyndFeed getFeed(String sfeed, boolean force) throws IOException {
        return plugin.getFeed(sfeed, force, context);
    }

    public SyndFeed getFeed(String sfeed, boolean ignoreInvalidFeeds, boolean force) throws IOException {
        return plugin.getFeed(sfeed, ignoreInvalidFeeds, force, context);
    }

}
