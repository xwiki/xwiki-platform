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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;

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
     * A lock which is locked when the attachment content is being read.
     */
    private final ReadWriteLock lock;

    /**
     * The Constructor.
     *
     * @param storage the file where the data is stored.
     * @param attachment the attachment to associate this content with.
     * @param lock this will be locked for reading when the attachment file is being read.
     */
    public FilesystemAttachmentContent(final File storage,
        final XWikiAttachment attachment,
        final ReadWriteLock lock)
    {
        // TODO This will cause a new FileItem to be created in XWikiAttachmentContent
        // but it is the only constructor available. This should be fixed in XAC.
        super(attachment);

        this.storageFile = storage;
        this.lock = lock;
    }

    @Override
    public FilesystemAttachmentContent clone()
    {
        return new FilesystemAttachmentContent(this.storageFile, this.getAttachment(), this.lock);
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
        /** An InputStream which locks a lock while it is being read. */
        final class LockingFileInputStream extends FileInputStream
        {
            /** The lock to lock while reading the file. */
            private Lock lock;

            /**
             * The Constructor.
             *
             * @param toRead the file for this stream to read.
             * @param lock the lock to lock on creation of the stream and unlock when it is closed.
             * @throws IOException if the extended FileInputStream throws one.
             */
            public LockingFileInputStream(final File toRead, final Lock lock) throws IOException
            {
                super(toRead);
                this.lock = lock;
                lock.lock();
            }

            /** {@inheritDoc} */
            public void close() throws IOException
            {
                // Make sure this only happens once.
                if (this.lock != null) {
                    super.close();
                    this.lock.unlock();
                    this.lock = null;
                }
            }
        }

        try {
            return new AutoCloseInputStream(
                new LockingFileInputStream(this.storageFile, this.lock.readLock()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to get InputStream", e);
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
