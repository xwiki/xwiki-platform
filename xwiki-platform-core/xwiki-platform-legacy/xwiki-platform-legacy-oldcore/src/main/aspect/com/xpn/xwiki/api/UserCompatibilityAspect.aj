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
package com.xpn.xwiki.api;

import java.text.MessageFormat;

/**
 * Add a backward compatibility layer to the {@link User} class.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
public privileged aspect UserCompatibilityAspect
{
    /**
     * Check if the user belongs to a group or not. This method only check direct membership (no recursive checking) in
     * the current wiki.
     *
     * @param groupName The group to check.
     * @return {@code true} if the user does belong to the specified group, false otherwise or if an exception occurs.
     * @deprecated use the org.xwiki.user.group.GroupManager component instead (or its {@code user.group} script
     *             service). Unlike this method, it takes a group reference (and is thus not restricted to the current
     *             wiki) and it can optionally take sub-groups into account
     */
    @Deprecated(since = "18.7.0RC1")
    public boolean User.isUserInGroup(String groupName)
    {
        boolean result = false;
        try {
            if (this.user == null) {
                User.LOGGER.warn("User considered not part of group [{}] since user is null", groupName);
            } else {
                result = this.user.isUserInGroup(groupName, getXWikiContext());
            }
        } catch (Exception ex) {
            User.LOGGER.warn(new MessageFormat("Unhandled exception while checking if user {0}"
                + " belongs to group {1}").format(new java.lang.Object[] { this.user, groupName }), ex);
        }
        return result;
    }
}
