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
package org.xwiki.notifications.notifiers;

import org.xwiki.component.annotation.Role;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;

/**
 * Render an event as a notification.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
public interface NotificationRenderer
{
    /**
     * Generate a rendering Block for a given event to display as notification.
     * @param event the event to render
     * @return a rendering block ready to display the event
     * @throws NotificationException if an error happens
     */
    Block render(CompositeEvent event) throws NotificationException;
}
