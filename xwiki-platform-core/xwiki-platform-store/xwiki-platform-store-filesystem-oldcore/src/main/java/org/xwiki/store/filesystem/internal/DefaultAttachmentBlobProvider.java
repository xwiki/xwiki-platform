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
package org.xwiki.store.filesystem.internal;

import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;

/**
 * A means of getting files for storing information about a given attachment.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class DefaultAttachmentBlobProvider implements AttachmentBlobProvider
{
    /**
     * This stores the attachment metadata for each revision of the attachment in XML format.
     *
     * @see #getAttachmentVersioningMetaBlob()
     */
    private static final String ATTACH_ARCHIVE_META_FILENAME = "~METADATA.xml";

    /**
     * The blob store where the attachment information is stored.
     */
    protected final BlobStore store;

    /**
     * The directory where all information about this attachment resides.
     */
    private final BlobPath attachmentDir;

    /**
     * The name of the attached file.
     */
    private final String attachmentFileName;

    /**
     * The Constructor.
     *
     * @param store the blob store where the attachment information is stored.
     * @param attachmentDir a directory where all information about this attachment is stored.
     * @param fileName the name of the attachment file.
     */
    public DefaultAttachmentBlobProvider(BlobStore store, final BlobPath attachmentDir, final String fileName)
    {
        this.store = store;
        this.attachmentDir = attachmentDir;
        this.attachmentFileName = fileName;
    }

    /**
     * @return the directory where information about this attachment is stored.
     */
    protected BlobPath getAttachmentDir()
    {
        return this.attachmentDir;
    }

    /**
     * @return the name of this attachment.
     */
    protected String getAttachmentFileName()
    {
        return this.attachmentFileName;
    }

    @Override
    public Blob getAttachmentContentBlob() throws BlobStoreException
    {
        return this.store.getBlob(
            this.attachmentDir.resolve(StoreFileUtils.getStoredFilename(this.attachmentFileName, null)));
    }

    @Override
    public Blob getAttachmentVersioningMetaBlob() throws BlobStoreException
    {
        return this.store.getBlob(this.attachmentDir.resolve(ATTACH_ARCHIVE_META_FILENAME));
    }

    @Override
    public Blob getAttachmentVersionContentBlob(final String versionName) throws BlobStoreException
    {
        return this.store.getBlob(
            this.attachmentDir.resolve(StoreFileUtils.getStoredFilename(this.attachmentFileName, versionName)));
    }
}
