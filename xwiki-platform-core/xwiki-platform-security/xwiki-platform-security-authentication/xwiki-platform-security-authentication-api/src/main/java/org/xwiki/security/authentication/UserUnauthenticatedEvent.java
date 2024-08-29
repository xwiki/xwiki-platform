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

package org.xwiki.security.authentication;

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Event triggered whenever a user is logged out.
 *
 * @version $Id$
 * @since 16.8.0RC1
 * @since 16.4.3
 * @since 15.10.13
 */
@Unstable
public class UserUnauthenticatedEvent implements Event
{
    /**
     * The reference related to an authenticated user for whom a {@link UserUnauthenticatedEvent} has been triggered.
     */
    private final UserReference userReference;

    /**
     * Default constructor without user reference for matching.
     */
    public UserUnauthenticatedEvent()
    {
        this(null);
    }

    /**
     * Default constructor.
     *
     * @param userReference The reference related to an authenticated user for whom a {@link UserUnauthenticatedEvent}
     * has been triggered.
     */
    public UserUnauthenticatedEvent(UserReference userReference)
    {
        this.userReference = userReference;
    }

    /**
     * @return the {@link UserReference} of the authenticated user.
     */
    public UserReference getUserReference()
    {
        return this.userReference;
    }

    @Override
    public boolean matches(Object other)
    {
        return other instanceof UserUnauthenticatedEvent;
    }
}
