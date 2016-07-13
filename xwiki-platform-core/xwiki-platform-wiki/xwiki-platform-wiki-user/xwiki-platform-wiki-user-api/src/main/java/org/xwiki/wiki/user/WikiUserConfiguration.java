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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Configuration about the user management in a wiki.
 *
 * @since 5.3M2
 * @version $Id$
 */
public class WikiUserConfiguration
{
    private UserScope userScope;

    private MembershipType membershipType;

    /**
     * Constructor.
     */
    public WikiUserConfiguration()
    {
        // Default values
        setUserScope(UserScope.GLOBAL_ONLY);
        setMembershipType(MembershipType.INVITE);
    }

    /**
     * @return the user scope
     */
    public UserScope getUserScope()
    {
        return userScope;
    }

    /**
     * @param scope the scope to set
     */
    public void setUserScope(UserScope scope)
    {
        this.userScope = scope;
    }

    /**
     * @param type membership type to set
     */
    public void setMembershipType(MembershipType type)
    {
        this.membershipType = type;
    }

    /**
     * @return the membership type of the wiki
     */
    public MembershipType getMembershipType()
    {
        return membershipType;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof WikiUserConfiguration)) {
            return false;
        }

        WikiUserConfiguration otherConfig = (WikiUserConfiguration) o;
        return new EqualsBuilder().append(this.membershipType, otherConfig.membershipType)
                .append(this.userScope, otherConfig.userScope).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(this.membershipType).append(this.userScope).toHashCode();
    }
}
