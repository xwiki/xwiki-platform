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

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.xwiki.store.UnexpectedException;

/**
 * The content of an attachment. This implementation is based on a file on the filesystem.
 * This implementation is immutable and is only created by the
 * {@link com.xpn.xwiki.store.FilesystemAttachmentStore}.
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
        // TODO This will cause a new FileItem to be created in XWikiAttachmentContent
        // but it is the only constructor available. This should be fixed in XAC.
        super(attachment);
        this.storageFile = storage;
    }

    @Override
    public FilesystemAttachmentContent clone()
    {
        return new FilesystemAttachmentContent(this.storageFile, this.getAttachment());
    }

    @Override
    @Deprecated
    public byte[] getContent()
    {
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
        try {
            return new AutoCloseInputStream(new FileInputStream(this.storageFile));
        } catch (IOException e) {
            throw new UnexpectedException("Failed to get InputStream", e);
        }
    }

    @Override
    public void setContent(final InputStream is) throws IOException
    {
        // This should be immutable but XWikiAttachment calls this when an attachment is being
        // saved which already exists. Disconnect this content from the attachment since it
        // might be being used by another instance of the same attachment, then let the attachment
        // create a new XWikiAttachmentContent instance with a new file.
        this.getAttachment().setAttachment_content(null);
        this.getAttachment().setContent(is);
    }

    @Override
    public int getSize()
    {
        long size = this.storageFile.length();
        // The most important thing is that it doesn't roll over into the negative space.
        if (size > ((long) Integer.MAX_VALUE)) {
            return Integer.MAX_VALUE;
        }
        return (int) size;
    }

    /*
     * Despite being an immutable implementation, setContentDirty(boolean) is not overridden,
     * this is because there is the possibility that the attachment will be moved from one
     * document to another, from versioning store to main store, or from deleted attachments to
     * main store and the core uses setContentDirty and isContentDirty as a way to signal
     * that there is reason to want to save the attachment.
     */
}
