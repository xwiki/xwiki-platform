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
package org.xwiki.store.legacy.doc.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.xwiki.store.UnexpectedException;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;

/**
 * The content of an attachment. This implementation is based on a file on the filesystem. This implementation is
 * mutable but the underlying file is left alone.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class FilesystemAttachmentContent extends XWikiAttachmentContent
{
    /**
     * The underlying storage mechanism.
     */
    private final File storageFile;

    /**
     * The Constructor.
     *
     * @param storage the file where the data is stored.
     * @param attachment the attachment to associate this content with.
     */
    public FilesystemAttachmentContent(final File storage, final XWikiAttachment attachment)
    {
        super(attachment, null);
        this.storageFile = storage;
    }

    /**
     * Constructor that doesn't set the attachment. This is necessary for loading attachment content without touching
     * the dirty bit of the owner document.
     *
     * @param storage the file where the data is stored.
     * @since 4.4RC1
     */
    public FilesystemAttachmentContent(final File storage)
    {
        super(null, null);
        this.storageFile = storage;
    }

    /**
     * Clone this {@link FilesystemAttachmentContent} instance.
     * 
     * @param filesystemAttachmentContent the content to copy
     * @since 10.2
     * @since 9.11.4
     */
    public FilesystemAttachmentContent(FilesystemAttachmentContent filesystemAttachmentContent)
    {
        super(filesystemAttachmentContent);

        this.storageFile = filesystemAttachmentContent.storageFile;
    }

    @Override
    public FilesystemAttachmentContent clone()
    {
        return new FilesystemAttachmentContent(this);
    }

    @Override
    public boolean exists()
    {
        return this.storageFile.exists();
    }

    @Override
    @Deprecated
    public byte[] getContent()
    {
        if (this.getFileItem() != null) {
            return super.getContent();
        }

        final InputStream is = this.getContentInputStream();
        try {
            return IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load attachment content", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public InputStream getContentInputStream()
    {
        if (this.getFileItem() != null) {
            return super.getContentInputStream();
        }

        try {
            return new AutoCloseInputStream(new FileInputStream(this.storageFile));
        } catch (IOException e) {
            throw new UnexpectedException("Failed to get InputStream", e);
        }
    }

    @Override
    public long getLongSize()
    {
        if (this.getFileItem() != null) {
            return super.getLongSize();
        }

        return this.storageFile.length();
    }
}
