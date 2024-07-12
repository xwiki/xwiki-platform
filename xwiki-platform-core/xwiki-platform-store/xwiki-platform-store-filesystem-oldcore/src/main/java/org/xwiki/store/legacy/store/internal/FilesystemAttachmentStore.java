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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.store.FileDeleteTransactionRunnable;
import org.xwiki.store.FileSaveTransactionRunnable;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.StringStreamProvider;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.filesystem.internal.StoreFileUtils;
import org.xwiki.store.internal.FileSystemStoreUtils;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.doc.ListAttachmentArchive;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Filesystem based implementation of XWikiAttachmentStoreInterface.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component
@Named(FileSystemStoreUtils.HINT)
@Singleton
public class FilesystemAttachmentStore implements XWikiAttachmentStoreInterface
{
    /**
     * Tools for getting files to store given content in.
     */
    @Inject
    private FilesystemStoreTools fileTools;

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private AttachmentVersioningStore hibernateAttachmentVersioningStore;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public String getHint()
    {
        return FileSystemStoreUtils.HINT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation cannot operate in a larger transaction so it starts a new transaction no matter whether
     * bTransaction is true or false.
     * </p>
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#saveAttachmentContent( XWikiAttachment, XWikiContext,
     *      boolean)
     */
    @Override
    public void saveAttachmentContent(final XWikiAttachment attachment, final XWikiContext context,
        final boolean bTransaction) throws XWikiException
    {
        this.saveAttachmentContent(attachment, true, context, bTransaction);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation cannot operate in a larger transaction so it starts a new transaction no matter whether
     * bTransaction is true or false.
     * </p>
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#saveAttachmentContent( XWikiAttachment, boolean,
     *      XWikiContext, boolean)
     */
    @Override
    public void saveAttachmentContent(final XWikiAttachment attachment, final boolean updateDocument,
        final XWikiContext context, final boolean bTransaction) throws XWikiException
    {
        final XWikiHibernateTransaction transaction = new XWikiHibernateTransaction(context);
        getAttachmentContentSaveRunnable(attachment, updateDocument, context).runIn(transaction);
        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT, "Exception while saving attachment.", e);
        }
    }

    /**
     * Get a TransactionRunnable for saving the attachment content. If {@link XWikiAttachment#getAttachment_content()}
     * yields null, this runnable will do nothing.
     *
     * @param attachment the XWikiAttachment whose content should be saved.
     * @param updateDocument whether or not to update the document at the same time.
     * @param context the XWikiContext for the request.
     * @return a TransactionRunnable for saving the attachment content in an XWikiHibernateTransaction.
     * @throws XWikiException if thrown by AttachmentSaveTransactionRunnable()
     */
    private TransactionRunnable<XWikiHibernateTransaction> getAttachmentContentSaveRunnable(
        final XWikiAttachment attachment, final boolean updateDocument, final XWikiContext context)
        throws XWikiException
    {
        final XWikiAttachmentContent content = attachment.getAttachment_content();

        if (content == null) {
            // If content does not exist we should not blank the attachment.
            return new TransactionRunnable<>();
        }

        // This is the permanent location where the attachment content will go.
        final File attachFile =
            this.fileTools.getAttachmentFileProvider(attachment.getReference()).getAttachmentContentFile();

        return new AttachmentSaveTransactionRunnable(attachment, updateDocument, context, attachFile);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation cannot operate in a larger transaction so it starts a new transaction no matter whether
     * bTransaction is true or false.
     * </p>
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#saveAttachmentsContent( List, XWikiDocument, boolean,
     *      XWikiContext, boolean)
     */
    @Override
    public void saveAttachmentsContent(final List<XWikiAttachment> attachments, final XWikiDocument doc,
        final boolean updateDocument, final XWikiContext context, final boolean bTransaction) throws XWikiException
    {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        try {
            final XWikiHibernateTransaction transaction = new XWikiHibernateTransaction(context);

            for (XWikiAttachment attach : attachments) {
                this.getAttachmentContentSaveRunnable(attach, false, context).runIn(transaction);
            }

            // Save the parent document only once.
            if (updateDocument) {
                new TransactionRunnable<XWikiHibernateTransaction>()
                {
                    @Override
                    protected void onRun() throws Exception
                    {
                        context.getWiki().getStore().saveXWikiDoc(doc, context, false);
                    }
                }.runIn(transaction);
            }

            transaction.start();
        } catch (XWikiException e) {
            throw e;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT, "Exception while saving attachments", e);
        }
    }

    @Override
    public void loadAttachmentContent(final XWikiAttachment attachment, final XWikiContext context,
        final boolean bTransaction) throws XWikiException
    {
        File attachFile =
            this.fileTools.getAttachmentFileProvider(attachment.getReference()).getAttachmentContentFile();

        // Support links
        try {
            attachFile = StoreFileUtils.resolve(attachFile, true);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MISC,
                "Failed to resolve the attachment file link for file [{0}]", e, new Object[] {attachFile});
        }

        // Check if the final file exist
        if (!attachFile.exists()) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_FILENOTFOUND,
                String.format("The attachment [%s] (file %s) could not be found in the filesystem attachment store.",
                    attachment.getReference(), attachFile));
        }

        FilesystemAttachmentContent content = new FilesystemAttachmentContent(attachFile);
        content.setContentDirty(false);
        attachment.setAttachment_content(content);
        attachment.setContentStore(FileSystemStoreUtils.HINT);
    }

    @Override
    public boolean attachmentContentExists(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        File attachFile;
        try {
            attachFile = StoreFileUtils.resolve(
                this.fileTools.getAttachmentFileProvider(attachment.getReference()).getAttachmentContentFile(), false);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MISC,
                "Failed to resolve the attachment file link", e);
        }

        return attachFile.exists();
    }

    @Override
    public void deleteXWikiAttachment(final XWikiAttachment attachment, final XWikiContext context,
        final boolean bTransaction) throws XWikiException
    {
        this.deleteXWikiAttachment(attachment, true, context, bTransaction);
    }

    @Override
    public void deleteXWikiAttachment(final XWikiAttachment attachment, final boolean parentUpdate,
        final XWikiContext context, final boolean bTransaction) throws XWikiException
    {
        final XWikiHibernateTransaction transaction = new XWikiHibernateTransaction(context);
        this.getAttachmentDeleteRunnable(attachment, parentUpdate, context).runIn(transaction);
        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Exception while deleting attachment.", e);
        }
    }

    /**
     * Get a TransactionRunnable for deleting an attachment.
     *
     * @param attachment the XWikiAttachment to delete.
     * @param updateDocument whether or not to update the document at the same time.
     * @param context the XWikiContext for the request.
     * @return a TransactionRunnable for deleting the attachment which must be run inside of an
     *         XWikiHibernateTransaction
     * @throws XWikiException if unable to load the attachment archive to delete.
     */
    private TransactionRunnable<XWikiHibernateTransaction> getAttachmentDeleteRunnable(final XWikiAttachment attachment,
        final boolean updateDocument, final XWikiContext context) throws XWikiException
    {
        final File attachFile =
            this.fileTools.getAttachmentFileProvider(attachment.getReference()).getAttachmentContentFile();

        return new AttachmentDeleteTransactionRunnable(attachment, updateDocument, context, attachFile);
    }

    @Override
    public void cleanUp(XWikiContext context)
    {
        // Do nothing.
    }

    /* ---------------------------- Nested Classes. ---------------------------- */

    /**
     * A TransactionRunnable for saving an attachment.
     */
    private class AttachmentSaveTransactionRunnable extends TransactionRunnable<XWikiHibernateTransaction>
    {
        /**
         * Construct a TransactionRunnable for saving the attachment content.
         * 
         * @param attachment the XWikiAttachment whose content should be saved.
         * @param updateDocument whether or not to update the document at the same time.
         * @param context the XWikiContext for the request.
         * @param attachFile the File to store the attachment in.
         * @param tempFile the File to put the attachment content in until the transaction is complete.
         * @param backupFile the File to backup the content of the existing attachment in.
         * @param lock this Lock will be locked while the attachment file is being written to.
         * @throws XWikiException if thrown by {@link XWikiAttachment#updateContentArchive(XWikiContext)} or
         *             {@link FilesystemAttachmentVersioningStore# getArchiveSaveRunnable(XWikiAttachmentArchive,
         *             XWikiContext)}
         */
        AttachmentSaveTransactionRunnable(final XWikiAttachment attachment, final boolean updateDocument,
            final XWikiContext context, final File attachFile) throws XWikiException
        {
            boolean link = false;

            //////////////////
            // ARCHIVE

            // If the versioning store supports TransactionRunnable then use it, otherwise don't.
            AttachmentVersioningStore avs = resolveAttachmentVersioningStore(attachment, context);
            if (avs instanceof FilesystemAttachmentVersioningStore favs) {
                XWikiAttachmentArchive archive = attachment.getAttachment_archive();
                if (archive == null) {
                    // If first save then create a new archive.
                    archive = new ListAttachmentArchive(attachment);

                    // The archive is identical to the current version by definition, so don't duplicate the content
                    link = true;
                } else {
                    // If the last archive is identical to the current version (which is the case except in very rare
                    // cases), don't duplicate content
                    XWikiAttachment archiveAttachment =
                        archive.getRevision(attachment, attachment.getVersion(), context);
                    // Really comparing the content could be very expensive so we assume comparing the size and date are
                    // enough
                    if (archiveAttachment != null && archiveAttachment.getDate() == attachment.getDate()
                        && archiveAttachment.getLongSize() == attachment.getLongSize()) {
                        link = true;
                    }
                }

                favs.getArchiveSaveRunnable(archive, context).runIn(this);
            } else {
                new TransactionRunnable<XWikiHibernateTransaction>()
                {
                    @Override
                    protected void onRun() throws XWikiException
                    {
                        avs.saveArchive(attachment.getAttachment_archive(), context, false);
                    }
                }.runIn(this);
            }

            //////////////////
            // CURRENT

            File linkAttachFile = StoreFileUtils.getLinkFile(attachFile);

            File finalAttachFile;
            File otherAttachFile;
            StreamProvider streamProvider;
            if (link) {
                // Create a link to the current version
                finalAttachFile = linkAttachFile;
                streamProvider = new StringStreamProvider(fileTools.getLinkContent(attachment), StandardCharsets.UTF_8);
                otherAttachFile = attachFile;
            } else {
                // Save the content as is
                finalAttachFile = attachFile;
                streamProvider = new AttachmentContentStreamProvider(attachment, context);
                otherAttachFile = linkAttachFile;
            }

            // Save the attachment file
            new FileSaveTransactionRunnable(finalAttachFile, fileTools.getTempFile(finalAttachFile),
                fileTools.getBackupFile(finalAttachFile), fileTools.getLockForFile(finalAttachFile), streamProvider)
                    .runIn(this);

            // Also delete any file remaining at the other location
            new FileDeleteTransactionRunnable(otherAttachFile, fileTools.getTempFile(otherAttachFile),
                fileTools.getLockForFile(otherAttachFile)).runIn(this);

            //////////////////
            // DOCUMENT

            // If updating of the parent document is required then add a TransactionRunnable to do that.
            if (updateDocument) {
                final XWikiStoreInterface store = context.getWiki().getStore();
                final XWikiDocument doc = attachment.getDoc();
                new TransactionRunnable<XWikiHibernateTransaction>()
                {
                    @Override
                    protected void onRun() throws XWikiException
                    {
                        store.saveXWikiDoc(doc, context, false);
                    }
                }.runIn(this);
            }
        }
    }

    /**
     * A TransactionRunnable for deleting an attachment.
     */
    private class AttachmentDeleteTransactionRunnable extends TransactionRunnable<XWikiHibernateTransaction>
    {
        /**
         * The XWikiAttachment whose content should be saved.
         */
        private final XWikiAttachment attachment;

        /**
         * Whether or not to update the document at the same time.
         */
        private final boolean updateDocument;

        /**
         * The XWikiContext for the request.
         */
        private final XWikiContext context;

        /**
         * Construct a TransactionRunnable for deleting the attachment.
         *
         * @param attachment the XWikiAttachment to delete
         * @param updateDocument whether or not to update the document at the same time.
         * @param context the XWikiContext for the request.
         * @param attachFile the file to where the attachment content is stored.
         * @throws XWikiException if unable to load the archive for the attachment to delete.
         */
        AttachmentDeleteTransactionRunnable(final XWikiAttachment attachment, final boolean updateDocument,
            final XWikiContext context, final File attachFile) throws XWikiException
        {
            // Delete both the standard and link location
            File linkAtachFile = StoreFileUtils.getLinkFile(attachFile);
            new FileDeleteTransactionRunnable(attachFile, fileTools.getBackupFile(attachFile),
                fileTools.getLockForFile(attachFile)).runIn(this);
            new FileDeleteTransactionRunnable(linkAtachFile, fileTools.getBackupFile(linkAtachFile),
                fileTools.getLockForFile(linkAtachFile)).runIn(this);

            // If the store supports deleting in the same transaction then do it.
            final AttachmentVersioningStore avs = context.getWiki().getDefaultAttachmentArchiveStore();

            if (avs instanceof FilesystemAttachmentVersioningStore) {
                final FilesystemAttachmentVersioningStore favs = (FilesystemAttachmentVersioningStore) avs;
                favs.getArchiveDeleteRunnable(attachment.loadArchive(context)).runIn(this);
            } else {
                new TransactionRunnable<HibernateTransaction>()
                {
                    @Override
                    protected void onRun() throws XWikiException
                    {
                        avs.deleteArchive(attachment, context, false);
                    }
                }.runIn(this);
            }

            this.context = context;
            this.attachment = attachment;
            this.updateDocument = updateDocument;
        }

        @Override
        protected void onRun() throws Exception
        {
            // TODO: When the rest of storage is rewritten using TransactionRunnable,
            // this method should be disolved.

            final Session session = this.context.getWiki().getHibernateStore().getSession(this.context);

            // Delete the content from the attachment.
            // In case it was stored in the database by XWikiHibernateAttachmentStore.
            session.delete(new XWikiAttachmentContent(this.attachment));

            // Update the document if required.
            if (this.updateDocument) {
                final String filename = this.attachment.getFilename();
                final List<XWikiAttachment> list = attachment.getDoc().getAttachmentList();
                for (int i = 0; i < list.size(); i++) {
                    if (filename.equals(list.get(i).getFilename())) {
                        list.remove(i);
                        break;
                    }
                }
                this.context.getWiki().getStore().saveXWikiDoc(this.attachment.getDoc(), this.context, false);
            }

            // Delete the attachment metadata.
            session.delete(this.attachment);
        }
    }

    private AttachmentVersioningStore resolveAttachmentVersioningStore(XWikiAttachment attachment,
        XWikiContext xcontext)
    {
        if (!attachment.isArchiveStoreSet()) {
            return xcontext.getWiki().getDefaultAttachmentArchiveStore();
        }

        AttachmentVersioningStore store = getAttachmentVersioningStore(attachment.getArchiveStore());

        return store != null ? store : this.hibernateAttachmentVersioningStore;
    }

    private AttachmentVersioningStore getAttachmentVersioningStore(String storeType)
    {
        if (storeType != null && !storeType.equals(XWikiHibernateBaseStore.HINT)) {
            try {
                return this.componentManager.getInstance(AttachmentVersioningStore.class, storeType);
            } catch (ComponentLookupException e) {
                this.logger.warn("Can't find attachment versionning store for type [{}]", storeType, e);
            }
        }

        return null;
    }
}
