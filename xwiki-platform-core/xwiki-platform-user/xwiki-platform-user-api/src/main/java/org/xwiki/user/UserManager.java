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

/**
 * CRUD operations on users. Note that for retrieving a user's properties you should use a
 * {@link UserPropertiesResolver}.
 *
 * @version $Id$
 * @since 12.2
 */
@Role
public interface UserManager
{
    /**
     * @param userReference the reference to the user to check for existence
     * @return true if the user pointed to by the reference exists, false otherwise
     * @throws UserException (since 14.6RC1, 14.4.3, 13.10.8) in case of error while checking for the existence of
     *     the user
     */
    boolean exists(UserReference userReference) throws UserException;
}
