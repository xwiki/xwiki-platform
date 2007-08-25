package com.xpn.xwiki.xmlrpc;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

public class MockXWikiServletContext implements ServletContext
{

    public Object getAttribute(String arg0)
    {
        // method stub
        return null;
    }

    public Enumeration getAttributeNames()
    {
        // method stub
        return null;
    }

    public ServletContext getContext(String arg0)
    {
        // method stub
        return null;
    }

    public String getInitParameter(String arg0)
    {
        // method stub
        return null;
    }

    public Enumeration getInitParameterNames()
    {
        // method stub
        return null;
    }

    public int getMajorVersion()
    {
        // method stub
        return 0;
    }

    public String getMimeType(String arg0)
    {
        // method stub
        return null;
    }

    public int getMinorVersion()
    {
        // method stub
        return 0;
    }

    public RequestDispatcher getNamedDispatcher(String arg0)
    {
        // method stub
        return null;
    }

    public String getRealPath(String arg0)
    {
        // method stub
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String arg0)
    {
        // method stub
        return null;
    }

    public URL getResource(String arg0) throws MalformedURLException
    {
        // method stub
        return null;
    }

    public InputStream getResourceAsStream(String arg0)
    {
        // method stub
        return null;
    }

    public Set getResourcePaths(String arg0)
    {
        // method stub
        return null;
    }

    public String getServerInfo()
    {
        // method stub
        return null;
    }

    public Servlet getServlet(String arg0) throws ServletException
    {
        // method stub
        return null;
    }

    public String getServletContextName()
    {
        // method stub
        return null;
    }

    public Enumeration getServletNames()
    {
        // method stub
        return null;
    }

    public Enumeration getServlets()
    {
        // method stub
        return null;
    }

    public void log(String arg0)
    {
        // method stub
    }

    public void log(Exception arg0, String arg1)
    {
        // method stub
    }

    public void log(String arg0, Throwable arg1)
    {
        // method stub
    }

    public void removeAttribute(String arg0)
    {
        // method stub
    }

    public void setAttribute(String arg0, Object arg1)
    {
        // method stub
    }

}
