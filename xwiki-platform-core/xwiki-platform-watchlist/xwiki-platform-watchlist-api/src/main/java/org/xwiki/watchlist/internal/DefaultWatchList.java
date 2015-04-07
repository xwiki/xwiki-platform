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
package org.xwiki.watchlist.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.watchlist.internal.api.WatchList;
import org.xwiki.watchlist.internal.api.WatchListEventFeedManager;
import org.xwiki.watchlist.internal.api.WatchListNotifier;
import org.xwiki.watchlist.internal.api.WatchListStore;

/**
 * Default implementation for {@link WatchList}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWatchList implements WatchList
{
    /**
     * Prefix used in ApplicationResources for this plugin.
     */
    public static final String APP_RES_PREFIX = "watchlist.";

    /**
     * Store instance.
     */
    @Inject
    private WatchListStore store;

    /**
     * Notifier instance.
     */
    @Inject
    private WatchListNotifier notifier;

    /**
     * Feed manager instance.
     */
    @Inject
    private WatchListEventFeedManager feedManager;

    /**
     * @return the store instance.
     */
    public WatchListStore getStore()
    {
        return this.store;
    }

    /**
     * @return the notifier instance.
     */
    public WatchListNotifier getNotifier()
    {
        return this.notifier;
    }

    /**
     * @return the feed manager instance.
     */
    public WatchListEventFeedManager getFeedManager()
    {
        return this.feedManager;
    }
}
