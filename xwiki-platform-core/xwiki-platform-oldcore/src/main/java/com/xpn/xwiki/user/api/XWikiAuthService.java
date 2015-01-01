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
package com.xpn.xwiki.user.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.security.Principal;

public interface XWikiAuthService
{
    /**
     * Check whether a user is authenticated.
     *
     * @param context the XWiki context object
     * @return a {@link XWikiUser} object representing the user or {@code null} if the user is not
     *         authenticated
     * @throws XWikiException in case of authentication error
     */
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException;

    /**
     * Check whether a user is authenticated.
     *
     * <p>Implementations may ignore username and password and just forward calls to
     * {@link #checkAuth(XWikiContext)}, e.g. for Single sign-on (SSO) systems.
     *
     * @param username the user name for the user to authenticate
     * @param password the password of the user to authenticate
     * @param context the XWiki context object
     * @return a {@link XWikiUser} object representing the user or {@code null} if the user is not
     *         authenticated
     * @throws XWikiException in case of authentication error
     * @deprecated
     */
    @Deprecated
    public XWikiUser checkAuth(String username, String password, String rememberme,
        XWikiContext context) throws XWikiException;

    /**
     * Prints the login screen or redirects there.
     *
     * <p>This method is called when the user is not authenticated and XWiki determines that the
     * user has to be to perform an action, e.g. by checking the "authenticate_edit" preference.
     *
     * @param context the XWiki context object
     * @throws XWikiException in case of authentication error
     */
    public void showLogin(XWikiContext context) throws XWikiException;

    /**
     * Authenticates a user.
     *
     * @param username the user name for the user to authenticate
     * @param password the password of the user to authenticate
     * @param context the XWiki context object
     * @return a {@link Principal} object representing the user or {@code null} if authentication
     *         failed.
     * @throws XWikiException in case of authentication error
     */
    public Principal authenticate(String username, String password, XWikiContext context)
        throws XWikiException;
}
