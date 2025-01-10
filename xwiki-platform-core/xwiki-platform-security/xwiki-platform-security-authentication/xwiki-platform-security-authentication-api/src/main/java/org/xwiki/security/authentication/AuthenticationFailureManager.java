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

import jakarta.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Role;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Manager of the authentication failures strategies.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Role
public interface AuthenticationFailureManager
{
    /**
     * Record that the given username fails to authenticate.
     * 
     * @param username the username that fails the authentication. Should be the username typed by the user and not a
     *            computed login.
     * @param request a wrapping of the request used for the authentication.
     * @return true if the authentication failure limits defined by the configuration has been reached.
     * @deprecated use {@link #recordAuthenticationFailure(String, HttpServletRequest)}
     */
    @Deprecated(since = "42.0.0")
    default boolean recordAuthenticationFailure(String username, javax.servlet.http.HttpServletRequest request)
    {
        return recordAuthenticationFailure(username, JakartaServletBridge.toJakarta(request));
    }

    /**
     * Record that the given username fails to authenticate.
     * 
     * @param username the username that fails the authentication. Should be the username typed by the user and not a
     *            computed login.
     * @param request a wrapping of the request used for the authentication.
     * @return true if the authentication failure limits defined by the configuration has been reached.
     * @since 42.0.0
     */
    @Unstable
    default boolean recordAuthenticationFailure(String username, HttpServletRequest request)
    {
        return recordAuthenticationFailure(username, JakartaServletBridge.toJavax(request));
    }

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
     * @param request a wrapping of the request used for the authentication.
     * @return the aggregated form information to add to the standard login form, or an empty string.
     * @deprecated use {@link #getForm(String, HttpServletRequest)} instead
     */
    @Deprecated(since = "42.0.0")
    default String getForm(String username, javax.servlet.http.HttpServletRequest request)
    {
        return getForm(username, JakartaServletBridge.toJakarta(request));
    }

    /**
     * If the user reached the authentication failure limit, aggregate form information returned by the different
     * strategies (see {@link AuthenticationFailureStrategy#getForm(String)}). Else return an empty string.
     * @param username the username that is used for the authentication. Should be the username typed by the user and
     *                   not a computed login.
     * @param request a wrapping of the request used for the authentication.
     * @return the aggregated form information to add to the standard login form, or an empty string.
     * @since 42.0.0
     */
    @Unstable
    default String getForm(String username, HttpServletRequest request)
    {
        return getForm(username, JakartaServletBridge.toJavax(request));        
    }

    /**
     * If the user reached the authentication failure limit, validate the form information against the different
     * strategies used and return the result (see
     * {@link AuthenticationFailureStrategy#validateForm(String, HttpServletRequest)}). Else returns true.
     * 
     * @param username the username that is used for the authentication. Should be the username typed by the user and
     *            not a computed login.
     * @param request a wrapping of the request used for the authentication.
     * @return true if all strategies validate the request or if the user didn't reach the limit.
     * @deprecated use {@link #validateForm(String, HttpServletRequest)} instead
     */
    @Deprecated(since = "42.0.0")
    default boolean validateForm(String username, javax.servlet.http.HttpServletRequest request)
    {
        return validateForm(username, JakartaServletBridge.toJakarta(request));
    }

    /**
     * If the user reached the authentication failure limit, validate the form information against the different
     * strategies used and return the result (see
     * {@link AuthenticationFailureStrategy#validateForm(String, HttpServletRequest)}). Else returns true.
     * 
     * @param username the username that is used for the authentication. Should be the username typed by the user and
     *            not a computed login.
     * @param request a wrapping of the request used for the authentication.
     * @return true if all strategies validate the request or if the user didn't reach the limit.
     * @since 42.0.0
     */
    @Unstable
    default boolean validateForm(String username, HttpServletRequest request)
    {
        return validateForm(username, JakartaServletBridge.toJavax(request));
    }

    /**
     * If the user reached the authentication failure limit, aggregate the error message of the different strategies
     * (see {@link AuthenticationFailureStrategy#getErrorMessage(String)}). Else return an empty string.
     * @param username the username that is used for the authentication. Should be the username typed by the user and
     *      not a computed login.
     * @return the aggregated error message from the strategies or an empty string.
     */
    String getErrorMessage(String username);

    /**
     * Find a user document reference based on the given username.
     * @param username the username from which to query the user document reference.
     * @return a document reference corresponding to the username or null if it doesn't exist.
     */
    DocumentReference findUser(String username);

    /**
     * Remove all records of authentication failure for the given user.
     * @param user the document reference of a user.
     * @since 11.7RC1
     */
    default void resetAuthenticationFailureCounter(DocumentReference user)
    {
    }
}
