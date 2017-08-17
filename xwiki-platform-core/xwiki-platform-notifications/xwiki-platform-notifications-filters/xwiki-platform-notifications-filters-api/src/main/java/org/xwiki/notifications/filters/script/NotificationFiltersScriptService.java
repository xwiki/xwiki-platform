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
package org.xwiki.notifications.filters.script;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.script.service.ScriptService;

/**
 * Script service for the notification filters.
 *
 * @since 9.7RC1
 * @version $Id$
 */
@Component
@Named("notification.filters")
@Singleton
public class NotificationFiltersScriptService implements ScriptService
{
    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Get a set of notification filters that can be toggled by the current user.
     *
     * @return a set of notification filters that are toggleable
     * @throws NotificationException if an error occurs
     */
    public Set<NotificationFilter> getToggleableNotificationFilters() throws NotificationException
    {
        return notificationFilterManager.getToggleableFilters(documentAccessBridge.getCurrentUserReference());
    }
}
