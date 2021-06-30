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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.struts2.dispatcher.multipart.MultiPartRequestWrapper;

import com.xpn.xwiki.util.Util;

public class XWikiServletRequest implements XWikiRequest
{
    private HttpServletRequest request;

    public XWikiServletRequest(HttpServletRequest request)
    {
        this.request = request;
    }

    // XWikiRequest

    @Override
    public String get(String name)
    {
        return this.request.getParameter(name);
    }

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        return this.request;
    }

    @Override
    public String getAuthType()
    {
        return this.request.getAuthType();
    }

    @Override
    public Cookie[] getCookies()
    {
        return this.request.getCookies();
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return Util.getCookie(cookieName, this);
    }

    @Override
    public long getDateHeader(String s)
    {
        return this.request.getDateHeader(s);
    }

    @Override
    public String getHeader(String s)
    {
        return this.request.getHeader(s);
    }

    @Override
    public Enumeration getHeaders(String s)
    {
        return this.request.getHeaders(s);
    }

    @Override
    public Enumeration getHeaderNames()
    {
        return this.request.getHeaderNames();
    }

    @Override
    public int getIntHeader(String s)
    {
        return this.request.getIntHeader(s);
    }

    @Override
    public String getMethod()
    {
        return this.request.getMethod();
    }

    @Override
    public String getPathInfo()
    {
        return this.request.getPathInfo();
    }

    @Override
    public String getPathTranslated()
    {
        return this.request.getPathTranslated();
    }

    @Override
    public String getContextPath()
    {
        return this.request.getContextPath();
    }

    @Override
    public String getQueryString()
    {
        return this.request.getQueryString();
    }

    @Override
    public String getRemoteUser()
    {
        return this.request.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String s)
    {
        return this.request.isUserInRole(s);
    }

    @Override
    public Principal getUserPrincipal()
    {
        return this.request.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId()
    {
        return this.request.getRequestedSessionId();
    }

    @Override
    public String getRequestURI()
    {
        return this.request.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL()
    {
        StringBuffer requestURL = this.request.getRequestURL();
        if ((requestURL == null) && (this.request instanceof MultiPartRequestWrapper)) {
            requestURL = ((HttpServletRequest) ((MultiPartRequestWrapper) this.request).getRequest()).getRequestURL();
        }
        return requestURL;
    }

    @Override
    public String getServletPath()
    {
        return this.request.getServletPath();
    }

    @Override
    public HttpSession getSession(boolean b)
    {
        return this.request.getSession(b);
    }

    @Override
    public HttpSession getSession()
    {
        return this.request.getSession();
    }

    @Override
    public boolean isRequestedSessionIdValid()
    {
        return this.request.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie()
    {
        return this.request.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL()
    {
        return this.request.isRequestedSessionIdFromURL();
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public boolean isRequestedSessionIdFromUrl()
    {
        return this.request.isRequestedSessionIdFromURL();
    }

    @Override
    public Object getAttribute(String s)
    {
        return this.request.getAttribute(s);
    }

    @Override
    public Enumeration getAttributeNames()
    {
        return this.request.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding()
    {
        return this.request.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException
    {
        this.request.setCharacterEncoding(s);
    }

    @Override
    public int getContentLength()
    {
        return this.request.getContentLength();
    }

    @Override
    public String getContentType()
    {
        return this.request.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        return this.request.getInputStream();
    }

    @Override
    public String getParameter(String s)
    {
        return this.request.getParameter(s);
    }

    @Override
    public Enumeration getParameterNames()
    {
        return this.request.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String s)
    {
        return this.request.getParameterValues(s);
    }

    @Override
    public Map getParameterMap()
    {
        return this.request.getParameterMap();
    }

    @Override
    public String getProtocol()
    {
        return this.request.getProtocol();
    }

    @Override
    public String getScheme()
    {
        return this.request.getScheme();
    }

    @Override
    public String getServerName()
    {
        return this.request.getServerName();
    }

    @Override
    public int getServerPort()
    {
        return this.request.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        return this.request.getReader();
    }

    @Override
    public String getRemoteAddr()
    {
        if (this.request.getHeader("x-forwarded-for") != null) {
            return this.request.getHeader("x-forwarded-for");
        }
        return this.request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost()
    {
        if (this.request.getHeader("x-forwarded-for") != null) {
            return this.request.getHeader("x-forwarded-for");
        }
        return this.request.getRemoteHost();
    }

    @Override
    public void setAttribute(String s, Object o)
    {
        this.request.setAttribute(s, o);
    }

    @Override
    public void removeAttribute(String s)
    {
        this.request.removeAttribute(s);
    }

    @Override
    public Locale getLocale()
    {
        return this.request.getLocale();
    }

    @Override
    public Enumeration getLocales()
    {
        return this.request.getLocales();
    }

    @Override
    public boolean isSecure()
    {
        return this.request.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s)
    {
        return this.request.getRequestDispatcher(s);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public String getRealPath(String s)
    {
        return this.request.getRealPath(s);
    }

    @Override
    public int getRemotePort()
    {
        return this.request.getRemotePort();
    }

    @Override
    public String getLocalName()
    {
        return this.request.getLocalName();
    }

    @Override
    public String getLocalAddr()
    {
        return this.request.getLocalAddr();
    }

    @Override
    public int getLocalPort()
    {
        return this.request.getLocalPort();
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException
    {
        return this.request.authenticate(httpServletResponse);
    }

    @Override
    public void login(String s, String s1) throws ServletException
    {
        this.request.login(s, s1);
    }

    @Override
    public void logout() throws ServletException
    {
        this.request.logout();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException
    {
        return this.request.getParts();
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException
    {
        return this.request.getPart(s);
    }

    @Override
    public ServletContext getServletContext()
    {
        return this.request.getServletContext();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException
    {
        return this.request.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
        throws IllegalStateException
    {
        return this.request.startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted()
    {
        return this.request.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported()
    {
        return this.request.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext()
    {
        return this.request.getAsyncContext();
    }

    @Override
    public DispatcherType getDispatcherType()
    {
        return this.request.getDispatcherType();
    }
}
