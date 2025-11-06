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
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.internal.reference.LocalUidStringEntityReferenceSerializer;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;
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

    /**
     * A means of acquiring locks for attachments. Because the attachments temp files are randomly named and rename is
     * atomic, locks are not needed. DummyLockProvider provides fake locks.
     */
    @Inject
    @Named("dummy")
    private LockProvider lockProvider;

    @Inject
    private BlobStoreManager blobStoreManager;

    private BlobStore store;

    /**
     * Default constructor.
     */
    public FilesystemStoreTools()
    {
        // Nothing to do
    }

    /**
     * Constructor used for testing.
     *
     * @param blobStore the blob store to use
     * @param lockProvider the lock provider to use
     * @since 17.9.0RC1
     */
    public FilesystemStoreTools(BlobStore blobStore, LockProvider lockProvider)
    {
        this.store = blobStore;
        this.lockProvider = lockProvider;
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.store = this.blobStoreManager.getBlobStore("store/" + FileSystemStoreUtils.HINT);
        } catch (BlobStoreException e) {
            throw new InitializationException("Failed to initialize attachments blob store.", e);
        }
    }

    /**
     * @return the store for file system attachment blobs
     * @since 17.8.0RC1
     */
    public BlobStore getStore()
    {
        return this.store;
    }

    /**
     * Get a backup file which for a given storage file. This file name will never collide with any other file gotten
     * through this interface.
     *
     * @param storageFile the file to get a backup file for.
     * @return a backup file with a name based on the name of the given file.
     */
    public Blob getBackupFile(final Blob storageFile) throws BlobStoreException
    {
        return getBlobWithRandomSuffix(storageFile, BACKUP_FILE_SUFFIX);
    }

    /**
     * Get a temporary file which for a given storage file. This file name will never collide with any other file gotten
     * through this interface.
     *
     * @param storageFile the file to get a temporary file for.
     * @return a temporary file with a name based on the name of the given file.
     */
    public Blob getTempFile(final Blob storageFile) throws BlobStoreException
    {
        return getBlobWithRandomSuffix(storageFile, TEMP_FILE_SUFFIX);
    }

    private static Blob getBlobWithRandomSuffix(Blob storageFile, String tempFileSuffix) throws BlobStoreException
    {
        // We pad our file names with random alphanumeric characters so that multiple operations on the same
        // file in the same transaction do not collide, the set of all capital and lower case letters
        // and numbers has 62 possibilities and 62^8 = 218340105584896 between 2^47 and 2^48.
        BlobPath basePath = storageFile.getPath();
        // The storage file cannot be the root, so we know this will have a file name.
        String fileName = Objects.requireNonNull(basePath.getFileName()).toString();
        String suffix = tempFileSuffix + RandomStringUtils.secure().nextAlphanumeric(8);
        BlobPath path = basePath.resolveSibling(fileName + suffix);
        return storageFile.getStore().getBlob(path);
    }

    /**
     * Get an instance of AttachmentBlobProvider which will save everything to do with an attachment in a separate
     * location which is repeatable only with the same attachment name, containing document, and date of deletion.
     *
     * @param attachment the reference of the attachment
     * @param index the index of the deleted attachment.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     * @since 9.10RC1
     */
    public DeletedAttachmentBlobProvider getDeletedAttachmentFileProvider(final AttachmentReference attachment,
        final long index)
    {
        return new DefaultDeletedAttachmentBlobProvider(getStore(), getDeletedAttachmentDir(attachment, index),
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
        return new DefaultDeletedDocumentContentFileProvider(getStore(), getDeletedDocumentContentDir(documentReference,
            index));
    }

    /**
     * Get an instance of AttachmentBlobProvider which will save everything to do with an attachment in a separate
     * location which is repeatable only with the same attachment name, and containing document.
     *
     * @param attachmentReference the reference attachment to get a tools for.
     * @return a provider which will provide files with collision free path and repeatable with same inputs.
     * @since 9.10RC1
     */
    public AttachmentBlobProvider getAttachmentFileProvider(final AttachmentReference attachmentReference)
    {
        return new DefaultAttachmentBlobProvider(getStore(), getAttachmentDir(attachmentReference),
            attachmentReference.getName());
    }

    /**
     * @param attachment the attachment
     * @return the content of the link file
     * @since 16.4.0
     */
    public String getLinkContent(XWikiAttachment attachment) throws BlobStoreException
    {
        AttachmentBlobProvider provider = getAttachmentFileProvider(attachment.getReference());
        Blob defaultFile = provider.getAttachmentContentBlob();
        Blob versionFile = provider.getAttachmentVersionContentBlob(attachment.getVersion());

        // The parent of defaultFile path cannot be null as attachment blobs are never the root.
        return Objects.requireNonNull(defaultFile.getPath().getParent()).relativize(versionFile.getPath()).toString();
    }

    /**
     * @param attachmentReference the attachment reference
     * @return the attachment directory
     * @since 11.0
     */
    public BlobPath getAttachmentDir(final AttachmentReference attachmentReference)
    {
        final BlobPath docDir = getDocumentContentDir(attachmentReference.getDocumentReference());
        BlobPath attachmentsPath = docDir.resolve(ATTACHMENTS_DIR_NAME);

        return hashDirectory(attachmentsPath, attachmentReference.getName());
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
    public BlobPath getDeletedAttachmentDir(final AttachmentReference attachment, final long index)
    {
        final DocumentReference doc = attachment.getDocumentReference();
        final BlobPath docDir = getDocumentContentDir(doc);
        final BlobPath deletedAttachmentsDir = docDir.resolve(DELETED_ATTACHMENTS_DIR_NAME);
        final BlobPath deletedAttachmentDir = hashDirectory(deletedAttachmentsDir, attachment.getName());

        return deletedAttachmentDir.resolve(String.valueOf(index));
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
    public BlobPath getDeletedDocumentContentDir(final DocumentReference documentReference, final long index)
    {
        return getDocumentContentDir(documentReference)
            .resolve(BlobPath.relative(DELETED_DOCUMENTS_DIR_NAME, String.valueOf(index)));
    }

    /**
     * @param wikiId the wiki identifier
     * @return the {@link File} corresponding to the passed wiki identifier
     * @since 10.1RC1
     */
    public BlobPath getWikiDir(String wikiId)
    {
        return BlobPath.absolute(wikiId);
    }

    private BlobPath hashDirectory(BlobPath parent, String name)
    {
        String md5 = DigestUtils.md5Hex(name);

        // Avoid having too many files in one folder because some filesystems don't perform well with large numbers of
        // files in one folder
        return parent.resolve(BlobPath.relative(
            String.valueOf(md5.charAt(0)),
            String.valueOf(md5.charAt(1)),
            md5.substring(2)));
    }

    /**
     * Get the directory associated with this document.
     *
     * @param documentReference the DocumentReference for the document to get the directory for.
     * @return a file path corresponding to the attachment location; each segment in the path is URL-encoded in order to
     *         be safe.
     * @since 11.0
     */
    public BlobPath getDocumentContentDir(final DocumentReference documentReference)
    {
        BlobPath wikiDir = getWikiDir(documentReference.getWikiReference().getName());

        String localKey =
            LocalUidStringEntityReferenceSerializer.INSTANCE.serialize(documentReference.getLocale() != null
                ? new DocumentReference(documentReference, (Locale) null) : documentReference);
        String md5 = DigestUtils.md5Hex(localKey);

        // Avoid having too many files in one folder because some filesystems don't perform well with large numbers of
        // files in one folder
        BlobPath documentDirPath = wikiDir.resolve(BlobPath.relative(
            String.valueOf(md5.charAt(0)),
            String.valueOf(md5.charAt(1)),
            md5.substring(2)));

        // Add the locale (if any)
        Locale documentLocale = documentReference.getLocale();
        if (documentLocale != null && !documentLocale.equals(Locale.ROOT)) {
            documentDirPath = documentDirPath.resolve(BlobPath.relative(
                DOCUMENT_LOCALES_DIR_NAME, documentLocale.toString()));
        }

        return documentDirPath;
    }

    /**
     * Get a {@link java.util.concurrent.locks.ReadWriteLock} which is unique to the given file. This method will always
     * return the same lock for the path on the filesystem even if the {@link java.io.File} object is different.
     *
     * @param toLock the file to get a lock for.
     * @return a lock for the given file.
     */
    public ReadWriteLock getLockForFile(final BlobPath toLock)
    {
        return this.lockProvider.getLock(toLock);
    }
}
