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
package org.xwiki.wikistream.internal.output;

import java.io.IOException;
import java.io.OutputStream;

import org.xwiki.wikistream.output.OutputStreamOutputTarget;
import org.xwiki.wikistream.output.OutputTarget;

/**
 * @version $Id$
 * @since 5.2M2
 */
public class DefaultOutputStreamOutputTarget implements OutputStreamOutputTarget
{
    private final boolean close;

    private final OutputStream outputStream;

    /**
     * Create an instance of {@link OutputStreamOutputTarget} returning the passed {@link OutputStream}.
     * <p>
     * The passed {@link OutputStream} is not closed when the {@link OutputTarget} is closed.
     * 
     * @param outputStream the {@link OutputStream}
     */
    public DefaultOutputStreamOutputTarget(OutputStream outputStream)
    {
        this(outputStream, false);
    }

    /**
     * Create an instance of {@link OutputStreamOutputTarget} returning the passed {@link OutputStream}.
     * 
     * @param outputStream the {@link OutputStream}
     * @param close indicate of the passer {@link OutputStream} should be closed when the {@link OutputTarget} is closed
     * @since 5.4.4, 6.0M2
     */
    public DefaultOutputStreamOutputTarget(OutputStream outputStream, boolean close)
    {
        this.outputStream = outputStream;
        this.close = close;
    }

    @Override
    public boolean restartSupported()
    {
        return false;
    }

    public OutputStream getOutputStream()
    {
        return this.outputStream;
    }

    @Override
    public void close() throws IOException
    {
        if (this.close) {
            this.outputStream.close();
        }
    }

    @Override
    public String toString()
    {
        return this.outputStream.toString();
    }
}
