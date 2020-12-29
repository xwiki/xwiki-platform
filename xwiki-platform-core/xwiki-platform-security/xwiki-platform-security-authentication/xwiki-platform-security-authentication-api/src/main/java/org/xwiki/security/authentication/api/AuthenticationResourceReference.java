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
package org.xwiki.security.authentication.api;

import org.apache.commons.lang3.StringUtils;
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
 * @since 13.0RC1
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

    /**
     * Possible actions for authentication resource reference.
     */
    public enum AuthenticationAction
    {
        /**
         * Action used to reset the password of a user.
         */
        RESET_PASSWORD("reset"),

        /**
         * Action used to retrieve the username of a user.
         */
        FORGOT_USERNAME("forgot");

        private String requestParameter;

        AuthenticationAction(String requestParameter)
        {
            this.requestParameter = requestParameter;
        }

        /**
         * @return the request parameter associated to this action.
         */
        public String getRequestParameter()
        {
            return requestParameter;
        }

        /**
         * Retrieve an action based on the request parameter.
         * @param parameter the parameter from which to retrieve the action.
         * @return an action associated to the given parameter.
         * @throws IllegalArgumentException if the parameter is empty or if no action could be found based on it.
         */
        public static AuthenticationAction getFromRequestParameter(String parameter) throws IllegalArgumentException
        {
            if (StringUtils.isEmpty(parameter)) {
                throw new IllegalArgumentException("The parameter needs to be provided.");
            }
            for (AuthenticationAction value : AuthenticationAction.values()) {
                if (value.requestParameter.equals(parameter.toLowerCase())) {
                    return value;
                }
            }
            throw new IllegalArgumentException(
                String.format("Cannot find an AuthenticationAction for parameter [%s]", parameter));
        }
    }

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
