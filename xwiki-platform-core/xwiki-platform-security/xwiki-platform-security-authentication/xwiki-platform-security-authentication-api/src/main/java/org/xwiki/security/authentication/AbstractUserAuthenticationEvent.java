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

import java.util.Objects;

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Abstract event related to authentication.
 *
 * @version $Id$
 * @since 16.8.0RC1
 */
@Unstable
public abstract class AbstractUserAuthenticationEvent implements Event
{
    /**
     * The reference related to an authenticated user for whom the event has been triggered.
     */
    private final UserReference userReference;

    /**
     * This event will match only events of the same type affecting the same user.
     *
     * @param userReference The reference related to an authenticated user for whom the event
     * has been triggered.
     */
    public AbstractUserAuthenticationEvent(UserReference userReference)
    {
        this.userReference = userReference;
    }

    /**
     * @return the {@link UserReference} of the user for whom the event is triggered.
     */
    public UserReference getUserReference()
    {
        return this.userReference;
    }

    @Override
    public boolean matches(Object other)
    {
        return other instanceof AbstractUserAuthenticationEvent
            && Objects.equals(((AbstractUserAuthenticationEvent) other).getUserReference(), this.userReference);
    }
}
