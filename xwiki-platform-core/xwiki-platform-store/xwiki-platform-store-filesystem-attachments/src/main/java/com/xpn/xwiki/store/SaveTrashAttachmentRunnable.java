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
import java.util.List;

import org.xwiki.store.FileSaveTransactionRunnable;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.filesystem.internal.DeletedAttachmentFileProvider;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.serialization.SerializationStreamProvider;
import org.xwiki.store.serialization.Serializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.DeletedFilesystemAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;

/**
 * A TransactionRunnable for saving deleted attachments.
 *
 * @version $Id$
 * @since 3.0M3
 */
class SaveTrashAttachmentRunnable extends StartableTransactionRunnable
{
    /**
     * The Constructor.
     *
     * @param deleted the deleted attachment.
     * @param provider a means of gettign the files used for storing the attachment.
     * @param fileTools tools for getting file locks and backup/temporary files.
     * @param deletedAttachmentSerializer a Serializer to serialize a DeletedAttachment.
     * @param versionSerializer a Serializer which will serialize a list of XWikiAttachment objects.
     * @param context the legacy XWikiContext which might be needed to get the attachment archive.
     * @throws XWikiException if loading the attachment content or archive fails.
     */
    public SaveTrashAttachmentRunnable(final DeletedFilesystemAttachment deleted,
        final DeletedAttachmentFileProvider provider,
        final FilesystemStoreTools fileTools,
        final Serializer<DeletedAttachment, ? extends DeletedAttachment> deletedAttachmentSerializer,
        final Serializer<List<XWikiAttachment>, List<XWikiAttachment>> versionSerializer,
        final XWikiContext context)
        throws XWikiException
    {
        // Save metadata about the deleted attachment.
        final StreamProvider metaProvider =
            new SerializationStreamProvider<DeletedAttachment>(deletedAttachmentSerializer, deleted);
        this.addSaver(metaProvider, fileTools, provider.getDeletedAttachmentMetaFile());

        final XWikiAttachment attachment = deleted.getAttachment();
        final XWikiAttachmentArchive archive = attachment.loadArchive(context);
        if (archive == null) {
            throw new NullPointerException("Failed to load attachment archive, "
                + "loadArchive() returned null");
        }

        // Save the archive for the deleted attachment.
        new AttachmentArchiveSaveRunnable(archive,
            fileTools,
            provider,
            versionSerializer,
            context).runIn(this);

        // Save the attachment's content.
        final StreamProvider contentProvider = new AttachmentContentStreamProvider(attachment, context);
        this.addSaver(contentProvider,
            fileTools,
            provider.getAttachmentContentFile());
    }

    /**
     * Save some content safely in this runnable.
     * TODO This duplicates AttachmentArchiveSaveRunnable, fix.
     *
     * @param provider the means to get the content to save.
     * @param fileTools the means to get the backup file, temporary file, and lock.
     * @param saveHere the location to save the data.
     */
    private void addSaver(final StreamProvider provider,
        final FilesystemStoreTools fileTools,
        final File saveHere)
    {
        new FileSaveTransactionRunnable(saveHere,
            fileTools.getTempFile(saveHere),
            fileTools.getBackupFile(saveHere),
            fileTools.getLockForFile(saveHere),
            provider).runIn(this);
    }
}
