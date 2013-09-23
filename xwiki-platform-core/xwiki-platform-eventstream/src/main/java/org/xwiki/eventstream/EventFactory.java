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
package org.xwiki.eventstream;

import org.xwiki.component.annotation.Role;

/**
 * A factory which creates {@link Event} objects ready to be used. Besides instantiating an Event object, the factory
 * also fills in some of the fields with the default values, for example a new {@link Event#getId() event ID}, the
 * current {@link Event#getGroupId() event group id}, the current {@link Event#getUser() user}, and the current
 * {@link Event#getDate() date}.
 * 
 * @version $Id$
 * @since 3.0M2
 */
@Role
public interface EventFactory
{
    /**
     * Create a new event with some fields already filled in.
     * <ul>
     * <li>unique event ID</li>
     * <li>the current event group ID</li>
     * <li>the current user</li>
     * <li>the current date</li>
     * </ul>
     * 
     * @return a ready-to-use event
     */
    Event createEvent();

    /**
     * Create a new event without any of the fields filled in.
     * 
     * @return a blank event
     */
    Event createRawEvent();
}
