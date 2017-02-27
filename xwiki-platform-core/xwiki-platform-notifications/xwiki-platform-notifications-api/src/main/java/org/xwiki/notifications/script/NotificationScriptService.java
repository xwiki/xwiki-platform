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
package org.xwiki.notifications.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.service.ScriptService;

/**
 * @version $Id$
 */
@Component
@Singleton
@Named("notification")
public class NotificationScriptService implements ScriptService
{
    @Inject
    private NotificationManager notificationManager;

    public List<Event> getEvents(int offset, int limit) throws NotificationException
    {
        return notificationManager.getEvents(offset, limit);
    }

    public List<Event> getEvents(String userId, int offset, int limit) throws NotificationException
    {
        return notificationManager.getEvents(userId, offset, limit);
    }

    public XDOM render(Event event) throws NotificationException
    {
        return notificationManager.render(event);
    }
}
