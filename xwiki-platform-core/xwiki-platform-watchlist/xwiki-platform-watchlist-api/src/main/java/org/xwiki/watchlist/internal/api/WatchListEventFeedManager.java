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
package org.xwiki.watchlist.internal.api;

import org.xwiki.component.annotation.Role;

import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiException;

/**
 * Manager for watchlist events RSS feeds.
 * 
 * @version $Id$
 */
@Role
public interface WatchListEventFeedManager
{
    /**
     * @param user user to get the RSS feed for
     * @param entryNumber number of entries to retrieve
     * @return The watchlist RSS feed for the given user
     * @throws XWikiException if the retrieval of RSS entries fails
     */
    SyndFeed getFeed(String user, int entryNumber) throws XWikiException;
}
