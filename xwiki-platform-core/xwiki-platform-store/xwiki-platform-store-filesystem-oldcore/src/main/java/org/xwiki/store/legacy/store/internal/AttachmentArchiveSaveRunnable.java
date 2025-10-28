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

import java.util.ArrayList;
import java.util.List;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.filesystem.internal.AttachmentBlobProvider;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.BlobSaveTransactionRunnable;
import org.xwiki.store.serialization.SerializationStreamProvider;
import org.xwiki.store.serialization.Serializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.store.VoidAttachmentVersioningStore;

/**
 * A TransactionRunnable for saving attachment archives.
 * It uses a chain of FileSaveTransactionRunnable so the attachment will either be saved or fail
 * safely, it should not hang in a halfway state.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class AttachmentArchiveSaveRunnable extends StartableTransactionRunnable
{
    /**
     * The Constructor.
     *
     * @param archive the attachment archive to save.
     * @param fileTools a set of tools for getting the file corrisponding to each version of the
     * attachment content and the file for the meta data, as well as temporary
     * and backup files corrisponding to each. Also for getting locks.
     * @param provider the means to get the blobs to store each version of the attachment.
     * @param serializer an attachment list metadata serializer for serializing the metadata of each
     * version of the attachment.
     * @param context the XWikiContext used to get the revisions of the attachment.
     * @throws XWikiException if it is unable to get a revision of an attachment using archive.getRevision()
     */
    public AttachmentArchiveSaveRunnable(final XWikiAttachmentArchive archive,
        final FilesystemStoreTools fileTools,
        final AttachmentBlobProvider provider,
        final Serializer<List<XWikiAttachment>,
            List<XWikiAttachment>> serializer,
        final XWikiContext context)
        throws XWikiException, BlobStoreException
    {
        if (archive instanceof VoidAttachmentVersioningStore.VoidAttachmentArchive) {
            return;
        }
        // If the archive is empty, initialize it
        if (archive.getVersions().length == 0 && archive.getAttachment() != null) {
            archive.addCurrentAttachment(context);
        }

        final Version[] versions = archive.getVersions();
        final List<XWikiAttachment> attachmentVersions = new ArrayList<>(versions.length);

        // Add the content files which need updating and add the attachments to the list.
        for (int i = 0; i < versions.length; i++) {
            final String versionName = versions[i].toString();
            final XWikiAttachment attachVer =
                archive.getRevision(archive.getAttachment(), versionName, context);
            attachmentVersions.add(attachVer);

            // If the content is not dirty and the file was already saved then we will not update.
            if (attachVer.isContentDirty()
                || !provider.getAttachmentVersionContentBlob(versionName).exists())
            {
                final StreamProvider contentProvider =
                    new AttachmentContentStreamProvider(attachVer, context);
                addSaver(contentProvider, fileTools, provider.getAttachmentVersionContentBlob(versionName));
            }
        }

        // Then do the metadata.
        final StreamProvider metaProvider =
            new SerializationStreamProvider<List<XWikiAttachment>>(serializer, attachmentVersions);
        addSaver(metaProvider, fileTools, provider.getAttachmentVersioningMetaBlob());
    }

    /**
     * Save some content safely in this runnable.
     *
     * @param provider the means to get the content to save.
     * @param fileTools the means to get the backup file, temporary file, and lock.
     * @param saveHere the location to save the data.
     */
    private void addSaver(final StreamProvider provider,
        final FilesystemStoreTools fileTools,
        final Blob saveHere) throws BlobStoreException
    {
        new BlobSaveTransactionRunnable(saveHere, fileTools.getTempFile(saveHere), fileTools.getBackupFile(saveHere),
            fileTools.getLockForFile(saveHere.getPath()),
            provider).runIn(this);
    }
}
