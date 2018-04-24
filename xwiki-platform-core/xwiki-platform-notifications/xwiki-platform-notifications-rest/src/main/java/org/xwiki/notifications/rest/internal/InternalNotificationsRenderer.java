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
package org.xwiki.notifications.rest.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.CompositeEventStatusManager;
import org.xwiki.notifications.notifiers.NotificationRenderer;
import org.xwiki.notifications.rest.model.Notification;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.text.StringUtils;

/**
 * Internal component to render notifications.
 *
 * @version $Id$
 * @since 10.4RC1
 */
@Component(roles = InternalNotificationsRenderer.class)
@Singleton
public class InternalNotificationsRenderer
{
    @Inject
    private CompositeEventStatusManager compositeEventStatusManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private NotificationRenderer notificationRenderer;

    /**
     * Render the notifications.
     *
     * @param compositeEvents list of composite events to render
     * @param userId id of the current user
     * @param showReadStatus either or not include the "read" status of the events
     * @return the list of notifications
     * @throws Exception if an error occurs
     */
    public List<Notification> renderNotifications(List<CompositeEvent> compositeEvents, String userId,
        boolean showReadStatus) throws Exception
    {
        List<Notification> notifications = new ArrayList<>();

        if (showReadStatus && StringUtils.isNotBlank(userId)) {
            for (CompositeEventStatus status
                    : compositeEventStatusManager.getCompositeEventStatuses(compositeEvents, userId)) {
                notifications.add(toNotification(status.getCompositeEvent(), status.getStatus()));
            }
        } else {
            for (CompositeEvent event : compositeEvents) {
                notifications.add(toNotification(event, null));
            }
        }
        return notifications;
    }

    private Notification toNotification(CompositeEvent event, Boolean status)
    {
        String html = null;
        String exception = null;
        try {
            html = render(event);
        } catch (Exception e) {
            exception = e.toString();
        }

        return new Notification(event, status, html, exception, entityReferenceSerializer);
    }

    private String render(CompositeEvent compositeEvent) throws Exception
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = componentManager.getInstance(BlockRenderer.class, "html/5.0");
        renderer.render(notificationRenderer.render(compositeEvent), printer);
        return printer.toString();
    }
}
