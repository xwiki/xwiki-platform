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
package org.xwiki.mentions.events;

import org.xwiki.observation.event.Event;

/**
 * An event sent at the end of the analysis of a document change (i.e., a created or updated document).
 * <p>
 * The following information is sent along the event:
 * <ul>
 *     <li>source: the reference of the user who made the change</li>
 *     <li>data: the {@link org.xwiki.mentions.notifications.MentionNotificationParameters} holding the
 *     list of mentions introduced by the change on a given entity</li>
 * </ul>
 *
 * @version $Id$
 * @since 12.10
 */
public class NewMentionsEvent implements Event
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof NewMentionsEvent;
    }
}
