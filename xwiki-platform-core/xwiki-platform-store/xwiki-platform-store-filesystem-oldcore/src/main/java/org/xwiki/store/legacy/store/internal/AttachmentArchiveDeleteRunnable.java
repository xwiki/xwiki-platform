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
import java.util.ArrayList;
import java.util.List;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.store.FileDeleteTransactionRunnable;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.filesystem.internal.AttachmentFileProvider;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;

import com.xpn.xwiki.doc.XWikiAttachmentArchive;

/**
 * A TransactionRunnable for deleting attachment archives. It uses FileDeleteTransactionRunnable so the attachment will
 * either be deleted or fail safely, it should not hang in a halfway state.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class AttachmentArchiveDeleteRunnable extends StartableTransactionRunnable
{
    /**
     * @param archive the attachment archive to delete.
     * @param fileTools tools for getting the metadata and versions of the attachment and locks.
     * @param provider the file provider for gettign the files to delete.
     */
    public AttachmentArchiveDeleteRunnable(final XWikiAttachmentArchive archive, final FilesystemStoreTools fileTools,
        final AttachmentFileProvider provider)
    {
        final List<File> toDelete = new ArrayList<>();
        toDelete.add(provider.getAttachmentVersioningMetaFile());

        final Version[] versions = archive.getVersions();
        for (int i = 0; i < versions.length; i++) {
            toDelete.add(provider.getAttachmentVersionContentFile(versions[i].toString()));
        }

        for (File file : toDelete) {
            new FileDeleteTransactionRunnable(file, fileTools.getBackupFile(file), fileTools.getLockForFile(file))
                .runIn(this);
        }
    }
}
