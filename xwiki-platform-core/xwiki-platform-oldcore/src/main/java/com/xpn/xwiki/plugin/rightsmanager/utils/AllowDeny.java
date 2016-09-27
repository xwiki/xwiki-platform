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
package com.xpn.xwiki.plugin.rightsmanager.utils;

/**
 * Contains list of users and groups for allow right and deny right.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class AllowDeny
{
    /**
     * List of users and groups for allow right.
     */
    public UsersGroups allow = new UsersGroups();

    /**
     * List of users and group for deny right.
     */
    public UsersGroups deny = new UsersGroups();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        String allowString = this.allow.toString();
        if (allowString.length() > 0) {
            sb.append('{');
            sb.append("allow : ");
            sb.append(this.allow);
            sb.append('}');
        }

        String denyString = this.deny.toString();
        if (denyString.length() > 0) {
            sb.append('{');
            sb.append("deny : ");
            sb.append(this.deny);
            sb.append('}');
        }

        return sb.toString();
    }
}
