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
package org.xwiki.export.pdf.internal.browser;

import java.net.URL;
import java.util.List;

import javax.servlet.http.Cookie;

import org.xwiki.component.annotation.Role;

/**
 * Filter the cookies passed to the web browser used for PDF printing.
 * 
 * @version $Id$
 * @since 14.10
 */
@Role
public interface CookieFilter
{
    /**
     * The context information needed by the cookies filters.
     * 
     * @version $Id$
     */
    interface CookieFilterContext
    {
        /**
         * @return the IP address of the web browser that is going to use the cookies
         */
        String getBrowserIPAddress();

        /**
         * @return the URL that is going to be accessed by the web browser
         */
        URL getTargetURL();
    }

    /**
     * Adds, removes or modifies cookies before they are passed to the web browser used for PDF printing.
     * 
     * @param cookies the cookies to filter
     * @param cookieFilterContext provides contextual information for cookie filtering
     */
    void filter(List<Cookie> cookies, CookieFilterContext cookieFilterContext);

    /**
     * @return {@code true} if at least one of the cookies from the current HTTP request needs to be filtered,
     *         {@code false} otherwise
     */
    boolean isFilterRequired();
}
