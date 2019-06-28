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
package org.xwiki.security.authentication.api;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

/**
 * This event occurs when a login authentication failure reach the level defined in the configuration (e.g. an user
 * failing his authentication 3 times in less than 5 minutes). See {@link AuthenticationConfiguration}.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Unstable
public class AuthenticationFailureLimitReachedEvent implements Event
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof AuthenticationFailureLimitReachedEvent;
    }

    public boolean equals(Object other)
    {
        return other instanceof AuthenticationFailureLimitReachedEvent;
    }

    public int hashCode()
    {
        return new HashCodeBuilder(39, 43).hashCode();
    }
}
