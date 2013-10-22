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
package org.xwiki.wiki.user;

import org.xwiki.wiki.properties.WikiPropertyGroup;

public class WikiUserPropertyGroup extends WikiPropertyGroup
{
    public static final String ENABLE_LOCAL_USERS_PROPERTY = "enableLocalUsers";

    public static final String MEMBERSHIP_TYPE_PROPERTY = "membershipType";

    /**
     * Constructor.
     *
     * @param id Unique identifier of the group
     */
    public WikiUserPropertyGroup(String id)
    {
        super(id);
    }

    public void enableLocalUsers(boolean enable)
    {
        this.set(ENABLE_LOCAL_USERS_PROPERTY, enable);
    }

    public boolean hasLocalUsersEnabled()
    {
        return ((Boolean) this.get(ENABLE_LOCAL_USERS_PROPERTY)).booleanValue();
    }

    public void setMembershypType(MembershipType type)
    {
        this.set(MEMBERSHIP_TYPE_PROPERTY, type);
    }

    public MembershipType getMembershipType()
    {
        return (MembershipType) this.get(MEMBERSHIP_TYPE_PROPERTY);
    }
}
