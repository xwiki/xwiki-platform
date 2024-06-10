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
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.xwiki.user.UserReference;

/**
 * A wrapper around {@link XWikiRequest}.
 * 
 * @version $Id$
 * @since 12.4RC1
 * @since 11.10.5
 */
public class WrappingXWikiRequest implements XWikiRequest
{
    protected final XWikiRequest request;

    /**
     * @param request the wrapped request
     */
    public WrappingXWikiRequest(XWikiRequest request)
    {
        this.request = request;
    }

    @Override
    public ServletContext getServletContext()
    {
        return this.request.getServletContext();
    }

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        return this.request.getHttpServletRequest();
    }

    @Override
    public HttpSession getSession()
    {
        return this.request.getSession();
    }

    @Override
    public HttpSession getSession(boolean create)
    {
        return this.request.getSession(create);
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
    public long getDateHeader(String name)
    {
        return this.request.getDateHeader(name);
    }

    @Override
    public String getHeader(String name)
    {
        return this.request.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name)
    {
        return this.request.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        return this.request.getHeaderNames();
    }

    @Override
    public int getIntHeader(String name)
    {
        return this.request.getIntHeader(name);
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
    public boolean isUserInRole(String role)
    {
        return this.request.isUserInRole(role);
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
        return this.request.getRequestURL();
    }

    @Override
    public String getServletPath()
    {
        return this.request.getServletPath();
    }

    @Override
    public String changeSessionId()
    {
        return this.request.changeSessionId();
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

    @Override
    public boolean isRequestedSessionIdFromUrl()
    {
        return this.request.isRequestedSessionIdFromUrl();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException
    {
        return this.request.authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException
    {
        this.request.login(username, password);
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
    public Part getPart(String name) throws IOException, ServletException
    {
        return this.request.getPart(name);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException
    {
        return this.request.upgrade(handlerClass);
    }

    @Override
    public Object getAttribute(String name)
    {
        return this.request.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return this.request.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding()
    {
        return this.request.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException
    {
        this.request.setCharacterEncoding(env);
    }

    @Override
    public int getContentLength()
    {
        return this.request.getContentLength();
    }

    @Override
    public long getContentLengthLong()
    {
        return this.request.getContentLengthLong();
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
    public String getParameter(String name)
    {
        return this.request.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return this.request.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return this.request.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap()
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
        return this.request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost()
    {
        return this.request.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object o)
    {
        this.request.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name)
    {
        this.request.removeAttribute(name);
    }

    @Override
    public Locale getLocale()
    {
        return this.request.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales()
    {
        return this.request.getLocales();
    }

    @Override
    public boolean isSecure()
    {
        return this.request.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path)
    {
        return this.request.getRequestDispatcher(path);
    }

    @Override
    public String getRealPath(String path)
    {
        return this.request.getRealPath(path);
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

    @Override
    public UserReference getEffectiveAuthor()
    {
        return this.request.getEffectiveAuthor();
    }
}
