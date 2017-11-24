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
import org.xwiki.store.locks.LockProvider;

/**
 * Default tools for getting files to store data in the filesystem. This should be replaced by a module which provides a
 * secure extension of java.io.File.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component
@Singleton
public class DefaultFilesystemStoreTools implements FilesystemStoreTools, Initializable
{
    /**
     * The name of the directory in the work directory where the hirearchy will be stored.
     */
    private static final String STORAGE_DIR_NAME = "storage";

    /**
     * The name of the directory where document information is stored. This must have a URL illegal character in it,
     * otherwise it will be confused if/when nested spaces are implemented.
     */
    private static final String DOCUMENT_DIR_NAME = "~this";

    /**
     * The name of the directory where document locale information is stored. This must have a URL illegal character in
     * it, otherwise it will be confused if/when nested spaces are implemented.
     * 
     * @since 9.0RC1
     */
    private static final String DOCUMENTLOCALE_DIR_NAME = DOCUMENT_DIR_NAME;

    /**
     * The directory within each document's directory where the document's attachments are stored.
     */
    private static final String ATTACHMENT_DIR_NAME = "attachments";

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
     * The directory within each document's directory for document locales.
     * 
     * @since 9.0RC1
     */
    private static final String DOCUMENT_LOCALES_DIR_NAME = "locales";

    /**
     * The folder name of {@link Locale#ROOT}.
     * 
     * @since 9.0RC1
     */
    private static final String DOCUMENT_LOCALES_ROOT_NAME = "~";

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
    @Named("path")
    private EntityReferenceSerializer<String> pathSerializer;

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
    public DefaultFilesystemStoreTools(final EntityReferenceSerializer<String> pathSerializer, final File storageDir,
        final LockProvider lockProvider)
    {
        this.pathSerializer = pathSerializer;
        this.storageDir = storageDir;
        this.lockProvider = lockProvider;
    }

    /**
     * Constructor for component manager.
     */
    public DefaultFilesystemStoreTools()
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
            new Thread(new Runnable()
            {
                public void run()
                {
                    deleteEmptyDirs(dir, 0);
                }
            }).start();
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

    @Override
    public File getBackupFile(final File storageFile)
    {
        // We pad our file names with random alphanumeric characters so that multiple operations on the same
        // file in the same transaction do not collide, the set of all capital and lower case letters
        // and numbers has 62 possibilities and 62^8 = 218340105584896 between 2^47 and 2^48.
        return new File(storageFile.getAbsolutePath() + BACKUP_FILE_SUFFIX + RandomStringUtils.randomAlphanumeric(8));
    }

    @Override
    public File getTempFile(final File storageFile)
    {
        return new File(storageFile.getAbsolutePath() + TEMP_FILE_SUFFIX + RandomStringUtils.randomAlphanumeric(8));
    }

    @Override
    public DeletedAttachmentFileProvider getDeletedAttachmentFileProvider(final AttachmentReference attachment,
        final long index)
    {
        return new DefaultDeletedAttachmentFileProvider(getDeletedAttachmentDir(attachment, index),
            attachment.getName());
    }

    @Override
    public DeletedDocumentContentFileProvider getDeletedDocumentFileProvider(DocumentReference documentReference,
        long index)
    {
        return new DefaultDeletedDocumentContentFileProvider(getDeletedDocumentContentDir(documentReference, index));
    }

    @Override
    public String getStorageLocationPath()
    {
        return this.storageDir.getPath();
    }

    @Override
    public File getStorageLocationFile()
    {
        return this.storageDir;
    }

    @Override
    public File getGlobalFile(final String name)
    {
        return new File(this.storageDir, "~GLOBAL_" + GenericFileUtils.getURLEncoded(name));
    }

    @Override
    public AttachmentFileProvider getAttachmentFileProvider(final AttachmentReference attachmentReference)
    {
        return new DefaultAttachmentFileProvider(getAttachmentDir(attachmentReference), attachmentReference.getName());
    }

    @Override
    public boolean attachmentContentExist(AttachmentReference attachmentReference)
    {
        return getAttachmentFileProvider(attachmentReference).getAttachmentContentFile().exists();
    }

    @Override
    public boolean attachmentArchiveExist(AttachmentReference attachmentReference)
    {
        return getAttachmentFileProvider(attachmentReference).getAttachmentVersioningMetaFile().exists();
    }

    private File getAttachmentDir(final AttachmentReference attachmentReference)
    {
        final File docDir =
            getDocumentDir(attachmentReference.getDocumentReference(), this.storageDir, this.pathSerializer);
        final File attachmentsDir = new File(docDir, ATTACHMENT_DIR_NAME);
        return new File(attachmentsDir, GenericFileUtils.getURLEncoded(attachmentReference.getName()));
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
        final File docDir = getDocumentDir(doc, this.storageDir, this.pathSerializer);
        final File deletedAttachmentsDir = new File(docDir, DELETED_ATTACHMENT_DIR_NAME);
        final String fileName = attachment.getName() + DELETED_ATTACHMENT_NAME_SEPARATOR + index;
        return new File(deletedAttachmentsDir, GenericFileUtils.getURLEncoded(fileName));
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
        final File docDir = getDocumentDir(documentReference, this.storageDir, this.pathSerializer);
        final File deletedDocumentContentsDir = new File(docDir, DELETED_DOCUMENT_DIR_NAME);
        return new File(deletedDocumentContentsDir, String.valueOf(index));
    }

    /**
     * Get the directory associated with this document. This is a path obtained from the owner document reference, where
     * each reference segment (wiki, spaces, document name) contributes to the final path. For a document called
     * xwiki:Main.WebHome, the directory will be: <code>(storageDir)/xwiki/Main/WebHome/~this/</code>
     *
     * @param docRef the DocumentReference for the document to get the directory for.
     * @param storageDir the directory to place the directory hirearcy for attachments in.
     * @param pathSerializer an EntityReferenceSerializer which will make a directory path from an an EntityReference.
     * @return a file path corresponding to the attachment location; each segment in the path is URL-encoded in order to
     *         be safe.
     */
    private static File getDocumentDir(final DocumentReference docRef, final File storageDir,
        final EntityReferenceSerializer<String> pathSerializer)
    {
        final File path = new File(storageDir, pathSerializer.serialize(docRef));
        File docDir = new File(path, DOCUMENT_DIR_NAME);

        // Add the locale
        Locale docLocale = docRef.getLocale();
        if (docLocale != null) {
            final File docLocalesDir = new File(docDir, DOCUMENT_LOCALES_DIR_NAME);
            final File docLocaleDir = new File(docLocalesDir,
                docLocale.equals(Locale.ROOT) ? DOCUMENT_LOCALES_ROOT_NAME : docLocale.toString());
            docDir = new File(docLocaleDir, DOCUMENTLOCALE_DIR_NAME);
        }

        return docDir;
    }

    @Override
    public ReadWriteLock getLockForFile(final File toLock)
    {
        return this.lockProvider.getLock(toLock);
    }
}
