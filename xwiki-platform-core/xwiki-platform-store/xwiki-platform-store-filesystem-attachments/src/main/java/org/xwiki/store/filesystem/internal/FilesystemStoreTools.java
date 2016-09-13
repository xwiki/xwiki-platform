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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Tools for getting files to store data in the filesystem.
 * These APIs are in flux and may change at any time without warning.
 * This should be replaced by a module which provides a secure extension of java.io.File.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Role
public interface FilesystemStoreTools
{
    /**
     * Get a backup file which for a given storage file.
     * This file name will never collide with any other file gotten through this interface.
     *
     * @param storageFile the file to get a backup file for.
     * @return a backup file with a name based on the name of the given file.
     */
    File getBackupFile(File storageFile);

    /**
     * Get a temporary file which for a given storage file.
     * This file name will never collide with any other file gotten through this interface.
     *
     * @param storageFile the file to get a temporary file for.
     * @return a temporary file with a name based on the name of the given file.
     */
    File getTempFile(File storageFile);

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment
     * in a separate location which is repeatable only with the same attachment name, and containing
     * document.
     *
     * @param attachment the attachment to get a tools for.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     */
    AttachmentFileProvider getAttachmentFileProvider(XWikiAttachment attachment);

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment
     * in a separate location which is repeatable only with the same attachment name, containing document,
     * and date of deletion.
     *
     * @param attachment the attachment to get a tools for.
     * @param deleteDate the date the attachment was deleted.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     */
    DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(XWikiAttachment attachment, Date deleteDate);

    /**
     * Get a map of dates of deletion by the document where the attachment was attached.
     *
     * @param docRef a reference to the document to get deleted attachments for.
     * @return a map of maps which provide FileProviders by deletion dates and filenames.
     */
    Map<String, Map<Date, DeletedAttachmentFileProvider>> deletedAttachmentsForDocument(DocumentReference docRef);

    /**
     * @return the absolute path to the directory where the files are stored.
     */
    String getStorageLocationPath();

    /**
     * Get a file which is global for the entire installation.
     *
     * @param name a unique identifier for the file.
     * @return a file unique to the given name.
     */
    File getGlobalFile(String name);

    /**
     * Get a deleted attachment file provider from a path to the deleted attachment directory.
     *
     * @param pathToDirectory a relitive path to the directory where the deleted attachment is.
     * @return a DeletedAttachmentFileProvider which will provide files for that deleted attachment.
     */
    DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(String pathToDirectory);

    /**
     * Get a {@link java.util.concurrent.locks.ReadWriteLock} which is unique to the given file.
     * This method will always return the same lock for the path on the filesystem even if the
     * {@link java.io.File} object is different.
     *
     * @param toLock the file to get a lock for.
     * @return a lock for the given file.
     */
    ReadWriteLock getLockForFile(File toLock);
}
