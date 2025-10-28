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
import org.xwiki.store.blob.BlobStoreException;

/**
 * A means of getting files for storing information about a given attachment.
 *
 * @version $Id$
 * @since 3.0M2
 */
public interface AttachmentFileProvider
{
    /**
     * @return the Blob for storing the latest version of the attachment's content.
     */
    Blob getAttachmentContentFile() throws BlobStoreException;

    /**
     * Get the meta blob for the attachment. The meta file contains information about each version of the attachment
     * such as who saved it.
     *
     * @return the Blob for storing meta data for each version of an attachment.
     */
    Blob getAttachmentVersioningMetaFile() throws BlobStoreException;

    /**
     * Get a uniquely named Blob for storing a particular version of the attachment.
     *
     * @param versionName the name of the version of the attachment eg: "1.1" or "1.2"
     * @return the Blob for storing the content of a particular version of the attachment.
     */
    Blob getAttachmentVersionContentFile(String versionName) throws BlobStoreException;
}
