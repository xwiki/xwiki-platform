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
package org.xwiki.notifications.rss;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.stability.Unstable;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

/**
 * Display a notification in a RSS format.
 *
 * @since 9.6RC1
 * @version $Id$
 */
@Role
@Unstable
public interface NotificationRSSRenderer
{
    /**
     * Render a specific composite event.
     *
     * @param event the {@link CompositeEvent} that should be rendered
     * @return the output rss entry
     * @throws NotificationException if an error occurred
     */
    SyndEntry renderNotification(CompositeEvent event)  throws NotificationException;

    /**
     * Render a complete RSS rss.
     *
     * @param events the list of {@link CompositeEvent} that should be included in the rss
     * @return the output rss feed
     * @throws NotificationException if an error occurred
     */
    SyndFeed renderFeed(List<CompositeEvent> events) throws NotificationException;
}
