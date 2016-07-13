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
package org.xwiki.security.internal;

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationException;

/**
 * Temporary interface to access user information without depending on oldcore.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface UserBridge
{
    /**
     * Retrieve the collection of group reference for which the user is a member in the given wiki.
     *
     * This method does not cache the results, so it should not be called too often.
     *
     * @param user the user to be queried.
     * @param wikiReference the reference of the wiki where group are evaluated.
     * @return the collection of group reference for which the user is a member in the given wiki.
     * @throws AuthorizationException if an error occurs during retrieval.
     */
    Collection<GroupSecurityReference> getAllGroupsFor(UserSecurityReference user, WikiReference wikiReference)
        throws AuthorizationException;
}
