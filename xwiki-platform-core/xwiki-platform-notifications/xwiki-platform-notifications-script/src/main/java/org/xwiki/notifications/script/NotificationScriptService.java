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
import org.xwiki.eventstream.EventStatus;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.script.internal.NotificationScriptEventHelper;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;

/**
 * Script services for the notifications.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
@Named(NotificationScriptService.ROLE_HINT)
public class NotificationScriptService implements ScriptService
{
    /**
     * Hint of the component.
     */
    public static final String ROLE_HINT = "notification";

    @Inject
    private NotificationConfiguration notificationConfiguration;

    @Inject
    private NotificationScriptEventHelper notificationScriptEventHelper;

    @Inject
    private ScriptServiceManager scriptServiceManager;


    /**
     * Get a sub script service related to wiki. (Note that we're voluntarily using an API name of "get" to make it
     * extra easy to access Script Services from Velocity (since in Velocity writing <code>$services.wiki.name</code> is
     * equivalent to writing <code>$services.wiki.get("name")</code>). It also makes it a short and easy API name for
     * other scripting languages.
     *
     * @param serviceName id of the script service
     * @return the service asked or null if none could be found
     */
    public ScriptService get(String serviceName)
    {
        return scriptServiceManager.get(ROLE_HINT + '.' + serviceName);
    }

    /**
     * Get the list of statuses concerning the given events and the current user.
     *
     * @param events a list of events
     * @return the list of statuses corresponding to each pair or event/entity
     *
     * @throws Exception if an error occurs
     */
    public List<EventStatus> getEventStatuses(List<Event> events) throws Exception
    {
        return notificationScriptEventHelper.getEventStatuses(events);
    }

    /**
     * Get the list of statuses concerning the given composite events and the current user.
     *
     * @param compositeEvents a list of composite events
     * @return the list of statuses corresponding to each pair or event/entity
     *
     * @throws Exception if an error occurs
     * @since 9.4RC1
     */
    public List<CompositeEventStatus> getCompositeEventStatuses(List<CompositeEvent> compositeEvents) throws Exception
    {
        return notificationScriptEventHelper.getCompositeEventStatuses(compositeEvents);
    }

    /**
     * Get the status of the module.
     * 
     * @return true if the notification module is enabled in the platform configuration
     */
    public boolean isEnabled()
    {
        return notificationConfiguration.isEnabled();
    }

    /**
     * Get the status of the module.
     *
     * @return true if the notification module can send emails
     * @since 9.5RC1
     */
    public boolean areEmailsEnabled()
    {
        return notificationConfiguration.areEmailsEnabled();
    }

    /**
     * Save a status for the current user.
     * @param eventId id of the event
     * @param isRead either or not the current user has read the given event
     * @throws Exception if an error occurs
     */
    public void saveEventStatus(String eventId, boolean isRead) throws Exception
    {
        notificationScriptEventHelper.saveEventStatus(eventId, isRead);
    }
}
