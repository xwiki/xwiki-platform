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
package org.xwiki.like.events;

import java.io.Serializable;

import org.xwiki.observation.event.Event;

/**
 * An event sent when a Like action is performed.
 *
 * The following information are sent along with the event:
 *   - source: a {@link org.xwiki.user.UserReference} of the user who performs the like
 *   - data: the {@link org.xwiki.model.reference.EntityReference} being target of the like.
 *
 * Note that we need those events to be serializable so that they can be sent on cluster.
 *
 * @version $Id$
 * @since 12.7RC1
 */
public class LikeEvent implements Event, Serializable
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof LikeEvent;
    }
}
