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

import org.xwiki.attachment.AttachmentAccessWrapper;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Attachment wrapper for {@link XWikiAttachment}. Provides access to various metadata required to access an attachment.
 * For instance, its size, name, and an input steam of its content.
 *
 * @version $Id$
 * @since 14.10
 */
public class XWikiAttachmentAccessWrapper implements AttachmentAccessWrapper
{
    private final XWikiAttachment attachment;

    private final XWikiContext context;

    /**
     * Default constructor.
     *
     * @param attachment the attachment to wrap
     * @param context the content, used when retrieving the attachment input stream
     */
    public XWikiAttachmentAccessWrapper(XWikiAttachment attachment, XWikiContext context)
    {
        this.attachment = attachment;
        this.context = context;
    }

    @Override
    public long getSize()
    {
        return this.attachment.getLongSize();
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        try {
            return this.attachment.getContentInputStream(this.context);
        } catch (XWikiException e) {
            throw new IOException(
                String.format("Failed to read the input stream for attachment [%s]", this.attachment), e);
        }
    }

    @Override
    public String getFileName()
    {
        return this.attachment.getFilename();
    }
}
