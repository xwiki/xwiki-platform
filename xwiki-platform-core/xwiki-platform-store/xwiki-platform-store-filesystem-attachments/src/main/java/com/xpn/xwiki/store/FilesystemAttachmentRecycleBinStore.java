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
package com.xpn.xwiki.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.store.FileDeleteTransactionRunnable;
import org.xwiki.store.FileSaveTransactionRunnable;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.filesystem.internal.DeletedAttachmentFileProvider;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.serialization.SerializationStreamProvider;
import org.xwiki.store.serialization.Serializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.DeletedFilesystemAttachment;
import com.xpn.xwiki.doc.FilesystemAttachmentContent;
import com.xpn.xwiki.doc.MutableDeletedFilesystemAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Realization of {@link AttachmentRecycleBinStore} for filesystem storage.
 *
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Named("file")
@Singleton
public class FilesystemAttachmentRecycleBinStore implements AttachmentRecycleBinStore, Initializable
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

    /**
     * Used to parse and serialize deleted attachment metadata when loading and storing.
     */
    @Inject
    @Named("deleted-attachment-meta/1.0")
    private Serializer<DeletedAttachment, MutableDeletedFilesystemAttachment> deletedAttachmentSerializer;

    /**
     * This is needed in order to be able to map the database ids given by the
     * user to meaningful paths to deleted attachments.
     */
    @Inject
    @Named("deleted-attachment-id-mappings/1.0")
    private Serializer<Map<Long, String>, Map<Long, String>> attachmentIdMappingSerializer;

    /**
     * Used to store the versions of the deleted attachment.
     */
    @Inject
    @Named("file")
    private AttachmentVersioningStore attachmentVersionStore;

    /**
     * This is required because deleted attachments may be looked up by a database id number
     * So we are forced to simulate the database id numbering scheme even though they
     * are and should be stored with their documents which are stored by name.
     */
    private final Map<Long, String> pathById = new ConcurrentHashMap<Long, String>();

    /**
     * The location to persist the pathById map.
     */
    private File pathByIdStore;

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

        this.pathByIdStore = this.fileTools.getGlobalFile("DELETED_ATTACHMENT_ID_MAPPINGS.xml");
        if (pathByIdStore.exists()) {
            try {
                this.pathById.putAll(
                    attachmentIdMappingSerializer.parse(new FileInputStream(this.pathByIdStore)));
            } catch (IOException e) {
                throw new InitializationException("Failed to parse deleted attachment id mappings.", e);
            }
        }
    }

    @Override
    public void saveToRecycleBin(final XWikiAttachment attachment,
        final String deleter,
        final Date deleteDate,
        final XWikiContext context,
        final boolean bTransaction) throws XWikiException
    {
        final DeletedFilesystemAttachment dfa =
            new DeletedFilesystemAttachment(attachment, deleter, deleteDate);
        final StartableTransactionRunnable tr = this.getSaveTrashAttachmentRunnable(dfa, context);

        // Need to add the ID to the map and persist the map
        // otherwise the attachment will not be able to loaded by the ID.
        // TODO standardize a deleted attachment entity reference and deprecate the use of a long integer
        //      as a key to load a deleted attachment with.
        final String absolutePath =
            this.fileTools.getDeletedAttachmentFileProvider(attachment, deleteDate)
                .getAttachmentContentFile().getParentFile().getAbsolutePath();
        final String path =
            absolutePath.substring(absolutePath.indexOf(this.fileTools.getStorageLocationPath()));
        final Long id = Long.valueOf(dfa.getId());
        new StartableTransactionRunnable()
        {
            public void onRun()
            {
                pathById.put(id, path);
            }

            public void onRollback()
            {
                pathById.remove(id);
            }
        } .runIn(tr);

        // Need to save the updated map right away in case the power goes out or something.
        new FileSaveTransactionRunnable(
            this.pathByIdStore,
            this.fileTools.getTempFile(this.pathByIdStore),
            this.fileTools.getBackupFile(this.pathByIdStore),
            this.fileTools.getLockForFile(this.pathByIdStore),
            new SerializationStreamProvider<Map<Long, String>>(
                this.attachmentIdMappingSerializer,
                this.pathById)).runIn(tr);

        try {
            tr.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.MODULE_XWIKI,
                "Failed to store deleted attachment " + attachment.getFilename()
                    + " for document: " + attachment.getDoc().getDocumentReference(), e);
        }
    }

    /**
     * Get a StartableTransactionRunnable to save an attachment in the recycle-bin.
     *
     * @param deleted the FilesystemDeletedAttachment to save.
     * @param context the legacy XWikiContext which might be needed to get the content
     * from the attachment, or to load the attachment versioning store.
     * @return a TransactionRunnable for storing the deleted attachment.
     * @throws XWikiException if one is thrown trying to get data from the attachment
     * or loading the attachment archive in order to save it in the deleted section.
     */
    public StartableTransactionRunnable
    getSaveTrashAttachmentRunnable(final DeletedFilesystemAttachment deleted,
        final XWikiContext context)
        throws XWikiException
    {
        final DeletedAttachmentFileProvider provider =
            this.fileTools.getDeletedAttachmentFileProvider(deleted.getAttachment(), deleted.getDate());

        return new SaveTrashAttachmentRunnable(deleted,
            provider,
            this.fileTools,
            this.deletedAttachmentSerializer,
            this.versionSerializer,
            context);
    }

    /**
     * {@inheritDoc}
     * bTransaction is ignored by this implementation.
     *
     * @see AttachmentRecycleBinStore#restoreFromRecycleBin(XWikiAttachment, long, XWikiContext, boolean)
     */
    @Override
    public XWikiAttachment restoreFromRecycleBin(final XWikiAttachment attachment,
        final long index,
        final XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        final DeletedAttachment delAttach = getDeletedAttachment(index, context, false);
        return delAttach != null ? delAttach.restoreAttachment(attachment, context) : null;
    }

    /**
     * {@inheritDoc}
     * bTransaction is ignored by this implementation.
     * context is unused and may safely be null.
     *
     * @see AttachmentRecycleBinStore#getDeletedAttachment(long, XWikiContext, boolean)
     */
    @Override
    public DeletedAttachment getDeletedAttachment(final long index,
        final XWikiContext context,
        final boolean bTransaction) throws XWikiException
    {
        final String path = this.pathById.get(Long.valueOf(index));
        if (path == null) {
            return null;
        }

        try {
            return this.deletedAttachmentFromProvider(
                this.fileTools.getDeletedAttachmentFileProvider(path));
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.MODULE_XWIKI,
                "Failed to get deleted attachment at index " + index
                    + " with filesystem path " + path, e);
        }
    }

    /**
     * {@inheritDoc}
     * bTransaction is ignored by this implementation.
     *
     * @see AttachmentRecycleBinStore#getAllDeletedAttachments(XWikiAttachment, XWikiContext, boolean)
     */
    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(final XWikiAttachment attachment,
        final XWikiContext context,
        final boolean bTransaction)
        throws XWikiException
    {
        if (attachment.getDoc() == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.MODULE_XWIKI,
                "Cannot load deleted attachments because the given attachment "
                    + attachment.getFilename() + " is not attached to any document.");
        }

        // I don't know that there is no way to upload an attachment named ""
        // so I don't want to use isEmpty here.
        if (attachment.getFilename() == null) {
            return this.getAllDeletedAttachments(attachment.getDoc(), context, false);
        }

        final Map<Date, DeletedAttachmentFileProvider> attachMap =
            this.fileTools.deletedAttachmentsForDocument(attachment.getDoc().getDocumentReference())
                .get(attachment.getFilename());
        final List<Date> deleteDatesList = new ArrayList<Date>(attachMap.keySet());
        Collections.sort(deleteDatesList, NewestFirstDateComparitor.INSTANCE);

        final List<DeletedAttachment> out = new ArrayList<DeletedAttachment>(deleteDatesList.size());
        try {
            for (Date date : deleteDatesList) {
                out.add(this.deletedAttachmentFromProvider(attachMap.get(date)));
            }
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.MODULE_XWIKI,
                "Failed to get deleted attachment " + attachment.getFilename()
                    + " attached to the document: "
                    + attachment.getDoc().getDocumentReference(), e);
        }
        return out;
    }

    /**
     * {@inheritDoc}
     * bTransaction is ignored by this implementation.
     * context is unused and may safely be null.
     *
     * @see AttachmentRecycleBinStore#getAllDeletedAttachments(XWikiDocument, XWikiContext, boolean)
     */
    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(final XWikiDocument doc,
        final XWikiContext context,
        final boolean bTransaction)
        throws XWikiException
    {
        final Map<String, Map<Date, DeletedAttachmentFileProvider>> attachMap =
            this.fileTools.deletedAttachmentsForDocument(doc.getDocumentReference());

        // Get a list of dates ordered by newest first.
        final List<Date> allDates = new ArrayList<Date>();
        for (Map<Date, ?> dateMap : attachMap.values()) {
            allDates.addAll(dateMap.keySet());
        }
        Collections.sort(allDates, NewestFirstDateComparitor.INSTANCE);

        // Populate the output list by the order of the date.
        // Everything cannot be placed into an ordered map because it is conceivable that 2 attachments
        // would be deleted in the same millisecond and that would cause them to be merged.
        try {
            final List<DeletedAttachment> out = new ArrayList<DeletedAttachment>(allDates.size());
            for (Date date : allDates) {
                for (Map<Date, DeletedAttachmentFileProvider> map : attachMap.values()) {
                    if (map.get(date) != null) {
                        out.add(this.deletedAttachmentFromProvider(map.get(date)));
                        map.remove(date);
                        break;
                    }
                }
            }
            return out;
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.MODULE_XWIKI,
                "Failed to get deleted attachments for document: "
                    + doc.getDocumentReference(), e);
        }
    }

    /**
     * {@inheritDoc}
     * bTransaction is ignored because the filesystem cannot synchronize with the database commit.
     * TODO: make getDeletedAttachmentPurgeRunnable public so that a transaction safe method is available.
     * context is unused and may safely be null.
     *
     * @see AttachmentRecycleBinStore#deleteFromRecycleBin(long, XWikiContext, boolean)
     */
    @Override
    public void deleteFromRecycleBin(final long index,
        final XWikiContext context,
        final boolean bTransaction)
        throws XWikiException
    {
        final String path = this.pathById.get(Long.valueOf(index));
        if (path != null) {
            this.getDeletedAttachmentPurgeRunnable(this.fileTools.getDeletedAttachmentFileProvider(path));
        }
    }

    /**
     * Get a TransactionRunnable for removing a deleted attachment from the filesystem entirely.
     * TODO: Standardize an EntityReference for deleted attachments and make that the parameter.
     *
     * @param provider the file provider for the deleted attachment to purge from the recycle bin.
     * @return a StartableTransactionRunnable for removing the attachment.
     */
    private StartableTransactionRunnable getDeletedAttachmentPurgeRunnable(
        final DeletedAttachmentFileProvider provider)
    {
        final StartableTransactionRunnable out = new StartableTransactionRunnable();
        final File deletedAttachDir = provider.getDeletedAttachmentMetaFile().getParentFile();
        if (!deletedAttachDir.exists()) {
            // No such dir, return a do-nothing runnable.
            return out;
        }
        // Easy thing to do is just delete everything in the deleted-attachment directory.
        for (File toDelete : deletedAttachDir.listFiles()) {
            new FileDeleteTransactionRunnable(toDelete,
                this.fileTools.getBackupFile(toDelete),
                this.fileTools.getLockForFile(toDelete)).runIn(out);
        }

        // Remove the entry from the pathById map so that it doesn't cause a memory leak.
        final String path = deletedAttachDir.getAbsolutePath();
        for (final Long id : pathById.keySet()) {
            if (pathById.get(id).endsWith(path)) {
                new StartableTransactionRunnable()
                {
                    public void onRun()
                    {
                        pathById.remove(id);
                    }

                    public void onRollback()
                    {
                        pathById.put(id, path);
                    }
                }.runIn(out);
                break;
            }
        }

        return out;
    }

    /**
     * Get a deleted attachment by it's filesystem location.
     * This returns a DeletedAttachment which is not attached to any document!
     * It is the job of the caller to get the attachment and any version of it and attach them
     * to a document.
     *
     * @param provider a means to get the files which store the deleted attachment content and metadata.
     * @return the deleted attachment for that directory.
     * @throws IOException if deserialization fails or there is a problem loading the archive.
     */
    private DeletedAttachment deletedAttachmentFromProvider(
        final DeletedAttachmentFileProvider provider) throws IOException
    {
        final File deletedMeta = provider.getDeletedAttachmentMetaFile();

        // No metadata, no deleted attachment.
        if (!deletedMeta.exists()) {
            return null;
        }

        final MutableDeletedFilesystemAttachment delAttach;
        ReadWriteLock lock = this.fileTools.getLockForFile(deletedMeta);
        lock.readLock().lock();
        try {
            delAttach = this.deletedAttachmentSerializer.parse(new FileInputStream(deletedMeta));
        } finally {
            lock.readLock().unlock();
        }

        final File contentFile = provider.getAttachmentContentFile();
        final XWikiAttachment attachment = delAttach.getAttachment();
        attachment.setAttachment_content(
            new FilesystemAttachmentContent(contentFile,
                attachment,
                this.fileTools.getLockForFile(contentFile)));

        attachment.setAttachment_archive(
            ((FilesystemAttachmentVersioningStore) this.attachmentVersionStore)
                .loadArchive(attachment, provider));

        return delAttach.getImmutable();
    }

    /* ---------------------------- Nested Classes. ---------------------------- */

    /**
     * A date comparator which compares dates in reverse chronological order.
     */
    private static class NewestFirstDateComparitor implements Comparator<Date>
    {
        /**
         * A static reference to a singleton instance of the comparator.
         */
        public static final Comparator<Date> INSTANCE = new NewestFirstDateComparitor();

        @Override
        public int compare(final Date d1, final Date d2)
        {
            return d2.compareTo(d1);
        }
    }
}
