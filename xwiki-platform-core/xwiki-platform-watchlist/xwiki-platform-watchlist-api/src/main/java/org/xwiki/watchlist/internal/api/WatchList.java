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

/**
 * Offers WatchList features to XWiki. These feature allow users to build lists of pages and spaces they want to follow.
 * At a frequency chosen by the user, XWiki will send an email notification to him with a list of the elements that has
 * been modified since the last notification.
 * 
 * @version $Id$
 */
@Role
public interface WatchList
{
    /**
     * @return the store instance.
     */
    WatchListStore getStore();

    /**
     * @return the notifier instance.
     */
    WatchListNotifier getNotifier();

    /**
     * @return the feed manager instance.
     */
    WatchListEventFeedManager getFeedManager();
}
