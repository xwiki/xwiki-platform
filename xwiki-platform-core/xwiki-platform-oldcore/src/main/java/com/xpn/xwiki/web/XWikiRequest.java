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
package com.xpn.xwiki.web;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.xwiki.stability.Unstable;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;

public interface XWikiRequest extends HttpServletRequest
{
    String get(String name);

    HttpServletRequest getHttpServletRequest();

    Cookie getCookie(String cookieName);

    /**
     * @return the user that holds the responsibility, in terms of access rights, for the submitted data and the changes
     *         triggered by this request. By default the effective author is the user that gets authenticated with the
     *         information provided by this request. If the request doesn't indicate an effective author and no user is
     *         authenticated then the {@link GuestUserReference} is returned.
     * @since 15.10.11
     * @since 16.4.1
     * @since 16.5.0RC1
     */
    @Unstable
    default UserReference getEffectiveAuthor()
    {
        return GuestUserReference.INSTANCE;
    }
}
