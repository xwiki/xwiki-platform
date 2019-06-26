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

import org.securityfilter.filter.SecurityRequestWrapper;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Manager of the authentication failures strategies.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Unstable
@Role
public interface AuthenticationFailureManager
{
    /**
     * Record that the given username fails to authenticate.
     * @param username the username that fails the authentication. Should be the username typed by the user and not a
     *          computed login.
     * @return true if the authentication failure limits defined by the configuration has been reached.
     */
    boolean recordAuthenticationFailure(String username);

    /**
     * Remove all records of authentication failure for the given user.
     * @param username the username that is used for the authentication. Should be the username typed by the user and
     *                  not a computed login.
     */
    void resetAuthenticationFailureCounter(String username);

    /**
     * If the user reached the authentication failure limit, aggregate form information returned by the different
     * strategies (see {@link AuthenticationFailureStrategy#getForm(String)}). Else return an empty string.
     * @param username the username that is used for the authentication. Should be the username typed by the user and
     *                   not a computed login.
     * @return the aggregated form information to add to the standard login form, or an empty string.
     */
    String getForm(String username);

    /**
     * If the user reached the authentication failure limit, validate the form information against the different
     * strategies used and return the result
     * (see {@link AuthenticationFailureStrategy#validateForm(String, SecurityRequestWrapper)}). Else returns true.
     * @param username the username that is used for the authentication. Should be the username typed by the user and
     *      not a computed login.
     * @param request a wrapping of the request used for the authentication.
     * @return true if all strategies validate the request or if the user didn't reach the limit.
     */
    boolean validateForm(String username, SecurityRequestWrapper request);

    /**
     * If the user reached the authentication failure limit, aggregate the error message of the different strategies
     * (see {@link AuthenticationFailureStrategy#getErrorMessage(String)}). Else return an empty string.
     * @param username the username that is used for the authentication. Should be the username typed by the user and
     *      not a computed login.
     * @return the aggregated error message from the strategies or an empty string.
     */
    String getErrorMessage(String username);
}
