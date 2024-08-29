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

import java.util.Properties;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration options for the User module.
 *
 * @version $Id$
 * @since 12.2
 */
@Role
public interface UserConfiguration
{
    /**
     * @return the hint of the implementation components used to store users (defaults to {@code document}
     */
    String getStoreHint();

    /**
     * @return the overriding preferences for the superadmin user
     */
    Properties getSuperAdminPreferences();

    /**
     * @return the overriding preferences for the guest user
     */
    Properties getGuestPreference();

    /**
     * When displaying a user in a compact mode, we usually rely only on the user avatar and their full name. If this is
     * not enough to properly identify the user, then this configuration can be used to display additional information.
     * 
     * @return the name of the user property to be used as qualifier (hint) when displaying the user in a compact mode,
     *         or {@code null} if no additional information should be displayed
     * @since 14.10.12
     * @since 15.5RC1
     */
    @Unstable
    default String getUserQualifierProperty()
    {
        return null;
    }
}
