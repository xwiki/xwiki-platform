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
package com.xpn.xwiki.doc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.ProxyOutputStream;
import org.xwiki.environment.Environment;
import org.xwiki.store.UnexpectedException;

import com.xpn.xwiki.web.Utils;

/**
 * The content of an attachment. Objects of this class hold the actual content which will be downloaded when a user
 * downloads an attachment.
 *
 * @version $Id$
 */
public class XWikiAttachmentContent implements Cloneable
{
    /** An empty byte array returned for empty attachment contents. */
    private static final byte[] NULLFILE = new byte[0];

    /** The XWikiAttachment (attachment metadata) which this attachment content is associated with. */
    private XWikiAttachment attachment;

    /** True if the content is out of sync with the content stored in the database and thus needs to be saved. */
    private boolean isContentDirty;

    /** Storage which holds the actual content. */
    // TODO: use TemporaryDeferredFileRepository instead (see DeletedAttachment)
    private FileItem file;

    /** The owner document. */
    private XWikiDocument ownerDocument;

    /**
     * Constructor which clones an existing XWikiAttachmentContent. Used by {@link #clone()}.
     *
     * @param original the XWikiAttachmentContent to clone.
     * @since 2.6M1
     */
    public XWikiAttachmentContent(XWikiAttachmentContent original)
    {
        this.file = original.file;
        this.attachment = original.attachment;
        this.isContentDirty = original.isContentDirty;
        this.ownerDocument = original.ownerDocument;
    }

    /**
     * Constructor with associated attachment specified.
     *
     * @param attachment the attachment which this is the content for.
     */
    public XWikiAttachmentContent(XWikiAttachment attachment)
    {
        setAttachment(attachment);
    }

    /**
     * The default Constructor. For creating content which will be associated with an attachment later.
     */
    public XWikiAttachmentContent()
    {
    }

    // Used in FilesystemAttachmentContent.clone()
    // since 5.2M1
    protected XWikiAttachmentContent(XWikiAttachment attachment, FileItem f)
    {
        this.setAttachment(attachment);
        this.file = f;
    }
    
    /**
     * @return the underlying storage file.
     * @since 5.2M1
     */
    protected FileItem getFileItem()
    {
        return this.file;
    }

    /**
     * @return a new FileItem for temporarily storing attachment content.
     * @since 4.2M3
     */
    private static FileItem getNewFileItem()
    {
        final Environment env = Utils.getComponent(Environment.class);
        final File dir = new File(env.getTemporaryDirectory(), "attachment-cache");
        try {
            if (!dir.mkdirs() && !dir.exists()) {
                throw new UnexpectedException("Failed to create directory for attachments " + dir);
            }
            final DiskFileItem dfi = new DiskFileItem(null, null, false, null, 10000, dir);
            // This causes the temp file to be created.
            dfi.getOutputStream().close();
            return dfi;
        } catch (IOException e) {
            throw new UnexpectedException("Failed to create new attachment temporary file.", e);
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
        if (this.file == null) {
            return NULLFILE;
        }
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

            setContent(new ByteArrayInputStream(internalContent));
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

        if (this.attachment != null) {
            setOwnerDocument(this.attachment.getDoc());
        }
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
        if (contentDirty && this.ownerDocument != null) {
            this.ownerDocument.setMetaDataDirty(contentDirty);
        }
    }

    /**
     * @return true of the content of this attachment still exist in the store
     * @since 13.8RC1
     * @since 13.4.4
     * @since 12.10.10
     */
    public boolean exists()
    {
        return true;
    }

    /**
     * @return an InputStream to read the binary content of this attachment.
     * @since 2.3M2
     */
    public InputStream getContentInputStream()
    {
        if (this.file == null) {
            return new ByteArrayInputStream(NULLFILE);
        }
        try {
            return new AutoCloseInputStream(this.file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to get InputStream", e);
        }
    }

    /**
     * Set the content of the attachment by writing to a provided OutputStream. Content is *not* appended, this method
     * clears the content and creates new content. If you want to append content, you can call
     * {@link #getContentInputStream()} and copy the content of that into the provided OutputStream. Before closing this
     * OutputStream the content will remain the old content prior to the change.
     *
     * @return an OutputStream into which the caller can set the content of the attachments.
     * @since 4.2M3
     */
    public OutputStream getContentOutputStream()
    {
        final FileItem fi = getNewFileItem();
        final XWikiAttachmentContent xac = this;
        final OutputStream fios;
        try {
            fios = fi.getOutputStream();
        } catch (IOException e) {
            // DiskFileItem does not do anything which could cause an exception to be thrown.
            // so unless it is modified, this should not happen.
            throw new RuntimeException("Exception getting attachment OutputStream.", e);
        }
        return (new ProxyOutputStream(fios)
        {
            @Override
            public void close() throws IOException
            {
                super.close();
                xac.file = fi;
                xac.setContentDirty(true);
                if (xac.attachment != null) {
                    xac.attachment.setLongSize(xac.getLongSize());
                }
            }
        });
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
        this.setContent(new BoundedInputStream(is, len));
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
        OutputStream fios = getContentOutputStream();
        try {
            IOUtils.copy(is, fios);
        } finally {
            fios.close();
        }

        // Indicate the content has been modified
        setContentDirty(true);
    }

    /**
     * @return the true size of the content of the attachment.
     * @since 2.3M2
     * @deprecated since 9.0RC1, use {@link #getLongSize()} instead
     */
    @Deprecated
    public int getSize()
    {
        long longSize = getLongSize();

        return longSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) longSize;
    }

    /**
     * @return the true size of the content of the attachment. -1 if the size is unknown.
     * @since 9.0RC1
     */
    public long getLongSize()
    {
        return this.file != null ? this.file.getSize() : -1;
    }

    /**
     * Set the owner document in order to propagate the content dirty flag.
     *
     * @param ownerDocument the owner document.
     */
    public void setOwnerDocument(XWikiDocument ownerDocument)
    {
        if (this.ownerDocument != ownerDocument) {
            this.ownerDocument = ownerDocument;
            if (this.isContentDirty && ownerDocument != null) {
                ownerDocument.setMetaDataDirty(true);
            }
        }
    }

    /**
     * Delete the temporary file item created to hold the content of the attachment.
     * This method only performs the deletion if its content is dirty, its purpose is mainly to allow cleaning temporary
     * attachments.
     *
     * @since 14.3RC1
     */
    public void dispose()
    {
        if (this.file != null && this.isContentDirty()) {
            this.file.delete();
            this.file = null;
        }
    }
}
