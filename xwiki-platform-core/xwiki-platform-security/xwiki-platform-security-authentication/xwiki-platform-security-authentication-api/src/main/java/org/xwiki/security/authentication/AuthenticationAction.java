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

import org.apache.commons.lang3.StringUtils;

/**
 * Possible actions for {@link AuthenticationResourceReference}.
 *
 * @version $Id$
 * @since 13.1RC1
 */
public enum AuthenticationAction
{
    /**
     * Action used to reset the password of a user.
     */
    RESET_PASSWORD("resetpassword"),

    /**
     * Action used to retrieve the username of a user.
     */
    RETRIEVE_USERNAME("retrieveusername");

    private final String requestParameter;

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
