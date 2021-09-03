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
package org.xwiki.security.authorization.testwikibuilding;

/**
 * This interface is used for building a mocked test setup for testing the authorization manager.
 *
 * Interface for entities holding that have an ACL.  I.e., a wiki, a space or a document entity.
 *
 * @since 4.2
 * @version $Id$
 */
public interface HasAcl
{

    /**
     * Add an "allow" right to a named user.
     * 
     * @param username The username to associate with a right.
     * @param type Of the right.  May be "login", "register", "view", "edit", "admin", or "programming".
     */
    void addAllowUser(String username, String type);

    /**
     * Add a "deny" right to a named user.
     * 
     * @param username The username to associate with a right.
     * @param type Of the right.  May be "login", "register", "view", "edit", "admin", or "programming".
     */
    void addDenyUser(String username, String type);

    /**
     * Add an "allow" right to a named group.
     * 
     * @param groupname The username to associate with a right.
     * @param type Of the right.  May be "login", "register", "view", "edit", "admin", or "programming".
     */
    void addAllowGroup(String groupname, String type);

    /**
     * Add a "deny" right to a named group.
     * 
     * @param groupname The username to associate with a right.
     * @param type Of the right.  May be "login", "register", "view", "edit", "admin", or "programming".
     */
    void addDenyGroup(String groupname, String type);

}
