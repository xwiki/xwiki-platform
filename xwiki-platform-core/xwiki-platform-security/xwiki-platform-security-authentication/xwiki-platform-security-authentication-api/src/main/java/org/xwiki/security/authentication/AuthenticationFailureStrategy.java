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

import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Role;

/**
 * Describes a strategy to perform in case the limit of authentication failures is reached.
 * See {@link AuthenticationConfiguration} for a definition of this limit.
 * Note that this strategy might be called even for users before reaching the authentication limit, this is done in
 * order to prevent the following scenario:
 * <ul>
 *     <li>an attacker tries to login x times with a login</li>
 *     <li>a CAPTCHA is displayed in form to mitigate the attack</li>
 *     <li>the attacker tries to perform the attack on a different login</li>
 * </ul>
 * In this case if the CAPTCHA is displayed, we should use it also to prevent attacking the other user.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Role
public interface AuthenticationFailureStrategy
{
    /**
     * @param username the username used for the authentication failure.
     * @return an error message to be displayed in the login form.
     */
    String getErrorMessage(String username);

    /**
     * @param username the username used for the authentication failure.
     * @return some additional form field to be processed for validating the authentication.
     */
    String getForm(String username);

    /**
     * @param username the username used for the authentication failure.
     * @param request the authentication request.
     * @return true if the authentication request can be validated, i.e. if the user should be authorized to login.
     */
    boolean validateForm(String username, HttpServletRequest request);

    /**
     * Notify the strategy about an authentication failure limit reached.
     * This method should be used to perform operation that does not need login form interaction.
     * @param username the username used for the authentication failure.
     */
    void notify(String username);
}
