package com.xpn.xwiki.xmlrpc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletInputStream;
import javax.servlet.RequestDispatcher;
import java.util.Enumeration;
import java.util.Map;
import java.util.Locale;
import java.security.Principal;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.BufferedReader;

public class MockXWikiRequest implements HttpServletRequest
{
    public String getAuthType()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public Cookie[] getCookies()
    {
        return new Cookie[0]; // To change body of implemented methods use File | Settings | File
                                // Templates.
    }

    public long getDateHeader(String string)
    {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
    }

    public String getHeader(String string)
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public Enumeration getHeaders(String string)
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public Enumeration getHeaderNames()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public int getIntHeader(String string)
    {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
    }

    public String getMethod()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getPathInfo()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getPathTranslated()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getContextPath()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getQueryString()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getRemoteUser()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public boolean isUserInRole(String string)
    {
        return false; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public Principal getUserPrincipal()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getRequestedSessionId()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getRequestURI()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public StringBuffer getRequestURL()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getServletPath()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public HttpSession getSession(boolean b)
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public HttpSession getSession()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public boolean isRequestedSessionIdValid()
    {
        return false; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public boolean isRequestedSessionIdFromCookie()
    {
        return false; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public boolean isRequestedSessionIdFromURL()
    {
        return false; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public boolean isRequestedSessionIdFromUrl()
    {
        return false; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public Object getAttribute(String string)
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public Enumeration getAttributeNames()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getCharacterEncoding()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public void setCharacterEncoding(String string) throws UnsupportedEncodingException
    {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    public int getContentLength()
    {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
    }

    public String getContentType()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public ServletInputStream getInputStream() throws IOException
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getParameter(String string)
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public Enumeration getParameterNames()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String[] getParameterValues(String string)
    {
        return new String[0]; // To change body of implemented methods use File | Settings | File
                                // Templates.
    }

    public Map getParameterMap()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getProtocol()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getScheme()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getServerName()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public int getServerPort()
    {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
    }

    public BufferedReader getReader() throws IOException
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getRemoteAddr()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getRemoteHost()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public void setAttribute(String string, Object object)
    {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeAttribute(String string)
    {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    public Locale getLocale()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public Enumeration getLocales()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public boolean isSecure()
    {
        return false; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public RequestDispatcher getRequestDispatcher(String string)
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getRealPath(String string)
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public int getRemotePort()
    {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
    }

    public String getLocalName()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public String getLocalAddr()
    {
        return null; // To change body of implemented methods use File | Settings | File
                        // Templates.
    }

    public int getLocalPort()
    {
        return 0; // To change body of implemented methods use File | Settings | File Templates.
    }
}
