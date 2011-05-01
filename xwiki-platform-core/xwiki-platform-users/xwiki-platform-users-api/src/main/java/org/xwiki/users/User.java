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

import java.net.URI;

import org.xwiki.model.reference.DocumentReference;

/**
 * XWiki User Interface.
 * 
 * @version $Id$
 * @since 3.1M2
 */
public interface User extends Comparable<User>
{
    /**
     * Check if the returned user profile actually exists or not.
     * 
     * @return {@code true} if the user profile is valid (e.g. the profile wiki document exists for a wiki user)
     */
    boolean exists();

    /**
     * An identifier which can be used internally for identifying the user. This is the value that should be stored to
     * remember the user.
     * 
     * @return serialized user identifier which can be used to store and retrieve back this user object
     */
    String getId();

    /**
     * The username used for identifying the user in the form. This is not supposed to be used internally, but only as a
     * user-friendly "username" to display to this or other users, for example in a mail reminding what username to use
     * on the wiki.
     * 
     * @return short username
     */
    String getUsername();

    /**
     * The real name of the user. The returned value should be in the "Givenname Familyname" format.
     * 
     * @return user full (real) name, displayed in the UI to other wiki users
     */
    String getName();

    /**
     * If the user has an associated wiki document where their profile can be seen, return a reference to it. This
     * happens for users defined in the wiki, or for SSO users mirrored/cloned in the wiki. For external SSO users
     * without a profile clone in the wiki, {@code null} is returned.
     * 
     * @return a reference to the user's profile document, if one exists, or {@code null} otherwise
     */
    DocumentReference getProfileDocument();

    /**
     * If the user has an associated URI where their profile can be seen, return it. For users defined (or mirrored) in
     * the wiki, a link to their profile document is returned. For external SSO users with a publicly accessible
     * profile, a link to their external profile is returned. For SSO services not accessible on the web, {@code null}
     * is returned.
     * 
     * @return a link to the user's profile page, if one exists, or {@code null} otherwise
     */
    URI getProfileURI();

    /**
     * Get the value of an attribute defined for the user. Some example attributes are the user's given and family
     * names, email address, company, birth date. Actual attributes depend on the actual user management system.
     * 
     * @param attributeName the name of the attribute to retrieve
     * @return the attribute value, if defined, or {@code null} otherwise
     */
    Object getAttribute(String attributeName);
}
