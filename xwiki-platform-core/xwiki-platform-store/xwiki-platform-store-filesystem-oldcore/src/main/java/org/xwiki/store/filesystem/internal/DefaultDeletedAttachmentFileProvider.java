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

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * A means of getting files for storing information about a given deleted attachment.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class DefaultDeletedAttachmentFileProvider extends DefaultAttachmentFileProvider
    implements DeletedAttachmentFileProvider
{
    /**
     * This stores the metadata for the deleted attachment such as who deleted it.
     *
     * @see #metaFileForDeletedAttachment(XWikiAttachment, Date)
     */
    private static final String DELETED_ATTACH_META_FILENAME = "~DELETED_ATTACH_METADATA.xml";

    /**
     * The Constructor.
     *
     * @param store the blob store where the attachment information is stored.
     * @param attachmentDir the location where the information about the deleted attachment will be stored.
     * @param fileName the name of the attachment file.
     */
    public DefaultDeletedAttachmentFileProvider(BlobStore store, final BlobPath attachmentDir, final String fileName)
    {
        super(store, attachmentDir, fileName);
    }

    @Override
    public Blob getDeletedAttachmentMetaFile() throws BlobStoreException
    {
        return this.store.getBlob(this.getAttachmentDir().resolve(DELETED_ATTACH_META_FILENAME));
    }
}
