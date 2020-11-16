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
package org.xwiki.livedata.internal.livetable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletResponse;

/**
 * Wraps an XWiki response in order to allow capturing the live table results JSON.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
public class LiveTableResponse extends XWikiServletResponse
{
    private StringWriter content = new StringWriter();

    private PrintWriter writer = new PrintWriter(this.content);

    private boolean committed;

    /**
     * Creates a new live table response wrapping the given XWiki response.
     * 
     * @param response the XWiki response to wrap
     */
    public LiveTableResponse(XWikiResponse response)
    {
        super(response);
    }

    /**
     * @return the content that has been written to the response
     */
    public String getContent()
    {
        return this.content.toString();
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        // Use a custom writer in order to capture the live table results JSON.
        return this.writer;
    }

    @Override
    public void setContentType(String type)
    {
        // Don't change the content type on the original response.
    }

    @Override
    public void setCharacterEncoding(String s)
    {
        // Don't change the character encoding on the original response.
    }

    @Override
    public void setContentLength(int length)
    {
        // Don't change the content length on the original response.
    }

    @Override
    public void setContentLengthLong(long length)
    {
        // Don't change the content length on the original response.
    }

    @Override
    public void flushBuffer() throws IOException
    {
        // Don't flush the buffer on the original response.
        this.committed = true;
    }

    @Override
    public boolean isCommitted()
    {
        return this.committed;
    }
}
