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
package org.xwiki.user.resource.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.user.UserReference;

/**
 * Represents a User resource (a.k.a. URI), i.e. a URI to the a user profile page.
 *
 * @version $Id$
 * @since 12.2RC1
 */
public class UserResourceReference extends AbstractResourceReference
{
    /**
     * Represents a User Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType("user");

    private UserReference userReference;

    /**
     * @param userReference the user reference pointed to
     */
    public UserResourceReference(UserReference userReference)
    {
        setType(TYPE);
        this.userReference = userReference;
    }

    /**
     * @return the user reference pointed to
     */
    public UserReference getUserReference()
    {
        return this.userReference;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(5, 5)
            .append(getUserReference())
            .append(getType())
            .append(getParameters())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        UserResourceReference rhs = (UserResourceReference) object;
        return new EqualsBuilder()
            .append(getUserReference(), rhs.getUserReference())
            .append(getType(), rhs.getType())
            .append(getParameters(), rhs.getParameters())
            .isEquals();
    }
}
