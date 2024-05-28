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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.model.internal.reference.LocalUidStringEntityReferenceSerializer;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.internal.FileSystemStoreUtils;
import org.xwiki.store.locks.LockProvider;

import com.xpn.xwiki.doc.XWikiAttachment;

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
     * The directory within each document's directory for document locales.
     * 
     * @since 10.0
     */
    public static final String DOCUMENT_LOCALES_DIR_NAME = "locales";

    /**
     * The directory within each document's directory where the document's attachments are stored.
     * 
     * @since 11.0
     */
    public static final String ATTACHMENTS_DIR_NAME = "attachments";

    /**
     * The directory within each document's directory for attachments which have been deleted.
     * 
     * @since 11.0
     */
    public static final String DELETED_ATTACHMENTS_DIR_NAME = "deleted-attachments";

    /**
     * The directory within each document's directory for documents which have been deleted.
     * 
     * @since 11.0
     */
    public static final String DELETED_DOCUMENTS_DIR_NAME = "deleted-documents";

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

    @Inject
    private FilesystemAttachmentsConfiguration config;

    /**
     * A means of acquiring locks for attachments. Because the attachments temp files are randomly named and rename is
     * atomic, locks are not needed. DummyLockProvider provides fake locks.
     */
    @Inject
    @Named("dummy")
    private LockProvider lockProvider;

    @Inject
    private Logger logger;

    /**
     * Used to get store directory.
     */
    @Inject
    private Environment environment;

    /**
     * This is the root directory of the stored data.
     */
    private File storeRootDirectory;

    /**
     * Testing Constructor.
     *
     * @param storageDir the directory to store the content in.
     * @param lockProvider a means of getting locks for making sure only one thread accesses an attachment at a time.
     */
    public FilesystemStoreTools(final File storageDir, final LockProvider lockProvider)
    {
        this.storeRootDirectory = storageDir;
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
        File fileStorageDirectory = this.config.getDirectory();

        if (fileStorageDirectory == null) {
            // General location when filesystem based stored put data
            File storeDirectory = new File(this.environment.getPermanentDirectory(), "store");
            // Specific location for file component
            fileStorageDirectory = new File(storeDirectory, FileSystemStoreUtils.HINT);
        }

        try {
            this.storeRootDirectory = fileStorageDirectory.getCanonicalFile();
        } catch (IOException e) {
            throw new InitializationException("Invalid permanent directory", e);
        }

        this.logger.info("Using filesystem store directory [{}]", this.storeRootDirectory);

        // TODO: make this useless (by cleaning empty directories as soon as they appear)
        if (this.config.cleanOnStartup()) {
            final File dir = this.storeRootDirectory;

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
     * @since 11.0
     */
    public File getStoreRootDirectory()
    {
        return this.storeRootDirectory;
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

    /**
     * @param attachment the attachment
     * @return the content of the link file
     * @since 16.4.0
     */
    public String getLinkContent(XWikiAttachment attachment)
    {
        AttachmentFileProvider provider = getAttachmentFileProvider(attachment.getReference());
        File defaultFile = provider.getAttachmentContentFile();
        File versionFile = provider.getAttachmentVersionContentFile(attachment.getVersion());

        return StoreFileUtils.getLinkContent(defaultFile.getParentFile(), versionFile);
    }

    /**
     * @param attachmentReference the attachment reference
     * @return the attachment directory
     * @since 11.0
     */
    public File getAttachmentDir(final AttachmentReference attachmentReference)
    {
        final File docDir = getDocumentContentDir(attachmentReference.getDocumentReference());
        final File attachmentsDir = new File(docDir, ATTACHMENTS_DIR_NAME);

        return hashDirectory(attachmentsDir, attachmentReference.getName());
    }

    /**
     * Get a directory for storing the contents of a deleted attachment. The format is {@code <document
     * name>/~this/deleted-attachments/<attachment name>-<delete date>/} {@code <delete date>} is expressed in "unix
     * time" so it might look like: {@code WebHome/~this/deleted-attachments/file.txt-0123456789/}
     *
     * @param attachment the attachment to get the file for.
     * @param index the index of the deleted attachment.
     * @return a directory which will be repeatable only with the same inputs.
     * @since 11.0
     */
    public File getDeletedAttachmentDir(final AttachmentReference attachment, final long index)
    {
        final DocumentReference doc = attachment.getDocumentReference();
        final File docDir = getDocumentContentDir(doc);
        final File deletedAttachmentsDir = new File(docDir, DELETED_ATTACHMENTS_DIR_NAME);
        final File deletedAttachmentDir = hashDirectory(deletedAttachmentsDir, attachment.getName());

        return new File(deletedAttachmentDir, String.valueOf(index));
    }

    /**
     * Get a directory for storing the content of a deleted document. The format is {@code <document
     * name>/~this/deleted-documents/<index>/content.xml}.
     *
     * @param documentReference the document to get the file for.
     * @param index the index of the deleted document.
     * @return a directory which will be repeatable only with the same inputs.
     * @since 11.0
     */
    public File getDeletedDocumentContentDir(final DocumentReference documentReference, final long index)
    {
        final File docDir = getDocumentContentDir(documentReference);
        final File deletedDocumentContentsDir = new File(docDir, DELETED_DOCUMENTS_DIR_NAME);

        return new File(deletedDocumentContentsDir, String.valueOf(index));
    }

    /**
     * @param wikiId the wiki identifier
     * @return the {@link File} corresponding to the passed wiki identifier
     * @since 10.1RC1
     */
    public File getWikiDir(String wikiId)
    {
        return new File(this.storeRootDirectory, wikiId);
    }

    private File hashDirectory(File parent, String name)
    {
        String md5 = DigestUtils.md5Hex(name);

        // Avoid having too many files in one folder because some filesystems don't perform well with large numbers of
        // files in one folder
        File documentDir1 = new File(parent, String.valueOf(md5.charAt(0)));
        File documentDir2 = new File(documentDir1, String.valueOf(md5.charAt(1)));

        return new File(documentDir2, String.valueOf(md5.substring(2)));
    }

    /**
     * Get the directory associated with this document.
     *
     * @param documentReference the DocumentReference for the document to get the directory for.
     * @return a file path corresponding to the attachment location; each segment in the path is URL-encoded in order to
     *         be safe.
     * @since 11.0
     */
    public File getDocumentContentDir(final DocumentReference documentReference)
    {
        File wikiDir = getWikiDir(documentReference.getWikiReference().getName());

        String localKey =
            LocalUidStringEntityReferenceSerializer.INSTANCE.serialize(documentReference.getLocale() != null
                ? new DocumentReference(documentReference, (Locale) null) : documentReference);
        String md5 = DigestUtils.md5Hex(localKey);

        // Avoid having too many files in one folder because some filesystems don't perform well with large numbers of
        // files in one folder
        File documentDir1 = new File(wikiDir, String.valueOf(md5.charAt(0)));
        File documentDir2 = new File(documentDir1, String.valueOf(md5.charAt(1)));
        File documentDirFinal = new File(documentDir2, String.valueOf(md5.substring(2)));

        // Add the locale (if any)
        Locale documentLocale = documentReference.getLocale();
        if (documentLocale != null && !documentLocale.equals(Locale.ROOT)) {
            File documentLocalesDir = new File(documentDirFinal, DOCUMENT_LOCALES_DIR_NAME);
            documentDirFinal = new File(documentLocalesDir, documentLocale.toString());
        }

        return documentDirFinal;
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
