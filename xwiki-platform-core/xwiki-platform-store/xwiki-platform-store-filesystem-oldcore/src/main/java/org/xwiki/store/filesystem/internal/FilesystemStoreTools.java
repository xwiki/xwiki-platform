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
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.internal.FileSystemStoreUtils;
import org.xwiki.store.locks.LockProvider;

/**
 * Default tools for getting files to store data in the filesystem. This should be replaced by a module which provides a
 * secure extension of java.io.File.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component(roles = FilesystemStoreTools.class)
@Singleton
public class FilesystemStoreTools implements Initializable
{
    /**
     * The name of the directory where document information is stored. This must have a URL illegal character in it,
     * otherwise it will be confused if/when nested spaces are implemented.
     * 
     * @since 10.0
     */
    public static final String DOCUMENT_DIR_NAME = "~this";

    /**
     * The directory within each document's directory for document locales.
     * 
     * @since 10.0
     */
    public static final String DOCUMENT_LOCALES_DIR_NAME = "locales";

    /**
     * The folder name of {@link Locale#ROOT}.
     * 
     * @since 10.0
     */
    public static final String DOCUMENT_LOCALES_ROOT_NAME = "~";

    /**
     * The name of the directory where document locale information is stored. This must have a URL illegal character in
     * it, otherwise it will be confused if/when nested spaces are implemented.
     */
    public static final String DOCUMENTLOCALE_DIR_NAME = DOCUMENT_DIR_NAME;

    /**
     * The directory within each document's directory where the document's attachments are stored.
     */
    public static final String ATTACHMENT_DIR_NAME = "attachments";

    /**
     * The name of the directory in the work directory where the hirearchy will be stored.
     */
    private static final String STORAGE_DIR_NAME = "storage";

    /**
     * The directory within each document's directory for attachments which have been deleted.
     */
    private static final String DELETED_ATTACHMENT_DIR_NAME = "deleted-attachments";

    /**
     * The part of the deleted attachment directory name after this is the date of deletion, The part before this is the
     * URL encoded attachment filename.
     */
    private static final String DELETED_ATTACHMENT_NAME_SEPARATOR = "-id";

    /**
     * The directory within each document's directory for documents which have been deleted.
     */
    private static final String DELETED_DOCUMENT_DIR_NAME = "deleted-documents";

    /**
     * When a file is being saved, the original will be moved to the same name with this after it. If the save operation
     * fails then this file will be moved back to the regular position to come as close as possible to ACID transaction
     * handling.
     */
    private static final String BACKUP_FILE_SUFFIX = "~bak";

    /**
     * When a file is being deleted, it will be renamed with this at the end of the filename in the transaction. If the
     * transaction succeeds then the temp file will be deleted, if it fails then the temp file will be renamed back to
     * the original filename.
     */
    private static final String TEMP_FILE_SUFFIX = "~tmp";

    /**
     * Serializer used for obtaining a safe file path from a document reference.
     */
    @Inject
    @Named(FileSystemStoreUtils.HINT)
    private EntityReferenceSerializer<String> fileEntitySerializer;

    @Inject
    private FilesystemAttachmentsConfiguration config;

    /**
     * A means of acquiring locks for attachments. Because the attachments temp files are randomly named and rename is
     * atomic, locks are not needed. DummyLockProvider provides fake locks.
     */
    @Inject
    @Named("dummy")
    private LockProvider lockProvider;

    /**
     * Used to get store directory.
     */
    @Inject
    private Environment environment;

    /**
     * This is the directory where all of the attachments will stored.
     */
    private File storageDir;

    /**
     * Testing Constructor.
     *
     * @param pathSerializer an EntityReferenceSerializer for generating file paths.
     * @param storageDir the directory to store the content in.
     * @param lockProvider a means of getting locks for making sure only one thread accesses an attachment at a time.
     */
    public FilesystemStoreTools(final EntityReferenceSerializer<String> pathSerializer, final File storageDir,
        final LockProvider lockProvider)
    {
        this.fileEntitySerializer = pathSerializer;
        this.storageDir = storageDir;
        this.lockProvider = lockProvider;
    }

    /**
     * Constructor for component manager.
     */
    public FilesystemStoreTools()
    {
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.storageDir = new File(this.environment.getPermanentDirectory(), STORAGE_DIR_NAME).getCanonicalFile();
        } catch (IOException e) {
            throw new InitializationException("Invalid permanent directory", e);
        }

        if (config.cleanOnStartup()) {
            final File dir = this.storageDir;

            new Thread(() -> deleteEmptyDirs(dir, 0)).start();
        }
    }

    /**
     * Delete all empty directories under the given directory. A directory which contains only empty directories is also
     * considered an empty ditectory. This function will not delete *location* unless depth is non-zero.
     *
     * @param location a directory to delete.
     * @param depth used for recursion, should always be zero.
     * @return true if the directory existed, was empty and was deleted.
     */
    private static boolean deleteEmptyDirs(final File location, int depth)
    {
        if (location != null && location.exists() && location.isDirectory()) {
            final File[] dirs = location.listFiles();
            boolean empty = true;
            for (int i = 0; i < dirs.length; i++) {
                if (!deleteEmptyDirs(dirs[i], depth + 1)) {
                    empty = false;
                }
            }

            if (empty && depth != 0) {
                location.delete();

                return true;
            }
        }

        return false;
    }

    /**
     * Get a backup file which for a given storage file. This file name will never collide with any other file gotten
     * through this interface.
     *
     * @param storageFile the file to get a backup file for.
     * @return a backup file with a name based on the name of the given file.
     */
    public File getBackupFile(final File storageFile)
    {
        // We pad our file names with random alphanumeric characters so that multiple operations on the same
        // file in the same transaction do not collide, the set of all capital and lower case letters
        // and numbers has 62 possibilities and 62^8 = 218340105584896 between 2^47 and 2^48.
        return new File(storageFile.getAbsolutePath() + BACKUP_FILE_SUFFIX + RandomStringUtils.randomAlphanumeric(8));
    }

    /**
     * Get a temporary file which for a given storage file. This file name will never collide with any other file gotten
     * through this interface.
     *
     * @param storageFile the file to get a temporary file for.
     * @return a temporary file with a name based on the name of the given file.
     */
    public File getTempFile(final File storageFile)
    {
        return new File(storageFile.getAbsolutePath() + TEMP_FILE_SUFFIX + RandomStringUtils.randomAlphanumeric(8));
    }

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment in a separate
     * location which is repeatable only with the same attachment name, containing document, and date of deletion.
     *
     * @param attachment the reference of the attachment
     * @param index the index of the deleted attachment.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     * @since 9.10RC1
     */
    public DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(final AttachmentReference attachment,
        final long index)
    {
        return new DefaultDeletedAttachmentFileProvider(getDeletedAttachmentDir(attachment, index),
            attachment.getName());
    }

    /**
     * Get an instance of DeletedDocumentFileProvider which will save everything to do with an document in a separate
     * location which is repeatable only with the same document reference, and date of deletion.
     *
     * @param documentReference the reference of the document.
     * @param index the index of the deleted document.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     * @since 9.0RC1
     */
    public DeletedDocumentContentFileProvider getDeletedDocumentFileProvider(DocumentReference documentReference,
        long index)
    {
        return new DefaultDeletedDocumentContentFileProvider(getDeletedDocumentContentDir(documentReference, index));
    }

    /**
     * @return the absolute path to the directory where the files are stored.
     */
    public String getStorageLocationPath()
    {
        return this.storageDir.getPath();
    }

    /**
     * @return the absolute path to the directory where the files are stored.
     * @since 9.10RC1
     */
    public File getStorageLocationFile()
    {
        return this.storageDir;
    }

    /**
     * Get a file which is global for the entire installation.
     *
     * @param name a unique identifier for the file.
     * @return a file unique to the given name.
     */
    public File getGlobalFile(final String name)
    {
        return new File(this.storageDir, "~GLOBAL_" + FileSystemStoreUtils.encode(name, false));
    }

    /**
     * Get an instance of AttachmentFileProvider which will save everything to do with an attachment in a separate
     * location which is repeatable only with the same attachment name, and containing document.
     *
     * @param attachmentReference the reference attachment to get a tools for.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     * @since 9.10RC1
     */
    public AttachmentFileProvider getAttachmentFileProvider(final AttachmentReference attachmentReference)
    {
        return new DefaultAttachmentFileProvider(getAttachmentDir(attachmentReference), attachmentReference.getName());
    }

    private File getAttachmentDir(final AttachmentReference attachmentReference)
    {
        final File docDir = getDocumentContentDir(attachmentReference.getDocumentReference());
        final File attachmentsDir = new File(docDir, ATTACHMENT_DIR_NAME);

        return new File(attachmentsDir, FileSystemStoreUtils.encode(attachmentReference.getName(), true));
    }

    /**
     * Get a directory for storing the contents of a deleted attachment. The format is {@code <document
     * name>/~this/deleted-attachments/<attachment name>-<delete date>/} {@code <delete date>} is expressed in "unix
     * time" so it might look like: {@code WebHome/~this/deleted-attachments/file.txt-0123456789/}
     *
     * @param attachment the attachment to get the file for.
     * @param index the index of the deleted attachment.
     * @return a directory which will be repeatable only with the same inputs.
     */
    private File getDeletedAttachmentDir(final AttachmentReference attachment, final long index)
    {
        final DocumentReference doc = attachment.getDocumentReference();
        final File docDir = getDocumentContentDir(doc);
        final File deletedAttachmentsDir = new File(docDir, DELETED_ATTACHMENT_DIR_NAME);
        final String fileName = attachment.getName() + DELETED_ATTACHMENT_NAME_SEPARATOR + index;
        return new File(deletedAttachmentsDir, FileSystemStoreUtils.encode(fileName, false));
    }

    /**
     * Get a directory for storing the content of a deleted document. The format is {@code <document
     * name>/~this/deleted-documents/<index>/content.xml}.
     *
     * @param documentReference the document to get the file for.
     * @param index the index of the deleted document.
     * @return a directory which will be repeatable only with the same inputs.
     */
    private File getDeletedDocumentContentDir(final DocumentReference documentReference, final long index)
    {
        final File docDir = getDocumentContentDir(documentReference);
        final File deletedDocumentContentsDir = new File(docDir, DELETED_DOCUMENT_DIR_NAME);
        return new File(deletedDocumentContentsDir, String.valueOf(index));
    }

    /**
     * Get the directory associated with this document. This is a path obtained from the owner document reference, where
     * each reference segment (wiki, spaces, document name) contributes to the final path. For a document called
     * xwiki:Main.WebHome, the directory will be: <code>(storageDir)/xwiki/Main/WebHome/~this/</code>
     *
     * @param documentReference the DocumentReference for the document to get the directory for.
     * @return a file path corresponding to the attachment location; each segment in the path is URL-encoded in order to
     *         be safe.
     */
    private File getDocumentContentDir(final DocumentReference documentReference)
    {
        final File documentDir =
            new File(this.storageDir, this.fileEntitySerializer.serialize(documentReference, true));
        File documentContentDir = new File(documentDir, DOCUMENT_DIR_NAME);

        // Add the locale
        Locale documentLocale = documentReference.getLocale();
        if (documentLocale != null) {
            final File documentLocalesDir = new File(documentContentDir, DOCUMENT_LOCALES_DIR_NAME);
            final File documentLocaleDir = new File(documentLocalesDir,
                documentLocale.equals(Locale.ROOT) ? DOCUMENT_LOCALES_ROOT_NAME : documentLocale.toString());
            documentContentDir = new File(documentLocaleDir, DOCUMENTLOCALE_DIR_NAME);
        }

        return documentContentDir;
    }

    /**
     * Get a {@link java.util.concurrent.locks.ReadWriteLock} which is unique to the given file. This method will always
     * return the same lock for the path on the filesystem even if the {@link java.io.File} object is different.
     *
     * @param toLock the file to get a lock for.
     * @return a lock for the given file.
     */
    public ReadWriteLock getLockForFile(final File toLock)
    {
        return this.lockProvider.getLock(toLock);
    }
}
