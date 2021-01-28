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
package org.xwiki.security.authentication;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.stability.Unstable;

/**
 * A dedicated {@link org.xwiki.resource.ResourceReference} to perform authentication related actions.
 * See {@link AuthenticationAction} for the possible actions.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Unstable
public class AuthenticationResourceReference extends AbstractResourceReference
{
    /**
     * Identifier of the resource type also used for associated components.
     */
    public static final String RESOURCE_TYPE_ID = "authenticate";

    /**
     * Dedicated resource type used for authentication.
     */
    public static final ResourceType TYPE = new ResourceType(RESOURCE_TYPE_ID);

    private AuthenticationAction action;

    /**
     * Default constructor.
     * @param action the action of the reference.
     */
    public AuthenticationResourceReference(AuthenticationAction action)
    {
        setType(TYPE);
        this.action = action;
    }

    /**
     * @return the action associated to this resource reference.
     */
    public AuthenticationAction getAction()
    {
        return action;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AuthenticationResourceReference that = (AuthenticationResourceReference) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(action, that.action)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(action).toHashCode();
    }
}
