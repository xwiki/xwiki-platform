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
package org.xwiki.notifications.notifiers.email;

import org.xwiki.component.annotation.Role;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;

import java.util.List;

/**
 * Defines how the events should be grouped to be sent in individual emails.
 * Contrarily to {@link org.xwiki.notifications.GroupingEventStrategy} the goal here is not to group individual
 * {@link org.xwiki.eventstream.Event} in {@link CompositeEvent} but to group a list of {@link CompositeEvent} that has
 * been already computed based on the {@link org.xwiki.notifications.GroupingEventStrategy} and decide how many mails
 * should be sent for those {@link CompositeEvent}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Role
public interface NotificationEmailGroupingStrategy
{
    /**
     * Group the given list of {@link CompositeEvent} in sub-lists, where each list represent an email to be sent.
     * Hence, if that method returns a single list containing all {@link CompositeEvent} a single email with all the
     * events will be sent. On the contrary, if it returns as many list as there is events, then there will be as many
     * emails as there is events.
     *
     * @param compositeEvents the list of composite events that should be sent by email
     * @return a list of list of events where each list of events represents an email
     * @throws NotificationException in case of problem to compute the sub-lists
     */
    List<List<CompositeEvent>> groupEventsPerMail(List<CompositeEvent> compositeEvents) throws NotificationException;
}
