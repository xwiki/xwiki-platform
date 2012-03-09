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
package com.xpn.xwiki.plugin.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletSession;
import javax.portlet.WindowState;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.xpn.xwiki.web.XWikiRequest;

/**
 * This stub is intended to simulate a servlet request in a daemon context, in order to be able to create a custom XWiki
 * context. This trick is used in the Scheduler plugin to give the job execution thread access to the XWiki api.
 * 
 * @version $Id$
 */
public class XWikiServletRequestStub implements XWikiRequest
{
    /** The scheme used by the runtime instance. This is required for creating URLs from scheduled jobs. */
    private String scheme;

    private String host;

    /**
     * The context path used by the runtime instance. This can be useful if a URL factory is created from within a
     * scheduler job.
     */
    private String contextPath;

    public XWikiServletRequestStub()
    {
        this.host = "";
    }

    public void setContextPath(String contextPath)
    {
        this.contextPath = contextPath;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    @Override
    public String getHeader(String s)
    {
        if (s.equals("x-forwarded-host")) {
            return this.host;
        }
        return "";
    }

    @Override
    public String get(String name)
    {
        return "";
    }

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        return null;
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return null;
    }

    @Override
    public boolean isWindowStateAllowed(WindowState windowState)
    {
        return false;
    }

    @Override
    public boolean isPortletModeAllowed(PortletMode portletMode)
    {
        return false;
    }

    @Override
    public PortletMode getPortletMode()
    {
        return null;
    }

    @Override
    public WindowState getWindowState()
    {
        return null;
    }

    @Override
    public PortletPreferences getPreferences()
    {
        return null;
    }

    @Override
    public PortletSession getPortletSession()
    {
        return null;
    }

    @Override
    public PortletSession getPortletSession(boolean b)
    {
        return null;
    }

    @Override
    public String getProperty(String s)
    {
        return null;
    }

    @Override
    public Enumeration getProperties(String s)
    {
        return null;
    }

    @Override
    public Enumeration getPropertyNames()
    {
        return null;
    }

    @Override
    public PortalContext getPortalContext()
    {
        return null;
    }

    @Override
    public String getAuthType()
    {
        return "";
    }

    @Override
    public Cookie[] getCookies()
    {
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String s)
    {
        return 0;
    }

    @Override
    public Enumeration getHeaders(String s)
    {
        return null;
    }

    @Override
    public Enumeration getHeaderNames()
    {
        return null;
    }

    @Override
    public int getIntHeader(String s)
    {
        return 0;
    }

    @Override
    public String getMethod()
    {
        return null;
    }

    @Override
    public String getPathInfo()
    {
        return null;
    }

    @Override
    public String getPathTranslated()
    {
        return null;
    }

    @Override
    public String getContextPath()
    {
        return this.contextPath;
    }

    @Override
    public String getQueryString()
    {
        return "";
    }

    @Override
    public String getRemoteUser()
    {
        return null;
    }

    @Override
    public boolean isUserInRole(String s)
    {
        return false;
    }

    @Override
    public Principal getUserPrincipal()
    {
        return null;
    }

    @Override
    public String getRequestedSessionId()
    {
        return null;
    }

    @Override
    public String getRequestURI()
    {
        return null;
    }

    @Override
    public StringBuffer getRequestURL()
    {
        return new StringBuffer();
    }

    @Override
    public String getServletPath()
    {
        return null;
    }

    @Override
    public HttpSession getSession(boolean b)
    {
        return null;
    }

    @Override
    public HttpSession getSession()
    {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    @Override
    public String getResponseContentType()
    {
        return null;
    }

    @Override
    public Enumeration getResponseContentTypes()
    {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl()
    {
        return false;
    }

    @Override
    public Object getAttribute(String s)
    {
        return null;
    }

    @Override
    public Enumeration getAttributeNames()
    {
        return null;
    }

    @Override
    public String getCharacterEncoding()
    {
        return null;
    }

    @Override
    public InputStream getPortletInputStream() throws IOException
    {
        return null;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException
    {

    }

    @Override
    public int getContentLength()
    {
        return 0;
    }

    @Override
    public String getContentType()
    {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        return null;
    }

    @Override
    public String getParameter(String s)
    {
        return null;
    }

    @Override
    public Enumeration getParameterNames()
    {
        return null;
    }

    @Override
    public String[] getParameterValues(String s)
    {
        return new String[0];
    }

    @Override
    public Map getParameterMap()
    {
        return null;
    }

    @Override
    public String getProtocol()
    {
        return null;
    }

    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    @Override
    public String getScheme()
    {
        return scheme;
    }

    @Override
    public String getServerName()
    {
        return null;
    }

    @Override
    public int getServerPort()
    {
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        return null;
    }

    @Override
    public String getRemoteAddr()
    {
        return null;
    }

    @Override
    public String getRemoteHost()
    {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o)
    {

    }

    @Override
    public void removeAttribute(String s)
    {

    }

    @Override
    public Locale getLocale()
    {
        return null;
    }

    @Override
    public Enumeration getLocales()
    {
        return null;
    }

    @Override
    public boolean isSecure()
    {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s)
    {
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public String getRealPath(String s)
    {
        return null;
    }

    @Override
    public int getRemotePort()
    {
        return 0;
    }

    @Override
    public String getLocalName()
    {
        return null;
    }

    @Override
    public String getLocalAddr()
    {
        return null;
    }

    @Override
    public int getLocalPort()
    {
        return 0;
    }
}
