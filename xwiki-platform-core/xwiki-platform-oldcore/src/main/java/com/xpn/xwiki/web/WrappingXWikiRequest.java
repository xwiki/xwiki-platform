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
import javax.servlet.http.HttpServletRequestWrapper;

import org.xwiki.container.Container;

/**
 * A wrapper around {@link XWikiRequest}.
 * 
 * @version $Id$
 * @since 12.4RC1
 * @since 11.10.5
 * @deprecated use the {@link Container} API instead
 */
//TODO: uncomment the annotation when XWiki Standard scripts are fully migrated to the new API
//@Deprecated(since = "42.0.0")
public class WrappingXWikiRequest extends HttpServletRequestWrapper implements XWikiRequest
{
    protected final XWikiRequest request;

    /**
     * @param request the wrapped request
     */
    public WrappingXWikiRequest(XWikiRequest request)
    {
        super(request);

        this.request = request;
    }

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        return this.request.getHttpServletRequest();
    }

    @Override
    public String get(String name)
    {
        return this.request.get(name);
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return this.request.getCookie(cookieName);
    }
}
