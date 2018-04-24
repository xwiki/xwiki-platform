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
package org.xwiki.notifications.sources;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;

/**
 * A notification manager that work only with the parameters given to it (it will not look at the current user
 * preferences, and so on) but that still search the events where they are stored.
 *
 * @version $Id$
 * @since 10.4RC1
 */
@Role
public interface ParametrizedNotificationManager
{
    /**
     * Get the events matching the given parameters.
     * @param parameters parameters to take care of
     * @return a list of the corresponding composite events
     * @throws NotificationException if an error occurs
     */
    List<CompositeEvent> getEvents(NotificationParameters parameters) throws NotificationException;
}
