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

import java.io.File;
import java.util.concurrent.locks.ReadWriteLock;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;

/**
 * Tools for getting files to store data in the filesystem. These APIs are in flux and may change at any time without
 * warning. This should be replaced by a module which provides a secure extension of java.io.File.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Role
public interface FilesystemStoreTools
{
    /**
     * Get a backup file which for a given storage file. This file name will never collide with any other file gotten
     * through this interface.
     *
     * @param storageFile the file to get a backup file for.
     * @return a backup file with a name based on the name of the given file.
     */
    File getBackupFile(File storageFile);

    /**
     * Get a temporary file which for a given storage file. This file name will never collide with any other file gotten
     * through this interface.
     *
     * @param storageFile the file to get a temporary file for.
     * @return a temporary file with a name based on the name of the given file.
     */
    File getTempFile(File storageFile);

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment in a separate
     * location which is repeatable only with the same attachment name, and containing document.
     *
     * @param attachmentReference the reference attachment to get a tools for.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     * @since 9.10RC1
     */
    AttachmentFileProvider getAttachmentFileProvider(AttachmentReference attachmentReference);

    /**
     * @param attachmentReference the reference of the attachment
     * @return true if the attachment content exist in the store
     * @since 9.10RC1
     */
    boolean attachmentContentExist(AttachmentReference attachmentReference);

    /**
     * @param attachmentReference the reference of the attachment
     * @return true if the attachment archive exist in the store
     * @since 9.10RC1
     */
    boolean attachmentArchiveExist(AttachmentReference attachmentReference);

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment in a separate
     * location which is repeatable only with the same attachment name, containing document, and date of deletion.
     *
     * @param attachmentReference the reference of the attachment
     * @param index the index of the deleted attachment.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     * @since 9.10RC1
     */
    DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(AttachmentReference attachmentReference, long index);

    /**
     * Get an instance of DeletedDocumentFileProvider which will save everything to do with an document in a separate
     * location which is repeatable only with the same document reference, and date of deletion.
     *
     * @param documentReference the reference of the document.
     * @param index the index of the deleted document.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     * @since 9.0RC1
     */
    DeletedDocumentContentFileProvider getDeletedDocumentFileProvider(DocumentReference documentReference, long index);

    /**
     * @return the absolute path to the directory where the files are stored.
     */
    String getStorageLocationPath();

    /**
     * @return the absolute path to the directory where the files are stored.
     * @since 9.10RC1
     */
    File getStorageLocationFile();

    /**
     * Get a file which is global for the entire installation.
     *
     * @param name a unique identifier for the file.
     * @return a file unique to the given name.
     */
    File getGlobalFile(String name);

    /**
     * Get a {@link java.util.concurrent.locks.ReadWriteLock} which is unique to the given file. This method will always
     * return the same lock for the path on the filesystem even if the {@link java.io.File} object is different.
     *
     * @param toLock the file to get a lock for.
     * @return a lock for the given file.
     */
    ReadWriteLock getLockForFile(File toLock);
}
