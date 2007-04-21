package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.web.XWikiResponse;

import javax.portlet.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

public class MockXWikiResponse implements XWikiResponse
{

    public HttpServletResponse getHttpServletResponse()
    {
        // method stub
        return null;
    }

    public void setCharacterEncoding(String s)
    {
        // method stub
    }

    public void addCookie(Cookie arg0)
    {
        // method stub
    }

    public boolean containsHeader(String arg0)
    {
        // method stub
        return false;
    }

    public String encodeURL(String arg0)
    {
        // method stub
        return null;
    }

    public String encodeRedirectURL(String arg0)
    {
        // method stub
        return null;
    }

    public String encodeUrl(String arg0)
    {
        // method stub
        return null;
    }

    public String encodeRedirectUrl(String arg0)
    {
        // method stub
        return null;
    }

    public void sendError(int arg0, String arg1) throws IOException
    {
        // method stub
    }

    public void sendError(int arg0) throws IOException
    {
        // method stub
    }

    public void sendRedirect(String arg0) throws IOException
    {
        // method stub
    }

    public void setDateHeader(String arg0, long arg1)
    {
        // method stub
    }

    public void addDateHeader(String arg0, long arg1)
    {
        // method stub
    }

    public void setHeader(String arg0, String arg1)
    {
        // method stub
    }

    public void addHeader(String arg0, String arg1)
    {
        // method stub
    }

    public void setIntHeader(String arg0, int arg1)
    {
        // method stub
    }

    public void addIntHeader(String arg0, int arg1)
    {
        // method stub
    }

    public void setStatus(int arg0)
    {
        // method stub
    }

    public void setStatus(int arg0, String arg1)
    {
        // method stub
    }

    public String getCharacterEncoding()
    {
        // method stub
        return null;
    }

    public String getContentType()
    {
        // method stub
        return null;
    }

    public ServletOutputStream getOutputStream() throws IOException
    {
        // method stub
        return null;
    }

    public PrintWriter getWriter() throws IOException
    {
        // method stub
        return null;
    }

    public void setContentLength(int arg0)
    {
        // method stub
    }

    public void setContentType(String arg0)
    {
        // method stub
    }

    public void setBufferSize(int arg0)
    {
        // method stub
    }

    public int getBufferSize()
    {
        // method stub
        return 0;
    }

    public void flushBuffer() throws IOException
    {
        // method stub
    }

    public void resetBuffer()
    {
        // method stub
    }

    public boolean isCommitted()
    {
        // method stub
        return false;
    }

    public void reset()
    {
        // method stub
    }

    public void setLocale(Locale arg0)
    {
        // method stub
    }

    public Locale getLocale()
    {
        // method stub
        return null;
    }

    public PortletURL createRenderURL()
    {
        // method stub
        return null;
    }

    public PortletURL createActionURL()
    {
        // method stub
        return null;
    }

    public String getNamespace()
    {
        // method stub
        return null;
    }

    public void setTitle(String arg0)
    {
        // method stub
    }

    public OutputStream getPortletOutputStream() throws IOException
    {
        // method stub
        return null;
    }

    public void addProperty(String arg0, String arg1)
    {
        // method stub
    }

    public void setProperty(String arg0, String arg1)
    {
        // method stub
    }

    public void setWindowState(WindowState arg0) throws WindowStateException
    {
        // method stub
    }

    public void setPortletMode(PortletMode arg0) throws PortletModeException
    {
        // method stub
    }

    public void setRenderParameters(Map arg0)
    {
        // method stub
    }

    public void setRenderParameter(String arg0, String arg1)
    {
        // method stub
    }

    public void setRenderParameter(String arg0, String[] arg1)
    {
        // method stub
    }
}
