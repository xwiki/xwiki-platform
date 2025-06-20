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
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.store.StartableTransactionRunnable;
import org.xwiki.store.filesystem.internal.AttachmentFileProvider;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileSystemStoreUtils;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.store.serialization.Serializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.internal.doc.ListAttachmentArchive;
import com.xpn.xwiki.store.AttachmentVersioningStore;

/**
 * Filesystem based AttachmentVersioningStore implementation.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component
@Named(FileSystemStoreUtils.HINT)
@Singleton
public class FilesystemAttachmentVersioningStore implements AttachmentVersioningStore
{
    /**
     * To put in an exception message if the document name or filename cannot be determined.
     */
    private static final String UNKNOWN_NAME = "UNKNOWN";

    /**
     * Tools for getting files to store given content in.
     */
    @Inject
    private FilesystemStoreTools fileTools;

    /**
     * A serializer for the list of attachment metdata.
     */
    @Inject
    @Named("attachment-list-meta/1.0")
    private Serializer<List<XWikiAttachment>, List<XWikiAttachment>> metaSerializer;

    @Override
    public String getHint()
    {
        return FileSystemStoreUtils.HINT;
    }

    @Override
    public XWikiAttachmentArchive loadArchive(final XWikiAttachment attachment, final XWikiContext context,
        final boolean bTransaction) throws XWikiException
    {
        try {
            return this.loadArchive(attachment, this.fileTools.getAttachmentFileProvider(attachment.getReference()));
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            final Object[] args = { attachment.getFilename(), UNKNOWN_NAME };
            if (attachment.getDoc() != null) {
                args[1] = attachment.getDoc().getFullName();
            }
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Exception while loading attachment archive {0} for document {1}", e, args);
        }
    }

    /**
     * Load an attachment archive from a specified location.
     *
     * @param attachment the attachment to load the archive for.
     * @param provider a means of gaining access to the location where the archive is stored.
     * @return an XWikiAttachmentArchive for the given attachment.
     * @throws IOException if the metadata cannot be found or there is a failure while parsing it.
     */
    XWikiAttachmentArchive loadArchive(final XWikiAttachment attachment, final AttachmentFileProvider provider)
        throws IOException
    {
        final File metaFile = provider.getAttachmentVersioningMetaFile();

        // If no meta file then assume no archive and return an empty archive.
        if (!metaFile.exists()) {
            return new ListAttachmentArchive(attachment);
        }

        final ReadWriteLock lock = this.fileTools.getLockForFile(metaFile);
        final List<XWikiAttachment> attachList;
        lock.readLock().lock();
        try {
            final InputStream is = new FileInputStream(metaFile);
            attachList = this.metaSerializer.parse(is);
            is.close();
        } finally {
            lock.readLock().unlock();
        }

        // Get the content file and lock for each revision.
        for (XWikiAttachment attach : attachList) {
            final File contentFile = provider.getAttachmentVersionContentFile(attach.getVersion());
            attach.setAttachment_content(new FilesystemAttachmentContent(contentFile, attach));
            attach.setContentStore(FileSystemStoreUtils.HINT);
            // Pass the document since it will be lost in the serialize/deserialize.
            attach.setDoc(attachment.getDoc(), false);
        }

        final ListAttachmentArchive out = new ListAttachmentArchive(attachList);
        out.setAttachment(attachment);
        return out;
    }

    /**
     * {@inheritDoc}
     * <p>
     * bTransaction cannot be used in this case, in order to have transaction atomicity, please use
     * getArchiveSaveRunnable() instead.
     * </p>
     *
     * @see AttachmentVersioningStore#saveArchive(XWikiAttachmentArchive, XWikiContext, boolean)
     */
    @Override
    public void saveArchive(final XWikiAttachmentArchive archive, final XWikiContext context,
        final boolean bTransaction) throws XWikiException
    {
        try {
            this.getArchiveSaveRunnable(archive, context).start();
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            final Object[] args = { UNKNOWN_NAME, UNKNOWN_NAME };
            if (archive.getAttachment() != null) {
                args[0] = archive.getAttachment().getFilename();
                if (archive.getAttachment().getDoc() != null) {
                    args[1] = archive.getAttachment().getDoc().getFullName();
                }
            }
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Exception while saving attachment archive {0} of document {1}", e, args);
        }
    }

    /**
     * Get a TransactionRunnable for saving or updating the current attachment. this runnable can be run with any
     * transaction including a VoidTransaction.
     *
     * @param archive The attachment archive to save.
     * @param context An XWikiContext used for getting the attachments from the archive with getRevision() and for
     *            getting the content from the attachments with getContentInputStream().
     * @return a new StartableTransactionRunnable for saving this attachment archive.
     * @throws XWikiException if versions of the attachment cannot be loaded form the archive.
     */
    public StartableTransactionRunnable getArchiveSaveRunnable(final XWikiAttachmentArchive archive,
        final XWikiContext context) throws XWikiException
    {
        return new AttachmentArchiveSaveRunnable(archive, this.fileTools,
            this.fileTools.getAttachmentFileProvider(archive.getAttachment().getReference()), this.metaSerializer,
            context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * bTransaction is ignored by this implementation. If you need to delete an archive inside of a larger transaction,
     * please use getArchiveDeleteRunnable()
     * </p>
     *
     * @see AttachmentVersioningStore#deleteArchive(XWikiAttachment, XWikiContext, boolean)
     */
    @Override
    public void deleteArchive(final XWikiAttachment attachment, final XWikiContext context, final boolean bTransaction)
        throws XWikiException
    {
        if (attachment == null) {
            throw new NullPointerException("The attachment to delete cannot be null");
        }
        try {
            final XWikiAttachmentArchive archive = this.loadArchive(attachment, context, bTransaction);
            this.getArchiveDeleteRunnable(archive).start();
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            final Object[] args = { attachment.getFilename(), UNKNOWN_NAME };
            if (attachment.getDoc() != null) {
                args[1] = attachment.getDoc().getFullName();
            }
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Exception while deleting attachment archive {0} from document {1}", e, args);
        }
    }

    /**
     * Get a TransactionRunnable for deleting an attachment archive. this runnable can be run with any transaction
     * including a VoidTransaction.
     *
     * @param archive The attachment archive to delete.
     * @return a StartableTransactionRunnable for deleting the attachment archive.
     */
    public StartableTransactionRunnable getArchiveDeleteRunnable(final XWikiAttachmentArchive archive)
    {
        if (archive == null) {
            throw new NullPointerException("The archive to delete cannot be null.");
        }
        if (archive.getAttachment() == null) {
            throw new IllegalArgumentException("Cannot delete an archive unless it is associated with an attachment.");
        }
        return new AttachmentArchiveDeleteRunnable(archive, this.fileTools,
            this.fileTools.getAttachmentFileProvider(archive.getAttachment().getReference()));
    }
}
