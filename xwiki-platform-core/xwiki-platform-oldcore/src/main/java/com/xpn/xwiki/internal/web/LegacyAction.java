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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xpn.xwiki.web.XWikiAction;

/**
 * A component role for the old {@link XWikiAction} based entry points.
 * 
 * @version $Id$
 * @since 12.9RC1
 * @deprecated use entity resource reference handlers instead
 */
@Deprecated
public interface LegacyAction
{
    /**
     * @param servletRequest the request passed to the servlet
     * @param servletResponse the response passed to the servlet
     * @throws Exception when the action produces an unexptected error
     */
    void execute(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception;
}
