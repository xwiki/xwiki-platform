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
package org.xwiki.user.rest;

import java.net.URI;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;
import org.xwiki.user.rest.model.jaxb.User;
import org.xwiki.user.rest.model.jaxb.UserSummary;

import com.xpn.xwiki.XWikiException;

/**
 * A component that converts a User Reference to REST API model elements.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
@Role
@Unstable
public interface UserReferenceModelSerializer
{
    /**
     * Return a summary of the user's information to include in, e.g., user lists.
     *
     * @param baseUri the base URL of the current instance
     * @param userId the id of the user (e.g., "wikiName:Space.Page" for a user stored as a document)
     * @param userReference the user reference to serialize
     * @return a user summary
     * @throws XWikiException if there was a problem during serialization
     */
    UserSummary toRestUserSummary(URI baseUri, String userId, UserReference userReference) throws XWikiException;

    /**
     * Return all the information available on the user.
     *
     * @param baseUri the base URL of the current instance
     * @param userId the id of the user (e.g., "wikiName:Space.Page" for a user stored as a document)
     * @param userReference the user reference to serialize
     * @param preferences whether to include user preferences in the output
     * @return a user
     * @throws XWikiException if there was a problem during serialization
     */
    User toRestUser(URI baseUri, String userId, UserReference userReference, boolean preferences) throws XWikiException;

    /**
     * Check that the current user has access to the user information.
     *
     * @param userReference the user reference we're trying to access
     * @return true if the current user can read information from {userReference}, false otherwise
     */
    boolean hasAccess(UserReference userReference);
}
