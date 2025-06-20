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
package org.xwiki.store.legacy.store.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.store.FileDeleteTransactionRunnable;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.filesystem.internal.DeletedAttachmentFileProvider;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.filesystem.internal.StoreFileUtils;
import org.xwiki.store.internal.FileSystemStoreUtils;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.store.serialization.Serializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.store.AttachmentRecycleBinContentStore;
import com.xpn.xwiki.store.AttachmentVersioningStore;

/**
 * Implementation of {@link AttachmentRecycleBinContentStore} for filesystem storage.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component
@Named(FileSystemStoreUtils.HINT)
@Singleton
public class FilesystemAttachmentRecycleBinContentStore implements AttachmentRecycleBinContentStore, Initializable
{
    /**
     * Some utilities for getting attachment files, locks, and backup files.
     */
    @Inject
    private FilesystemStoreTools fileTools;

    /**
     * A serializer for the archive metadata.
     */
    @Inject
    @Named("attachment-list-meta/1.0")
    private Serializer<List<XWikiAttachment>, List<XWikiAttachment>> versionSerializer;

    @Inject
    @Named("deleted-attachment-meta/1.0")
    private Serializer<XWikiAttachment, XWikiAttachment> metaSerializer;

    /**
     * Used to store the versions of the deleted attachment.
     */
    @Inject
    @Named(FileSystemStoreUtils.HINT)
    private AttachmentVersioningStore attachmentVersionStore;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String getHint()
    {
        return FileSystemStoreUtils.HINT;
    }

    /**
     * Load the pathById mappings so that attachments can be loaded by database id.
     *
     * @throws InitializationException if the mapping file cannot be parsed.
     */
    @Override
    public void initialize() throws InitializationException
    {
        // make sure we have a FilesystemAttachmentVersioningStore.
        if (!(attachmentVersionStore instanceof FilesystemAttachmentVersioningStore)) {
            throw new InitializationException("Wrong attachment versioning store registered under hint "
                + "'file', expecting a FilesystemAttachmentVersioningStore");
        }
    }

    @Override
    public void delete(AttachmentReference reference, Date deleteDate, long index, boolean bTransaction)
        throws XWikiException
    {
        DeletedAttachmentFileProvider provider = this.fileTools.getDeletedAttachmentFileProvider(reference, index);

        StartableTransactionRunnable tr = getDeletedAttachmentPurgeRunnable(provider);

        try {
            tr.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.MODULE_XWIKI,
                "Failed to delete deleted attachment [" + reference + "]", e);
        }
    }

    /**
     * Get a TransactionRunnable for removing a deleted attachment from the filesystem entirely.
     *
     * @param provider the file provider for the deleted attachment to purge from the recycle bin.
     * @return a StartableTransactionRunnable for removing the attachment.
     */
    private StartableTransactionRunnable getDeletedAttachmentPurgeRunnable(final DeletedAttachmentFileProvider provider)
    {
        final StartableTransactionRunnable out = new StartableTransactionRunnable();
        final File deletedAttachDir = provider.getDeletedAttachmentMetaFile().getParentFile();
        if (!deletedAttachDir.exists()) {
            // No such dir, return a do-nothing runnable.
            return out;
        }

        // Easy thing to do is just delete everything in the deleted-attachment directory.
        for (File toDelete : deletedAttachDir.listFiles()) {
            new FileDeleteTransactionRunnable(toDelete, this.fileTools.getBackupFile(toDelete),
                this.fileTools.getLockForFile(toDelete)).runIn(out);
        }

        return out;
    }

    @Override
    public DeletedAttachmentContent get(AttachmentReference reference, Date deleteDate, long index,
        boolean bTransaction) throws XWikiException
    {
        DeletedAttachmentFileProvider provider = this.fileTools.getDeletedAttachmentFileProvider(reference, index);

        try {
            return deletedAttachmentContentFromProvider(provider);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.MODULE_XWIKI,
                "Failed to get deleted attachment [" + reference + "] at index [" + index + "] and date  [" + deleteDate
                    + "]",
                e);
        }
    }

    /**
     * Get a deleted attachment content by it's filesystem location. This returns a DeletedAttachmentContent which is
     * not attached to any document! It is the job of the caller to get the attachment and any version of it and attach
     * them to a document.
     * 
     * @param provider a means to get the files which store the deleted attachment content and metadata.
     * @return the deleted attachment for that directory.
     * @throws IOException if deserialization fails or there is a problem loading the archive.
     */
    private DeletedAttachmentContent deletedAttachmentContentFromProvider(final DeletedAttachmentFileProvider provider)
        throws IOException
    {
        final File deletedMeta = provider.getDeletedAttachmentMetaFile();

        // No metadata, no deleted attachment.
        if (!deletedMeta.exists()) {
            return null;
        }

        final XWikiAttachment attachment;
        ReadWriteLock lock = this.fileTools.getLockForFile(deletedMeta);
        lock.readLock().lock();
        try {
            attachment = this.metaSerializer.parse(new FileInputStream(deletedMeta));
        } finally {
            lock.readLock().unlock();
        }

        File contentFile = provider.getAttachmentContentFile();

        // Support links
        contentFile = StoreFileUtils.resolve(contentFile, true);

        attachment.setAttachment_content(new FilesystemAttachmentContent(contentFile, attachment));
        attachment.setContentStore(FileSystemStoreUtils.HINT);

        attachment.setAttachment_archive(
            ((FilesystemAttachmentVersioningStore) this.attachmentVersionStore).loadArchive(attachment, provider));
        attachment.setArchiveStore(FileSystemStoreUtils.HINT);

        return new FileDeletedAttachmentContent(attachment);
    }

    @Override
    public void save(XWikiAttachment attachment, Date deleteDate, long index, boolean bTransaction)
        throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        final StartableTransactionRunnable tr = getSaveTrashAttachmentRunnable(attachment, index, xcontext);

        new StartableTransactionRunnable().runIn(tr);

        try {
            tr.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.MODULE_XWIKI,
                "Failed to store deleted attachment [" + attachment.getReference() + "]", e);
        }
    }

    /**
     * Get a StartableTransactionRunnable to save an attachment in the recycle-bin.
     *
     * @param deleted the attachment to save.
     * @param index the index of the deleted attachment
     * @param context the legacy XWikiContext which might be needed to get the content from the attachment, or to load
     *            the attachment versioning store.
     * @return a TransactionRunnable for storing the deleted attachment.
     * @throws XWikiException if one is thrown trying to get data from the attachment or loading the attachment archive
     *             in order to save it in the deleted section.
     */
    private StartableTransactionRunnable getSaveTrashAttachmentRunnable(final XWikiAttachment deleted, long index,
        final XWikiContext context) throws XWikiException
    {
        final DeletedAttachmentFileProvider provider =
            this.fileTools.getDeletedAttachmentFileProvider(deleted.getReference(), index);

        return new SaveDeletedAttachmentContentRunnable(deleted, provider, this.fileTools, this.metaSerializer,
            this.versionSerializer, context);
    }
}
