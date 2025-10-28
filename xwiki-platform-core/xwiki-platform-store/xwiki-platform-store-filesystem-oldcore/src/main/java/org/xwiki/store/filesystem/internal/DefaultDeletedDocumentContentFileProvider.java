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
 * A means of getting files for storing information about a given deleted document.
 *
 * @version $Id$
 * @since 9.0RC1
 */
public class DefaultDeletedDocumentContentFileProvider implements DeletedDocumentContentFileProvider
{
    /**
     * The file holding the content of the deleted document.
     */
    private static final String DELETED_DOCUMENT_FILE_NAME = "content.xml";

    /**
     * The blob store where the document information is stored.
     */
    protected final BlobStore store;

    /**
     * The directory where all information about this deleted document resides.
     */
    private final BlobPath deletedDocumentDir;

    /**
     * @param store the blob store where the document information is stored.
     * @param deletedDocumentDir the location where the information about the deleted document will be stored.
     */
    public DefaultDeletedDocumentContentFileProvider(BlobStore store, final BlobPath deletedDocumentDir)
    {
        this.store = store;
        this.deletedDocumentDir = deletedDocumentDir;
    }

    @Override
    public Blob getDeletedDocumentContentBlob() throws BlobStoreException
    {
        return this.store.getBlob(this.deletedDocumentDir.resolve(DELETED_DOCUMENT_FILE_NAME));
    }
}
