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

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.stability.Unstable;

/**
 * Represents an XWiki user. Note that it's independent from where users are stored, and should remain that way, so
 * that we can switch the user store in the future.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Unstable
public interface User extends ConfigurationSource
{
    /**
     * Represents the Guest user, see {@link GuestUser}.
     */
    User GUEST = GuestUser.INSTANCE;

    /**
     * Represents the Super Admin user, see {@link SuperAdminUser}.
     */
    User SUPERADMIN = SuperAdminUser.INSTANCE;

    /**
     * @return true if the user is configured to display hidden documents in the wiki
     */
    boolean displayHiddenDocuments();

    /**
     * @return true if the user is active in the wiki. An active user can log in.
     */
    boolean isActive();

    /**
     * @return the first name of the user or null if not set
     */
    String getFirstName();

    /**
     * @return the last name of the user or null if not set
     */
    String getLastName();

    /**
     * @return the email address of the user and null if not set
     */
    String getEmail();

    /**
     * @return the type of the user (simple user, advanced user)
     * @see <a href="https://bit.ly/37TUlCp">user profile</a>
     */
    UserType getType();

    /**
     * @return true if the user's email has been checked. In some configurations, users must have had their emails
     *         verified before they can access the wiki. Also, disabled users must have their emails checked to be
     *         able to view pages.
     */
    boolean isEmailChecked();

    /**
     * @return true if this user is registered in the main wiki (i.e. it's a global user)
     */
    boolean isGlobal();

    /**
     * @return the reference to his user (i.e. a way to retrieve this user's data)
     */
    UserReference getUserReference();
}
