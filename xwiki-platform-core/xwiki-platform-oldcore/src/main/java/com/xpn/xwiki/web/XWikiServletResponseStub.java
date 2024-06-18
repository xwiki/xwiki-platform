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
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.container.Container;

/**
 * This stub is intended to simulate a servlet request in a daemon context, in order to be able to create a custom XWiki
 * context. This trick is used in to give a daemon thread access to the XWiki api.
 *
 * @version $Id$
 * @deprecated use the {@link Container} API instead
 */
// TODO: uncomment the annotation when XWiki Standard scripts are fully migrated to the new API
//@Deprecated(since = "42.0.0")
public class XWikiServletResponseStub implements XWikiResponse
{
    private OutputStream outputStream;

    private ServletOutputStream servletOutputStream = new ServletOutputStream()
    {
        @Override
        public void write(int b) throws IOException
        {
            if (XWikiServletResponseStub.this.outputStream != null) {
                XWikiServletResponseStub.this.outputStream.write(b);
            }
        }

        @Override
        public boolean isReady()
        {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener)
        {
            // Not needed
        }
    };

    public void setOutpuStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    @Override
    public HttpServletResponse getHttpServletResponse()
    {
        return null;
    }

    @Override
    public void setCharacterEncoding(String s)
    {
    }

    @Override
    public void removeCookie(String cookieName, XWikiRequest request)
    {
    }

    @Override
    public void addCookie(Cookie cookie)
    {
    }

    @Override
    public boolean containsHeader(String name)
    {
        return false;
    }

    @Override
    public String encodeURL(String url)
    {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url)
    {
        return url;
    }

    @Override
    public String encodeUrl(String url)
    {
        return url;
    }

    @Override
    public String encodeRedirectUrl(String url)
    {
        return url;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException
    {
    }

    @Override
    public void sendError(int sc) throws IOException
    {
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
    }

    @Override
    public void setDateHeader(String name, long date)
    {
    }

    @Override
    public void addDateHeader(String name, long date)
    {
    }

    @Override
    public void setHeader(String name, String value)
    {
    }

    @Override
    public void addHeader(String name, String value)
    {
    }

    @Override
    public void setIntHeader(String name, int value)
    {
    }

    @Override
    public void addIntHeader(String name, int value)
    {
    }

    @Override
    public void setStatus(int sc)
    {
    }

    @Override
    public void setStatus(int sc, String sm)
    {
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
    public ServletOutputStream getOutputStream() throws IOException
    {
        return this.servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        return null;
    }

    @Override
    public void setContentLength(int len)
    {
    }

    @Override
    public void setContentLengthLong(long len)
    {
    }

    @Override
    public void setContentType(String type)
    {
    }

    @Override
    public void setBufferSize(int size)
    {
    }

    @Override
    public int getBufferSize()
    {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException
    {
    }

    @Override
    public void resetBuffer()
    {
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
    public void setLocale(Locale loc)
    {
    }

    @Override
    public Locale getLocale()
    {
        return null;
    }

    @Override
    public int getStatus()
    {
        return 0;
    }

    @Override
    public String getHeader(String s)
    {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String s)
    {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames()
    {
        return null;
    }
}
