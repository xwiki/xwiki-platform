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

import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.xwiki.user.UserReference;

import com.xpn.xwiki.util.Util;

/**
 * {@link HttpServletRequest} implementation with XWiki related specific behavior.
 * 
 * @version $Id$
 */
public class XWikiServletRequest extends HttpServletRequestWrapper implements XWikiRequest
{
    public static final String ATTRIBUTE_EFFECTIVE_AUTHOR = XWikiRequest.class.getName() + "#effectiveAuthor";

    public XWikiServletRequest(HttpServletRequest request)
    {
        // Passing null to #XWikiServletRequest(HttpServletRequest) used partially work so keeping it working to be safe
        super(request != null ? request : new XWikiServletRequestStub());
    }

    // XWikiRequest

    @Override
    public String get(String name)
    {
        return getRequest().getParameter(name);
    }

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        return (HttpServletRequest) getRequest();
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return Util.getCookie(cookieName, this);
    }

    // HttpServletRequest

    @Override
    public StringBuffer getRequestURL()
    {
        return getHttpServletRequest().getRequestURL();
    }

    @Override
    public String getParameter(String s)
    {
        // Some servlet containers don't support passing null so we need to handle it.
        return s != null ? getRequest().getParameter(s) : null;
    }

    @Override
    public String[] getParameterValues(String s)
    {
        // Some servlet containers don't support passing null so we need to handle it.
        return s != null ? getRequest().getParameterValues(s) : null;
    }

    @Override
    public String getRemoteAddr()
    {
        HttpServletRequest request = getHttpServletRequest();
        if (request.getHeader("x-forwarded-for") != null) {
            return request.getHeader("x-forwarded-for");
        }

        return request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost()
    {
        HttpServletRequest request = getHttpServletRequest();
        if (request.getHeader("x-forwarded-for") != null) {
            return request.getHeader("x-forwarded-for");
        }

        return request.getRemoteHost();
    }

    @Override
    public Optional<UserReference> getEffectiveAuthor()
    {
        return Optional.ofNullable((UserReference) getAttribute(ATTRIBUTE_EFFECTIVE_AUTHOR));
    }
}
