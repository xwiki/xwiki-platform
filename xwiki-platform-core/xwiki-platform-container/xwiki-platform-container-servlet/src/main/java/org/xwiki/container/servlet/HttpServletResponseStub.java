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
package org.xwiki.container.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This stub is intended to simulate a servlet request in a daemon context, in order to be able to create a custom XWiki
 * context. This trick is used in to give a daemon thread access to the XWiki api.
 *
 * @version $Id$
 * @since 17.0.0RC1
 */
public class HttpServletResponseStub implements HttpServletResponse
{
    private OutputStream outputStream;

    private ServletOutputStream servletOutputStream = new ServletOutputStream()
    {
        @Override
        public void write(int b) throws IOException
        {
            if (HttpServletResponseStub.this.outputStream != null) {
                HttpServletResponseStub.this.outputStream.write(b);
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

    /**
     * @param outputStream the stream where to write the response entity
     */
    public void setOutpuStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    @Override
    public void setCharacterEncoding(String s)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void addCookie(Cookie cookie)
    {
        // Nothing to do, this stub does not send any response.
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
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void sendError(int sc) throws IOException
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setDateHeader(String name, long date)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void addDateHeader(String name, long date)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setHeader(String name, String value)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void addHeader(String name, String value)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setIntHeader(String name, int value)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void addIntHeader(String name, int value)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setStatus(int sc)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setStatus(int sc, String sm)
    {
        // Nothing to do, this stub does not send any response.
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
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setContentLengthLong(long len)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setContentType(String type)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setBufferSize(int size)
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public int getBufferSize()
    {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void resetBuffer()
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public boolean isCommitted()
    {
        return false;
    }

    @Override
    public void reset()
    {
        // Nothing to do, this stub does not send any response.
    }

    @Override
    public void setLocale(Locale loc)
    {
        // Nothing to do, this stub does not send any response.
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
