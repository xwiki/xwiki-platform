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
package org.xwiki.container.servlet;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Role
public interface ServletContainerInitializer
{
    @Deprecated(since = "17-jakarta")
    void initializeRequest(javax.servlet.http.HttpServletRequest request, Object xwikiContext)
        throws ServletContainerException;

    @Deprecated(since = "17-jakarta")
    void initializeRequest(javax.servlet.http.HttpServletRequest request) throws ServletContainerException;

    @Deprecated(since = "17-jakarta")
    void initializeResponse(javax.servlet.http.HttpServletResponse response);

    @Deprecated(since = "17-jakarta")
    void initializeSession(javax.servlet.http.HttpServletRequest request);

    /**
     * @deprecated use the notion of Environment instead
     */
    @Deprecated(since = "3.5M1")
    void initializeApplicationContext(javax.servlet.ServletContext servletContext);

    /**
     * @param request the current request
     * @param response the current response
     * @since 17-jakarta
     */
    @Unstable
    void initializeRequest(HttpServletRequest request, HttpServletResponse response) throws ServletContainerException;
}
