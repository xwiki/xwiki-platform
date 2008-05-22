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
 *
 */
package org.xwiki.container.servlet;

import org.xwiki.container.Request;
import org.xwiki.url.XWikiURL;

import javax.servlet.http.HttpServletRequest;

public class ServletRequest implements Request
{
    private HttpServletRequest httpServletRequest;
    private XWikiURL xwikiURL;

    public ServletRequest(HttpServletRequest httpServletRequest)
    {
        this.httpServletRequest = httpServletRequest;
    }

    public HttpServletRequest getHttpServletRequest()
    {
        return this.httpServletRequest;
    }

    public XWikiURL getURL()
    {
        return this.xwikiURL;
    }

    public void setXWikiURL(XWikiURL url)
    {
        this.xwikiURL = url;
    }

    public Object getProperty(String key)
    {
        return this.httpServletRequest.getAttribute(key);
    }

    public void setProperty(String key, Object value)
    {
        this.httpServletRequest.setAttribute(key, value);
    }

    public void removeProperty(String key)
    {
        this.httpServletRequest.removeAttribute(key);
    }
}
