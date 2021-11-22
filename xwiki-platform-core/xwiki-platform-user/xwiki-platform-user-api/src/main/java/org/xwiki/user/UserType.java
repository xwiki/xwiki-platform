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

/**
 * The type of the user (simple user, advanced user).
 *
 * @see <a href="https://bit.ly/37TUlCp">user profile</a>
 * @version $Id$
 * @since 12.2
 */
public enum UserType
{
    /**
     * Simple user (hides complex actions in the UI for simplicity).
     */
    SIMPLE,

    /**
     * Advanced user (sees all possible actions in the UI).
     */
    ADVANCED;

    /**
     * @param typeAsString the user type represented as a string ("Simple", "Advanced"). The case is ignored.
     * @return the {@link UserType} object matching the passed string representation. All values different than
     *         {@code advanced} are considered to represent a simple user
     */
    public static UserType fromString(String typeAsString)
    {
        UserType result;
        if ("advanced".equalsIgnoreCase(typeAsString)) {
            result = ADVANCED;
        } else {
            result = SIMPLE;
        }
        return result;
    }
}
