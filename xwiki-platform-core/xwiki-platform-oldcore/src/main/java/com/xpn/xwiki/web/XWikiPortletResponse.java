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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

import javax.portlet.ActionResponse;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletResponse;
import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class XWikiPortletResponse implements XWikiResponse
{
    private PortletResponse response;

    public XWikiPortletResponse(PortletResponse response)
    {
        this.response = response;
    }

    public PortletResponse getPortletResponse()
    {
        return response;
    }

    @Override
    public String getContentType()
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).getContentType();
        else
            return "";
    }

    @Override
    public PortletURL createRenderURL()
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).createRenderURL();
        return null;
    }

    @Override
    public PortletURL createActionURL()
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).createActionURL();
        return null;
    }

    @Override
    public String getNamespace()
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).getNamespace();
        return "";
    }

    @Override
    public void setTitle(String s)
    {
        if (response instanceof RenderResponse)
            ((RenderResponse) response).setTitle(s);
    }

    @Override
    public void setContentType(String type)
    {
        if (response instanceof RenderResponse)
            ((RenderResponse) response).setContentType(type);
    }

    @Override
    public String getCharacterEncoding()
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).getCharacterEncoding();
        return "";
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).getWriter();
        return null;
    }

    @Override
    public void setCharacterEncoding(String s)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setCharacterEncoding(s);
    }

    @Override
    public Locale getLocale()
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).getLocale();
        return null;
    }

    @Override
    public void setBufferSize(int i)
    {
        if (response instanceof RenderResponse)
            ((RenderResponse) response).setBufferSize(i);
    }

    @Override
    public int getBufferSize()
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).getBufferSize();
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException
    {
        if (response instanceof RenderResponse)
            ((RenderResponse) response).flushBuffer();
    }

    @Override
    public void resetBuffer()
    {
        if (response instanceof RenderResponse)
            ((RenderResponse) response).resetBuffer();
    }

    @Override
    public boolean isCommitted()
    {
        if (response instanceof RenderResponse)
            ((RenderResponse) response).isCommitted();
        return false;
    }

    @Override
    public void reset()
    {
        if (response instanceof RenderResponse)
            ((RenderResponse) response).reset();
    }

    @Override
    public OutputStream getPortletOutputStream() throws IOException
    {
        if (response instanceof RenderResponse)
            return ((RenderResponse) response).getPortletOutputStream();
        return null;
    }

    @Override
    public void addProperty(String s, String s1)
    {
        response.addProperty(s, s1);
    }

    @Override
    public void setProperty(String s, String s1)
    {
        response.setProperty(s, s1);
    }

    @Override
    public String encodeURL(String s)
    {
        return response.encodeURL(s);
    }

    @Override
    public void setWindowState(WindowState windowState) throws WindowStateException
    {
        if (response instanceof ActionResponse)
            ((ActionResponse) response).setWindowState(windowState);
    }

    @Override
    public void setPortletMode(PortletMode portletMode) throws PortletModeException
    {
        if (response instanceof ActionResponse)
            ((ActionResponse) response).setPortletMode(portletMode);
    }

    @Override
    public void setRenderParameters(Map map)
    {
        if (response instanceof ActionResponse)
            ((ActionResponse) response).setRenderParameters(map);
    }

    @Override
    public void setRenderParameter(String s, String s1)
    {
        if (response instanceof ActionResponse)
            ((ActionResponse) response).setRenderParameter(s, s1);
    }

    @Override
    public void setRenderParameter(String s, String[] strings)
    {
        if (response instanceof ActionResponse)
            ((ActionResponse) response).setRenderParameter(s, strings);
    }

    // Servlet functions

    @Override
    public HttpServletResponse getHttpServletResponse()
    {
        try {
            return (HttpServletResponse) getPortletResponse();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().getOutputStream();
        return null;
    }

    @Override
    public String encodeRedirectURL(String s)
    {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().encodeRedirectURL(s);
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public String encodeUrl(String s)
    {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().encodeUrl(s);
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public String encodeRedirectUrl(String s)
    {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().encodeRedirectUrl(s);
        return null;
    }

    @Override
    public void sendError(int i, String s) throws IOException
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().sendError(i, s);
    }

    @Override
    public void sendError(int i) throws IOException
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().sendError(i);
    }

    @Override
    public void sendRedirect(String redirect) throws IOException
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().sendRedirect(redirect);
    }

    @Override
    public void addCookie(Cookie cookie)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addCookie(cookie);
    }

    /**
     * Remove a cookie.
     * 
     * @param request The servlet request needed to find the cookie to remove
     * @param cookieName The name of the cookie that must be removed.
     */
    @Override
    public void removeCookie(String cookieName, XWikiRequest request)
    {
        if (response instanceof HttpServletResponse) {
            Cookie cookie = request.getCookie(cookieName);
            if (cookie != null) {
                cookie.setMaxAge(0);
                cookie.setPath(cookie.getPath());
                getHttpServletResponse().addCookie(cookie);
            }
        }
    }

    @Override
    public void setContentLength(int length)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setContentLength(length);
    }

    @Override
    public void setDateHeader(String name, long value)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setDateHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setIntHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addHeader(name, value);
    }

    @Override
    public void addDateHeader(String name, long value)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addDateHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().addIntHeader(name, value);
    }

    @Override
    public void setStatus(int i)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setStatus(i);
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public void setStatus(int i, String s)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setStatus(i, s);
    }

    @Override
    public void setLocale(Locale locale)
    {
        if (response instanceof HttpServletResponse)
            getHttpServletResponse().setLocale(locale);
    }

    @Override
    public boolean containsHeader(String name)
    {
        if (response instanceof HttpServletResponse)
            return getHttpServletResponse().containsHeader(name);
        return false;
    }
}
