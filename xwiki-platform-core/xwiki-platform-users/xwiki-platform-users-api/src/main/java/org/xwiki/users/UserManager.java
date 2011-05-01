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
package org.xwiki.users;

import org.xwiki.component.annotation.ComponentRole;

/**
 * User Management.
 * 
 * @version $Id:$
 */
@ComponentRole
public interface UserManager
{
    /**
     * The user corresponding to the identifier.
     * 
     * @param identifier the user identifier to resolve: the username passed in the login form, a serialized identifier
     *            stored in the document's metadata, or an identifier passed by an external authentication service
     * @return the corresponding user, if found, or {@code null} otherwise
     */
    User getUser(String identifier);

    /**
     * The user corresponding to the identifier. If no existing user is found and the {@code force} parameter is {@code
     * true}, return a new user in the default user management system.
     * 
     * @param identifier the user identifier to resolve: the username passed in the login form, a serialized identifier
     *            stored in the document's metadata, or an identifier passed by an external authentication service
     * @param force whether to force returning a new profile in case the user is not found
     * @return the corresponding user, if found; a new user profile if {@code force} is {@code true}; {@code null}
     *         otherwise
     */
    User getUser(String identifier, boolean force);
}
