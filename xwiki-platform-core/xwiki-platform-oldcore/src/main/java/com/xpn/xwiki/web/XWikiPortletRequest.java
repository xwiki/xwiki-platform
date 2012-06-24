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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.WindowState;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.util.Util;

public class XWikiPortletRequest implements XWikiRequest
{
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final String ROOT_SPACE_PREF_NAME = "rootSpace";

    private PortletRequest request;

    public XWikiPortletRequest(PortletRequest request)
    {
        this.request = request;
    }

    public PortletRequest getPortletRequest()
    {
        return request;
    }

    @Override
    public String get(String name)
    {
        return request.getParameter(name);
    }

    @Override
    public String getParameter(String name)
    {
        String retVal = request.getParameter(name);
        if (retVal == null && name.equals("topic")) {
            String rootSpace = request.getPreferences().getValue("rootSpace", null);
            if (rootSpace != null && rootSpace.length() > 0) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Root space [" + rootSpace + "] was getted from preferences");
                retVal = rootSpace + ".WebHome";
            }
        }
        return retVal;
    }

    @Override
    public Enumeration getParameterNames()
    {
        return request.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return request.getParameterValues(name);
    }

    @Override
    public boolean isWindowStateAllowed(WindowState windowState)
    {
        return request.isWindowStateAllowed(windowState);
    }

    @Override
    public boolean isPortletModeAllowed(PortletMode portletMode)
    {
        return request.isPortletModeAllowed(portletMode);
    }

    @Override
    public PortletMode getPortletMode()
    {
        return request.getPortletMode();
    }

    @Override
    public WindowState getWindowState()
    {
        return request.getWindowState();
    }

    @Override
    public PortletPreferences getPreferences()
    {
        return request.getPreferences();
    }

    @Override
    public PortletSession getPortletSession()
    {
        return request.getPortletSession();
    }

    @Override
    public PortletSession getPortletSession(boolean b)
    {
        return request.getPortletSession(b);
    }

    @Override
    public String getProperty(String s)
    {
        return request.getProperty(s);
    }

    @Override
    public Enumeration getProperties(String s)
    {
        return request.getProperties(s);
    }

    @Override
    public Enumeration getPropertyNames()
    {
        return request.getPropertyNames();
    }

    @Override
    public PortalContext getPortalContext()
    {
        return request.getPortalContext();
    }

    @Override
    public String getRemoteUser()
    {
        return request.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String s)
    {
        return request.isUserInRole(s);
    }

    @Override
    public Principal getUserPrincipal()
    {
        return request.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId()
    {
        return request.getRequestedSessionId();
    }

    @Override
    public Locale getLocale()
    {
        return request.getLocale();
    }

    @Override
    public Enumeration getLocales()
    {
        return request.getLocales();
    }

    @Override
    public boolean isSecure()
    {
        return request.isSecure();
    }

    @Override
    public Map getParameterMap()
    {
        return request.getParameterMap();
    }

    @Override
    public String getScheme()
    {
        return request.getScheme();
    }

    @Override
    public String getServerName()
    {
        return request.getServerName();
    }

    @Override
    public int getServerPort()
    {
        return request.getServerPort();
    }

    @Override
    public void setAttribute(String s, Object o)
    {
        request.setAttribute(s, o);
    }

    @Override
    public void removeAttribute(String s)
    {
        request.removeAttribute(s);
    }

    @Override
    public String getResponseContentType()
    {
        return request.getResponseContentType();
    }

    @Override
    public Enumeration getResponseContentTypes()
    {
        return request.getResponseContentTypes();
    }

    @Override
    public String getContextPath()
    {
        return request.getContextPath();
    }

    @Override
    public Object getAttribute(String s)
    {
        return request.getAttribute(s);
    }

    @Override
    public Enumeration getAttributeNames()
    {
        return request.getAttributeNames();
    }

    @Override
    public boolean isRequestedSessionIdValid()
    {
        return request.isRequestedSessionIdValid();
    }

    /*
     * Implemented Servlet Function for Portlets This will only work if the portlet implementation makes PortletRequest
     * extends HttpServletRequest Modified getHttpServletRequest to work with WebLogic Portal Implementation
     */

    @Override
    public HttpServletRequest getHttpServletRequest()
    {
        HttpServletRequest req = null;

        if (request instanceof HttpServletRequest) {
            req = (HttpServletRequest) getPortletRequest();
        } else {
            // WLP impl
            req = (HttpServletRequest) getPortletRequest().getAttribute("javax.servlet.request");
        }

        if (req == null) {
            throw new UnsupportedOperationException();
        }

        return req;
    }

    @Override
    public String getPathInfo()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getPathInfo();
        return null;
    }

    @Override
    public String getServletPath()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getServletPath();
        return null;
    }

    @Override
    public StringBuffer getRequestURL()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRequestURL();
        return null;
    }

    @Override
    public String getQueryString()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getQueryString();
        return null;
    }

    @Override
    public String getRequestURI()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRequestURI();
        return null;
    }

    @Override
    public String getAuthType()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getAuthType();
        return null;
    }

    @Override
    public Cookie[] getCookies()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getCookies();
        return null;
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        if (request instanceof HttpServletRequest)
            return Util.getCookie(cookieName, getHttpServletRequest());
        else
            return null;
    }

    @Override
    public long getDateHeader(String s)
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getDateHeader(s);
        return 0;
    }

    @Override
    public String getHeader(String s)
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getHeader(s);
        return null;
    }

    @Override
    public Enumeration getHeaders(String s)
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getHeaders(s);
        return null;
    }

    @Override
    public Enumeration getHeaderNames()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getHeaderNames();
        return null;
    }

    @Override
    public int getIntHeader(String s)
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getIntHeader(s);
        return 0;
    }

    @Override
    public String getMethod()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getMethod();
        return null;
    }

    @Override
    public String getCharacterEncoding()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getCharacterEncoding();
        return null;
    }

    @Override
    public InputStream getPortletInputStream() throws IOException
    {
        if (request instanceof ActionRequest)
            return ((ActionRequest) request).getPortletInputStream();
        return null;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException
    {
        if (request instanceof HttpServletRequest)
            getHttpServletRequest().setCharacterEncoding(s);
    }

    @Override
    public int getContentLength()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getContentLength();
        return 0;
    }

    @Override
    public String getContentType()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getContentType();
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getInputStream();
        return null;
    }

    @Override
    public HttpSession getSession(boolean b)
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getSession(b);
        return null;
    }

    @Override
    public HttpSession getSession()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getSession();
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().isRequestedSessionIdFromCookie();
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().isRequestedSessionIdFromURL();
        return false;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().isRequestedSessionIdFromUrl();
        return false;
    }

    @Override
    public String getPathTranslated()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getPathTranslated();
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s)
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRequestDispatcher(s);
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public String getRealPath(String s)
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRealPath(s);
        return s;
    }

    @Override
    public int getRemotePort()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRemotePort();
        return 0;
    }

    @Override
    public String getLocalName()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getLocalName();
        return "";
    }

    @Override
    public String getLocalAddr()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getLocalAddr();
        return "";
    }

    @Override
    public int getLocalPort()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getLocalPort();
        return 0;
    }

    @Override
    public String getProtocol()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getProtocol();
        return null;
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getReader();
        return null;
    }

    @Override
    public String getRemoteAddr()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRemoteAddr();
        return null;
    }

    @Override
    public String getRemoteHost()
    {
        if (request instanceof HttpServletRequest)
            return getHttpServletRequest().getRemoteHost();
        return null;
    }
}
