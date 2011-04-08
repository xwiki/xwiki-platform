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

package com.xpn.xwiki.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletURL;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class XWikiServletResponse implements XWikiResponse
{
    private HttpServletResponse response;

    public XWikiServletResponse(HttpServletResponse response)
    {
        this.response = response;
    }

    public HttpServletResponse getHttpServletResponse()
    {
        return this.response;
    }

    public void sendRedirect(String redirect) throws IOException
    {
        this.response.sendRedirect(redirect);
    }

    public void setContentType(String type)
    {
        this.response.setContentType(type);
    }

    public void setBufferSize(int i)
    {
        this.response.setBufferSize(i);
    }

    public int getBufferSize()
    {
        return this.response.getBufferSize();
    }

    public void flushBuffer() throws IOException
    {
        this.response.flushBuffer();
    }

    public void resetBuffer()
    {
        this.response.resetBuffer();
    }

    public boolean isCommitted()
    {
        return this.response.isCommitted();
    }

    public void reset()
    {
        this.response.reset();
    }

    public void setContentLength(int length)
    {
        this.response.setContentLength(length);
    }

    public String getCharacterEncoding()
    {
        return this.response.getCharacterEncoding();
    }

    public ServletOutputStream getOutputStream() throws IOException
    {
        return this.response.getOutputStream();
    }

    public PrintWriter getWriter() throws IOException
    {
        return this.response.getWriter();
    }

    public void setCharacterEncoding(String s)
    {
        this.response.setCharacterEncoding(s);
    }

    public void addCookie(Cookie cookie)
    {
        this.response.addCookie(cookie);
    }

    public void addCookie(String cookieName, String cookieValue, int age)
    {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setVersion(1);
        cookie.setMaxAge(age);
        this.response.addCookie(cookie);
    }

    /**
     * Remove a cookie.
     * 
     * @param request The servlet request needed to find the cookie to remove
     * @param cookieName The name of the cookie that must be removed.
     */
    public void removeCookie(String cookieName, XWikiRequest request)
    {
        Cookie cookie = request.getCookie(cookieName);
        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setPath(cookie.getPath());
            addCookie(cookie);
        }
    }

    public void setLocale(Locale locale)
    {
        this.response.setLocale(locale);
    }

    public Locale getLocale()
    {
        return this.response.getLocale();
    }

    public void setDateHeader(String name, long value)
    {
        this.response.setDateHeader(name, value);
    }

    public void setIntHeader(String name, int value)
    {
        this.response.setIntHeader(name, value);
    }

    public void setHeader(String name, String value)
    {
        this.response.setHeader(name, value);
    }

    public void addHeader(String name, String value)
    {
        this.response.addHeader(name, value);
    }

    public void addDateHeader(String name, long value)
    {
        this.response.addDateHeader(name, value);
    }

    public void addIntHeader(String name, int value)
    {
        this.response.addIntHeader(name, value);
    }

    public void setStatus(int i)
    {
        this.response.setStatus(i);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setStatus(int i, String s)
    {
        this.response.setStatus(i, s);
    }

    public boolean containsHeader(String name)
    {
        return this.response.containsHeader(name);
    }

    public String encodeURL(String s)
    {
        return this.response.encodeURL(s);
    }

    public String encodeRedirectURL(String s)
    {
        return this.response.encodeRedirectURL(s);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String encodeUrl(String s)
    {
        return this.response.encodeUrl(s);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String encodeRedirectUrl(String s)
    {
        return this.response.encodeRedirectUrl(s);
    }

    public void sendError(int i, String s) throws IOException
    {
        this.response.sendError(i, s);
    }

    public void sendError(int i) throws IOException
    {
        this.response.sendError(i);
    }

    /*
     * Portlet Functions
     */
    public void addProperty(String s, String s1)
    {
    }

    public void setProperty(String s, String s1)
    {
    }

    public String getContentType()
    {
        return null;
    }

    public OutputStream getPortletOutputStream() throws IOException
    {
        return null;
    }

    public PortletURL createRenderURL()
    {
        return null;
    }

    public PortletURL createActionURL()
    {
        return null;
    }

    public String getNamespace()
    {
        return null;
    }

    public void setTitle(String s)
    {
    }

    public void setWindowState(WindowState windowState) throws WindowStateException
    {
    }

    public void setPortletMode(PortletMode portletMode) throws PortletModeException
    {
    }

    public void setRenderParameters(Map map)
    {
    }

    public void setRenderParameter(String s, String s1)
    {
    }

    public void setRenderParameter(String s, String[] strings)
    {
    }

}
