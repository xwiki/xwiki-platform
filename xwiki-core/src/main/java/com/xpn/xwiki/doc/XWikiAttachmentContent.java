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

/**
 * The content of an attachment. Objects of this class hold the actual content which will be downloaded when a user
 * downloads an attachment.
 * 
 * @version $Id$
 */
public class XWikiAttachmentContent implements Cloneable
{
    /** The XWikiAttachment (attachment metadata) which this attachment content is associated with. */
    private XWikiAttachment attachment;

    /** The content of the attachment. */
    private byte[] content;

    /** True if the content is out of sync with the content stored in the database and thus needs to be saved. */
    private boolean isContentDirty;

    /**
     * Constructor with associated attachment specified.
     * 
     * @param attachment the attachment which this is the content for.
     */
    public XWikiAttachmentContent(XWikiAttachment attachment)
    {
        this();
        this.setAttachment(attachment);
    }

    /**
     * The default Constructor. For creating content which will be associated with an attachment later.
     */
    public XWikiAttachmentContent()
    {
        this.content = new byte[0];
    }

    /**
     * This is used so that Hibernate will associate this content with the right attachment (metadata).
     * 
     * @return the id of the attachment (metadata) which this content is associated with.
     */
    public long getId()
    {
        return this.attachment.getId();
    }

    /**
     * This function does nothing and exists only for Hibernate to be able to load a value which is not used.
     * 
     * @param id is ignored.
     */
    public void setId(long id)
    {
        // Do nothing here.
        // The id is taken from the attachment which is set in XWikiHibernateAttachmentStore#loadAttachmentContent.
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        final XWikiAttachmentContent newContent = new XWikiAttachmentContent(this.getAttachment());

        // This points the new attachment to the same array, behavior is not guarenteed if the byte array is mutated.
        newContent.setContent(this.getContent());

        newContent.setContentDirty(this.isContentDirty());

        return newContent;
    }

    /**
     * @return a byte array containing the binary content of the attachment.
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
     * Set the content from a byte array.
     * 
     * @param content a byte array containing the binary data of the attachment
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

    /**
     * @return which attachment (Metadata) this content belongs to.
     */
    public XWikiAttachment getAttachment()
    {
        return this.attachment;
    }

    /**
     * @param attachment which attachment (metadata) this content is to be associated with.
     */
    public void setAttachment(XWikiAttachment attachment)
    {
        this.attachment = attachment;
    }

    /**
     * Is the content "dirty" meaning out of sync with the database.
     * 
     * @return true if the content is out of sync with the database and in need of saving.
     */
    public boolean isContentDirty()
    {
        return this.isContentDirty;
    }

    /**
     * Set the content as "dirty" meaning out of sync with the database.
     * 
     * @param contentDirty if true then the content is regarded as out of sync with the database and in need of saving,
     *            otherwise it's considered saved.
     */
    public void setContentDirty(boolean contentDirty)
    {
        this.isContentDirty = contentDirty;
    }

    /**
     * @return an InputStream to read the binary content of this attachment.
     * @since 2.3M2
     */
    public InputStream getContentInputStream()
    {
        return new ByteArrayInputStream(getContent());
    }

    /**
     * Set the content of the attachment from an InputStream.
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
     * @return the true size of the content of the attachment.
     * @since 2.3M2
     */
    public int getSize()
    {
        return this.content.length;
    }

    /**
     * Read an input stream into a byte array.
     * 
     * @param is the input stream to read
     * @param length the number of bytes of the stream to read.
     * @return a byte array of size len containing the read data
     * @throws IOException when an error occurs during streaming operation
     * @since 2.3M2
     */
    private byte[] readData(InputStream is, int length) throws IOException
    {
        // We don't want to modify the input so we must copy it.
        int len = length;

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
