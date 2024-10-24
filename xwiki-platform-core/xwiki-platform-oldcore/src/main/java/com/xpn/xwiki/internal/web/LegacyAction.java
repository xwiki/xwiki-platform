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
package com.xpn.xwiki.internal.web;

import org.xwiki.component.annotation.Role;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;

import com.xpn.xwiki.web.XWikiAction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A component role for the old {@link XWikiAction} based entry points.
 * 
 * @version $Id$
 * @since 13.0
 * @deprecated use entity resource reference handlers instead
 */
@Deprecated
@Role
public interface LegacyAction
{
    /**
     * @param servletRequest the request passed to the servlet
     * @param servletResponse the response passed to the servlet
     * @throws Exception when the action produces an unexptected error
     * @deprecated use {@link #execute(HttpServletRequest, HttpServletResponse)} instead
     */
    @Deprecated(since = "42.0.0")
    default void execute(javax.servlet.http.HttpServletRequest servletRequest,
        javax.servlet.http.HttpServletResponse servletResponse) throws Exception
    {
        execute(JakartaServletBridge.toJakarta(servletRequest), JakartaServletBridge.toJakarta(servletResponse));
    }

    /**
     * @param servletRequest the request passed to the servlet
     * @param servletResponse the response passed to the servlet
     * @throws Exception when the action produces an unexptected error
     * @since 42.0.0
     */
    default void execute(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception
    {
        execute(JakartaServletBridge.toJavax(servletRequest), JakartaServletBridge.toJavax(servletResponse));
    }
}
