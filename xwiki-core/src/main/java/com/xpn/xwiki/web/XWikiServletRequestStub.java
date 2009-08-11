package com.xpn.xwiki.web;

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

/**
 * This stub is intended to simulate a servlet request in a daemon context, in order to be able to create a custom XWiki
 * context. This trick is used in to give a daemon thread access to the XWiki api.
 * 
 * @version $Id$
 */
public class XWikiServletRequestStub implements XWikiRequest
{
    /** The scheme used by the runtime instance. This is required for creating URLs from daemon thread. */
    private String scheme;

    public XWikiServletRequestStub()
    {
        this.host = "";
    }

    private String host;

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getHeader(String s)
    {
        if (s.equals("x-forwarded-host")) {
            return this.host;
        }
        return "";
    }

    public String get(String name)
    {
        return "";
    }

    public HttpServletRequest getHttpServletRequest()
    {
        return null;
    }

    public Cookie getCookie(String cookieName)
    {
        return null;
    }

    public boolean isWindowStateAllowed(WindowState windowState)
    {
        return false;
    }

    public boolean isPortletModeAllowed(PortletMode portletMode)
    {
        return false;
    }

    public PortletMode getPortletMode()
    {
        return null;
    }

    public WindowState getWindowState()
    {
        return null;
    }

    public PortletPreferences getPreferences()
    {
        return null;
    }

    public PortletSession getPortletSession()
    {
        return null;
    }

    public PortletSession getPortletSession(boolean b)
    {
        return null;
    }

    public String getProperty(String s)
    {
        return null;
    }

    public Enumeration getProperties(String s)
    {
        return null;
    }

    public Enumeration getPropertyNames()
    {
        return null;
    }

    public PortalContext getPortalContext()
    {
        return null;
    }

    public String getAuthType()
    {
        return "";
    }

    public Cookie[] getCookies()
    {
        return new Cookie[0];
    }

    public long getDateHeader(String s)
    {
        return 0;
    }

    public Enumeration getHeaders(String s)
    {
        return null;
    }

    public Enumeration getHeaderNames()
    {
        return null;
    }

    public int getIntHeader(String s)
    {
        return 0;
    }

    public String getMethod()
    {
        return null;
    }

    public String getPathInfo()
    {
        return null;
    }

    public String getPathTranslated()
    {
        return null;
    }

    public String getContextPath()
    {
        return null;
    }

    public String getQueryString()
    {
        return "";
    }

    public String getRemoteUser()
    {
        return null;
    }

    public boolean isUserInRole(String s)
    {
        return false;
    }

    public Principal getUserPrincipal()
    {
        return null;
    }

    public String getRequestedSessionId()
    {
        return null;
    }

    public String getRequestURI()
    {
        return null;
    }

    public StringBuffer getRequestURL()
    {
        return new StringBuffer();
    }

    public String getServletPath()
    {
        return null;
    }

    public HttpSession getSession(boolean b)
    {
        return null;
    }

    public HttpSession getSession()
    {
        return null;
    }

    public boolean isRequestedSessionIdValid()
    {
        return false;
    }

    public String getResponseContentType()
    {
        return null;
    }

    public Enumeration getResponseContentTypes()
    {
        return null;
    }

    public boolean isRequestedSessionIdFromCookie()
    {
        return false;
    }

    public boolean isRequestedSessionIdFromURL()
    {
        return false;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public boolean isRequestedSessionIdFromUrl()
    {
        return false;
    }

    public Object getAttribute(String s)
    {
        return null;
    }

    public Enumeration getAttributeNames()
    {
        return null;
    }

    public String getCharacterEncoding()
    {
        return null;
    }

    public InputStream getPortletInputStream() throws IOException
    {
        return null;
    }

    public void setCharacterEncoding(String s) throws UnsupportedEncodingException
    {

    }

    public int getContentLength()
    {
        return 0;
    }

    public String getContentType()
    {
        return null;
    }

    public ServletInputStream getInputStream() throws IOException
    {
        return null;
    }

    public String getParameter(String s)
    {
        return null;
    }

    public Enumeration getParameterNames()
    {
        return null;
    }

    public String[] getParameterValues(String s)
    {
        return new String[0];
    }

    public Map getParameterMap()
    {
        return null;
    }

    public String getProtocol()
    {
        return null;
    }

    public void setScheme(String scheme)
    {
        this.scheme = scheme;
    }

    public String getScheme()
    {
        return scheme;
    }

    public String getServerName()
    {
        return null;
    }

    public int getServerPort()
    {
        return 0;
    }

    public BufferedReader getReader() throws IOException
    {
        return null;
    }

    public String getRemoteAddr()
    {
        return null;
    }

    public String getRemoteHost()
    {
        return null;
    }

    public void setAttribute(String s, Object o)
    {

    }

    public void removeAttribute(String s)
    {

    }

    public Locale getLocale()
    {
        return null;
    }

    public Enumeration getLocales()
    {
        return null;
    }

    public boolean isSecure()
    {
        return false;
    }

    public RequestDispatcher getRequestDispatcher(String s)
    {
        return null;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getRealPath(String s)
    {
        return null;
    }

    public int getRemotePort()
    {
        return 0;
    }

    public String getLocalName()
    {
        return null;
    }

    public String getLocalAddr()
    {
        return null;
    }

    public int getLocalPort()
    {
        return 0;
    }
}
