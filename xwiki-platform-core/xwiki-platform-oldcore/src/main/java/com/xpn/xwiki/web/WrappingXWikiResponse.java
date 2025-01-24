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

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.xwiki.container.Container;

/**
 * A wrapper around {@link XWikiResponse}.
 * 
 * @version $Id$
 * @since 17.0.0RC1
 * @deprecated use the {@link Container} API instead
 */
// TODO: uncomment the annotation when XWiki Standard scripts are fully migrated to the new API
// @Deprecated(since = "17.0.0RC1")
public class WrappingXWikiResponse extends HttpServletResponseWrapper implements XWikiResponse
{
    protected final XWikiResponse response;

    /**
     * @param response the wrapped response
     */
    public WrappingXWikiResponse(XWikiResponse response)
    {
        super(response);

        this.response = response;
    }

    @Override
    public HttpServletResponse getHttpServletResponse()
    {
        return this.response.getHttpServletResponse();
    }

    @Override
    public void removeCookie(String cookieName, XWikiRequest request)
    {
        this.response.removeCookie(cookieName, request);
    }
}
