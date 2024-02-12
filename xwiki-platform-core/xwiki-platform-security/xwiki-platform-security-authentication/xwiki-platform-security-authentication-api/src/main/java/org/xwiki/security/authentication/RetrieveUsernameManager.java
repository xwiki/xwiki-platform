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

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.user.UserReference;

/**
 * Default manager for handling requests of user who lost their usernames.
 *
 * @version $Id$
 * @since 14.9
 * @since 14.4.6
 * @since 13.10.10
 */
@Role
public interface RetrieveUsernameManager
{
    /**
     * Look for the users registered with the given email address in current wiki and main wiki if none can be found in
     * current one.
     *
     * @param requestEmail the email to use to find users.
     * @return a set of user references that have been found matching the mail.
     * @throws RetrieveUsernameException in case of problem to execute the query for finding users.
     */
    Set<UserReference> findUsers(String requestEmail) throws RetrieveUsernameException;

    /**
     * Send an email to the given username with the username information of the given user references.
     *
     * @param requestEmail the email address to use for sending the information
     * @param userReferences the actual user references that can be used by this user
     * @throws RetrieveUsernameException in case of problem when sending the email, or if the email address is wrong, or
     *         if the set of user references is empty.
     */
    void sendRetrieveUsernameEmail(String requestEmail, Set<UserReference> userReferences)
        throws RetrieveUsernameException;
}
