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
package org.xwiki.notifications.notifiers.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationRenderer;
import org.xwiki.notifications.notifiers.rss.NotificationRSSManager;
import org.xwiki.notifications.sources.NotificationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.service.ScriptService;

import com.rometools.rome.io.SyndFeedOutput;

/**
 * Script service for the notification notifiers.
 *
 * @since 9.7RC1
 * @version $Id$
 */
@Component
@Named("notification.notifiers")
@Singleton
public class NotificationNotifiersScriptService implements ScriptService
{
    @Inject
    private NotificationRenderer notificationRenderer;

    @Inject
    private NotificationRSSManager notificationRSSManager;

    @Inject
    private NotificationManager notificationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Generate a rendering Block for a given event to display as notification.
     * @param event the event to render
     * @return a rendering block ready to display the event
     *
     * @throws NotificationException if an error happens
     */
    public Block render(CompositeEvent event) throws NotificationException
    {
        return notificationRenderer.render(event);
    }

    /**
     * Get the RSS notifications feed of the given user.
     *
     * @param entryNumber number of entries to get
     * @return the notifications RSS feed
     * @throws NotificationException if an error occurs
     *
     * @since 10.1RC1
     */
    public String getFeed(int entryNumber) throws NotificationException
    {
        String userId = entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference());
        return this.getFeed(userId, entryNumber);
    }

    /**
     * Get the RSS notifications feed of the given user.
     *
     * @param userId id of the user
     * @param entryNumber number of entries to get
     * @return the notifications RSS feed
     * @throws NotificationException if an error occurs
     *
     * @since 10.1RC1
     */
    public String getFeed(String userId, int entryNumber) throws NotificationException
    {
        SyndFeedOutput output = new SyndFeedOutput();
        try {
            return output.outputString(this.notificationRSSManager.renderFeed(
                    this.notificationManager.getEvents(userId, entryNumber)));
        } catch (Exception e) {
            throw new NotificationException("Unable to render RSS feed", e);
        }
    }
}
