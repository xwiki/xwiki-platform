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
package org.xwiki.notifications;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.Event;
import org.xwiki.stability.Unstable;

/**
 * Component responsible to perform the grouping based on the available {@link GroupingEventStrategy}, on the targeted
 * output, and on the preference of the users.
 *
 * @version $Id$
 * @since 15.4RC1
 */
@Role
@Unstable
public interface GroupingEventManager
{
    /**
     * Retrieve the {@link GroupingEventStrategy} based on the given information, or use a default strategy and apply
     * it to group the given events.
     *
     * @param events the list of events to group
     * @param userId the identifier of the user for whom the grouping is performed
     * @param target the output target (e.g. email or alert)
     * @return a list of composite events as computed by {@link GroupingEventStrategy#group(List)}
     * @throws NotificationException in case of problem when performing the grouping
     */
    List<CompositeEvent> getCompositeEvents(List<Event> events, String userId, String target) throws
        NotificationException;
}
