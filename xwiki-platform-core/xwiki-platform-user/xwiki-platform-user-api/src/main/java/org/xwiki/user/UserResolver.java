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
package org.xwiki.user;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Convert a raw representation of a user  into a {@link User} object (for example converts a User Reference into a
 * User).
 *
 * @param <T> the type of the user representation
 * @version $Id$
 * @since 12.2RC1
 */
@Unstable
@Role
public interface UserResolver<T>
{
    /**
     * @param userRepresentation the representation of the user (e.g. User Reference, oldcore XWikiUser object etc)
     * @param parameters optional parameters that have a meaning only for the specific resolver implementation used
     * @return the User object
     */
    User resolve(T userRepresentation, Object... parameters);
}
