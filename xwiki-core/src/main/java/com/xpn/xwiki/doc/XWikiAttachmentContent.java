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
 *
 */

package com.xpn.xwiki.doc;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class XWikiAttachmentContent implements Cloneable
{
    private XWikiAttachment attachment;

    private byte[] content;

    private boolean isContentDirty = false;

    public XWikiAttachmentContent(XWikiAttachment attachment)
    {
        this();

        setAttachment(attachment);
    }

    public XWikiAttachmentContent()
    {
        this.content = new byte[0];
    }

    public long getId()
    {
        return this.attachment.getId();
    }

    public void setId(long id)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        XWikiAttachmentContent attachmentcontent = null;
        try {
            attachmentcontent = (XWikiAttachmentContent) getClass().newInstance();
        } catch (Exception e) {
            // This should not happen
        }

        attachmentcontent.setAttachment(getAttachment());

        // setContent makes isContentDirty true which if this.isContentDirty == false then it's not a valid clone.
        attachmentcontent.content = this.getContent();

        return attachmentcontent;
    }

    /**
     * @return a byte array containing the binary content of the attachment
     * 
     * @deprecated use {@link #getContentInputStream()} instead
     */
    @Deprecated
    public byte[] getContent()
    {
        if (this.content == null) {
            return new byte[0];
        } else {
            return this.content;
        }
    }

    /**
     * Set the content from a byte array
     *
     * @param content a byte array containing the binary data of the attachment
     *
     * @deprecated use {@link #setContent(java.io.InputStream, int)} instead
     */
    @Deprecated
    public void setContent(byte[] content)
    {
        if (content == null) {
            this.content = null;
        } else {
            if (!content.equals(this.content)) {
                setContentDirty(true);
            }
            this.content = content;
            this.attachment.setFilesize(content.length);
        }
    }

    public XWikiAttachment getAttachment()
    {
        return this.attachment;
    }

    public void setAttachment(XWikiAttachment attachment)
    {
        this.attachment = attachment;
    }

    public boolean isContentDirty()
    {
        return this.isContentDirty;
    }

    public void setContentDirty(boolean contentDirty)
    {
        this.isContentDirty = contentDirty;
    }

    /**
     * @return an InputStream to read the binary content of this attachment
     *
     * @since 2.3M2
     */
    public InputStream getContentInputStream()
    {
        return new ByteArrayInputStream(getContent());
    }

    /**
     * set the content of the attachment from an InputStream
     *
     * @param is the input stream that will be read
     * @param len the length in byte to read
     * @throws IOException when an error occurs during streaming operation
     * @since 2.3M2
     */
    public void setContent(InputStream is, int len) throws IOException
    {
        setContent(readData(is, len));
    }

    /**
     * @return the true size of the content of the attachment
     *
     * @since 2.3M2
     */
    public int getSize()
    {
        return content.length;
    }

    /**
     * Read an input stream into a byte array
     *
     * @param is the input stream to read
     * @param len the len to read
     * @return a byte array of size len containing the read data
     * @throws IOException when an error occurs during streaming operation
     * @since 2.3M2
     */
    private byte[] readData(InputStream is, int len) throws IOException
    {
        if (is == null) {
            return null;
        }

        int off = 0;
        byte[] buf = new byte[len];
        while (len > 0) {
            int n = is.read(buf, off, len);
            if (n == -1) {
                throw new EOFException();
            }
            off += n;
            len -= n;
        }
        return buf;
    }
}
