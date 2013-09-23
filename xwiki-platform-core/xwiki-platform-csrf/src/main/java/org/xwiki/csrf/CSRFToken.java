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
package org.xwiki.csrf;

import org.xwiki.component.annotation.Role;

/**
 * Anti-CSRF (Cross Site Request Forgery) protection using secret token validation mechanism.
 * <p>
 * A random secret token should be included into every request that modifies or stores some data. If the token included
 * into the request does not match the token stored on the server side, the request is redirected to a resubmission page
 * where a legitimate user has a chance to confirm his action.
 * 
 * @see <a href="http://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29_Prevention_Cheat_Sheet">CSRF
 *      Prevention Cheat Sheet</a>
 * @version $Id$
 * @since 2.5M2
 */
@Role
public interface CSRFToken
{
    /**
     * Returns the anti-CSRF token associated with the current user. Creates a fresh token on first call.
     * 
     * @return the secret token
     * @see #isTokenValid(String)
     */
    String getToken();

    /**
     * Removes the anti-CSRF token associated with the current user. Current token is invalidated immediately, a
     * subsequent call of {@link #getToken()} will generate a fresh token.
     */
    void clearToken();

    /**
     * Check if the given <code>token</code> matches the internally stored token associated with the current user.
     * 
     * @param token random token from the request
     * @return {@code true} if the component is disabled or the given token is correct, {@code false} otherwise
     */
    boolean isTokenValid(String token);

    /**
     * Get the URL where a failed request should be redirected to.
     * 
     * @return URL of the resubmission page with correct parameters
     */
    String getResubmissionURL();
}
