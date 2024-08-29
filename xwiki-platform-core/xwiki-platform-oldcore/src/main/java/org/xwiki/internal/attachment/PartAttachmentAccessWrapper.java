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
package org.xwiki.internal.attachment;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.Part;

import org.xwiki.attachment.AttachmentAccessWrapper;

/**
 * Attachment wrapper for {@link Part}. Provides access to various metadata required to access an attachment. For
 * instance, its size, name, and an input steam of its content. This wrapper is used to wrap a {@link Part} that is
 * meant to be saved as an attachment.
 *
 * @version $Id$
 * @since 14.10
 */
public class PartAttachmentAccessWrapper implements AttachmentAccessWrapper
{
    private final Part part;

    /**
     * Default constructor.
     *
     * @param part the part to access as an attachment
     */
    public PartAttachmentAccessWrapper(Part part)
    {
        this.part = part;
    }

    @Override
    public long getSize()
    {
        return this.part.getSize();
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        try {
            return this.part.getInputStream();
        } catch (IOException e) {
            throw new IOException(
                String.format("Failed to read the input stream for part [%s]", this.part), e);
        }
    }

    @Override
    public String getFileName()
    {
        return this.part.getSubmittedFileName();
    }
}
