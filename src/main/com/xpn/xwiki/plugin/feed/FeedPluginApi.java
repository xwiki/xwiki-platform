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
 * Time: 00:57:33
 */
package com.xpn.xwiki.plugin.feed;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.calendar.CalendarPlugin;
import com.xpn.xwiki.XWikiContext;
import com.sun.syndication.feed.synd.SyndFeed;

import java.io.IOException;

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

    public SyndFeed getFeed(String sfeed) throws IOException {
        return plugin.getFeed(sfeed, false, context);
    }

    public SyndFeed getFeed(String sfeed, boolean force) throws IOException {
        return plugin.getFeed(sfeed, force, context);
    }

}
