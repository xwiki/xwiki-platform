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

/**
 * Property groups attached to wiki descriptors to store properties about the user management in a wiki.
 */
public class WikiUserPropertyGroup extends WikiPropertyGroup
{
    /**
     * Name of the "enableLocalUsers" property.
     */
    public static final String ENABLE_LOCAL_USERS_PROPERTY = "enableLocalUsers";

    /**
     * Name of the "membershipType" property.
     */
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

    /**
     * Helper to enable or disable local users in the wiki that hold this property group.
     * @param enable whether or not the local users should be enabled
     */
    public void enableLocalUsers(boolean enable)
    {
        this.set(ENABLE_LOCAL_USERS_PROPERTY, enable);
    }

    /**
     * Helper to know if local users are supported in the wiki that hold this property group.
     * @return whether or not the local users are enabled
     */
    public boolean hasLocalUsersEnabled()
    {
        return ((Boolean) this.get(ENABLE_LOCAL_USERS_PROPERTY)).booleanValue();
    }

    /**
     * Helper to set the membership type of the wiki that hold this property group.
     * @param type membership type to set
     */
    public void setMembershypType(MembershipType type)
    {
        this.set(MEMBERSHIP_TYPE_PROPERTY, type);
    }

    /**
     * Helper to get the membership type of the wiki that hold this property group.
     * @return the membership type of the wiki
     */
    public MembershipType getMembershipType()
    {
        return (MembershipType) this.get(MEMBERSHIP_TYPE_PROPERTY);
    }
}
