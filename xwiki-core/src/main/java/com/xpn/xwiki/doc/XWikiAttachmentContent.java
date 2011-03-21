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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;

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

    /** True if the content is out of sync with the content stored in the database and thus needs to be saved. */
    private boolean isContentDirty;

    /** Storage which holds the actual content. */
    private FileItem file;

    /**
     * Constructor which clones an existing XWikiAttachmentContent.
     * used by {@link #clone()}.
     *
     * @param original the XWikiAttachmentContent to clone.
     * @since 2.6M1
     */
    public XWikiAttachmentContent(XWikiAttachmentContent original)
    {
        this.file = original.file;
        this.attachment = original.attachment;
    }

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
        this.newFileItem();
    }

    /**
     * Set a new FileItem for storage.
     * @since 2.6M1
     */
    private void newFileItem()
    {
        String tempFileLocation = System.getProperty("java.io.tmpdir");
        // TODO try to get a different temp file location.
        try {
            final DiskFileItem dfi = new DiskFileItem(null, null, false, null, 10000, new File(tempFileLocation));
            // This causes the temp file to be created.
            dfi.getOutputStream();
            // Make sure this file is marked for deletion on VM exit because DiskFileItem does not.
            dfi.getStoreLocation().deleteOnExit();
            this.file = dfi;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create new attachment temporary file."
                                       + " Are you sure you have permission to write to "
                                       + tempFileLocation + "?", e);
        }
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
        return new XWikiAttachmentContent(this);
    }

    /**
     * @return a byte array containing the binary content of the attachment.
     * @deprecated use {@link #getContentInputStream()} instead
     */
    @Deprecated
    public byte[] getContent()
    {
        return this.file.get();
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
        try {
            byte[] internalContent = new byte[0];
            if (content != null) {
                internalContent = content;
            }

            this.setContent(new ByteArrayInputStream(internalContent));
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy data to storage.", e);
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
        try {
            return new AutoCloseInputStream(this.file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to get InputStream", e);
        }
    }

    /**
     * Set the content of the attachment from a portion of an InputStream.
     * 
     * @param is the input stream that will be read
     * @param len the number of bytes to read from the beginning of the stream
     * @throws IOException when an error occurs during streaming operation
     * @since 2.3M2
     */
    public void setContent(InputStream is, int len) throws IOException
    {
        // TODO Fix so this sends a EOS when the limit is reached.
        //this.setContent(new LimitedInputStream(is, ((long) len)));
        this.setContent(is);
    }

    /**
     * Set the content of the attachment from an InputStream.
     * 
     * @param is the input stream that will be read
     * @throws IOException when an error occurs during streaming operation
     * @since 2.6M1
     */
    public void setContent(InputStream is) throws IOException
    {
        this.newFileItem();
        IOUtils.copy(is, this.file.getOutputStream());
        this.setContentDirty(true);

        this.attachment.setFilesize(this.getSize());
    }

    /**
     * @return the true size of the content of the attachment.
     * @since 2.3M2
     */
    public int getSize()
    {
        return (int) this.file.getSize();
    }
}
