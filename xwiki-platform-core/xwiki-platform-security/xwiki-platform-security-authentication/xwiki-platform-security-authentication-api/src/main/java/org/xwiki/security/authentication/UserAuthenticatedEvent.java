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
 * This event is triggered every time a user is authenticated.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Unstable
public class UserAuthenticatedEvent implements Event
{
    /**
     * Reference of the user who triggered the authentication event.
     */
    private UserReference userReference;

    /**
     * This event will match any other document event of the same type.
     */
    public UserAuthenticatedEvent()
    {
        super();
    }

    /**
     * This event will match only events of the same type affecting the same document.
     *
     * @param userReference the reference related to the user
     */
    public UserAuthenticatedEvent(UserReference userReference)
    {
        super();
        this.userReference = userReference;
    }

    /**
     * @return the {@link UserReference} of the source user reference.
     */
    public UserReference getUserReference()
    {
        return this.userReference;
    }

    @Override
    public boolean matches(Object other)
    {
        return other instanceof UserAuthenticatedEvent;
    }

}

