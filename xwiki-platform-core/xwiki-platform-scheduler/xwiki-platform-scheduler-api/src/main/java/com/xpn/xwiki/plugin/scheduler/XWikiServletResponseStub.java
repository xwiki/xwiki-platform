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

import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * This stub is intended to emulate a servlet response in the daemon context of the scheduler.
 * 
 * @version $Id$
 */
public class XWikiServletResponseStub implements XWikiResponse
{
    @Override
    public HttpServletResponse getHttpServletResponse()
    {
        return null;
    }

    @Override
    public void removeCookie(String arg0, XWikiRequest arg1)
    {

    }

    @Override
    public void setCharacterEncoding(String arg0)
    {

    }

    @Override
    public void addCookie(Cookie arg0)
    {

    }

    @Override
    public void addDateHeader(String arg0, long arg1)
    {

    }

    @Override
    public void addHeader(String arg0, String arg1)
    {

    }

    @Override
    public void addIntHeader(String arg0, int arg1)
    {

    }

    @Override
    public boolean containsHeader(String arg0)
    {
        return false;
    }

    @Override
    public String encodeRedirectURL(String arg0)
    {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String arg0)
    {
        return null;
    }

    @Override
    public String encodeURL(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String encodeUrl(String arg0)
    {
        return null;
    }

    @Override
    public void sendError(int arg0) throws IOException
    {

    }

    @Override
    public void sendError(int arg0, String arg1) throws IOException
    {

    }

    @Override
    public void sendRedirect(String arg0) throws IOException
    {

    }

    @Override
    public void setDateHeader(String arg0, long arg1)
    {

    }

    @Override
    public void setHeader(String arg0, String arg1)
    {

    }

    @Override
    public void setIntHeader(String arg0, int arg1)
    {

    }

    @Override
    public void setStatus(int arg0)
    {

    }

    @Override
    public void setStatus(int arg0, String arg1)
    {

    }

    @Override
    public void flushBuffer() throws IOException
    {

    }

    @Override
    public int getBufferSize()
    {
        return 0;
    }

    @Override
    public String getCharacterEncoding()
    {
        return null;
    }

    @Override
    public String getContentType()
    {
        return null;
    }

    @Override
    public Locale getLocale()
    {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        return null;
    }

    @Override
    public boolean isCommitted()
    {
        return false;
    }

    @Override
    public void reset()
    {

    }

    @Override
    public void resetBuffer()
    {

    }

    @Override
    public void setBufferSize(int arg0)
    {

    }

    @Override
    public void setContentLength(int arg0)
    {

    }

    @Override
    public void setContentType(String arg0)
    {

    }

    @Override
    public void setLocale(Locale arg0)
    {

    }

    @Override
    public PortletURL createActionURL()
    {
        return null;
    }

    @Override
    public PortletURL createRenderURL()
    {
        return null;
    }

    @Override
    public String getNamespace()
    {
        return null;
    }

    @Override
    public OutputStream getPortletOutputStream() throws IOException
    {
        return null;
    }

    @Override
    public void setTitle(String arg0)
    {

    }

    @Override
    public void addProperty(String arg0, String arg1)
    {

    }

    @Override
    public void setProperty(String arg0, String arg1)
    {

    }

    @Override
    public void setPortletMode(PortletMode arg0) throws PortletModeException
    {

    }

    @Override
    public void setRenderParameter(String arg0, String arg1)
    {

    }

    @Override
    public void setRenderParameter(String arg0, String[] arg1)
    {

    }

    @Override
    public void setRenderParameters(Map arg0)
    {

    }

    @Override
    public void setWindowState(WindowState arg0) throws WindowStateException
    {

    }
}
