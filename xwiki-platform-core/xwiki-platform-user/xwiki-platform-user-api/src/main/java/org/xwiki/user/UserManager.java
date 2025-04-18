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
import org.xwiki.model.reference.WikiReference;
import org.xwiki.stability.Unstable;

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

    /**
     * Verify if a wiki contains any user. This method only look at the given wiki, it's not taking into account global
     * users when the target wiki is a sub-wiki.
     * 
     * @param wiki the reference of the wiki where to search for users
     * @return true if at least one user is available on that target wiki
     * @throws UserException when failing to check if a user exist on the target wiki
     * @since 17.4.0RC1
     */
    @Unstable
    default boolean hasUsers(WikiReference wiki) throws UserException
    {
        return false;
    }
}
