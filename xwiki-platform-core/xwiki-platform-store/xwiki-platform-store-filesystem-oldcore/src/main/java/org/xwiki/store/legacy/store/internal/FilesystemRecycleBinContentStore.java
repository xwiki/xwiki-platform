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
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.FileDeleteTransactionRunnable;
import org.xwiki.store.FileSaveTransactionRunnable;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocumentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRecycleBinContentStoreInterface;

/**
 * Implementation of {@link XWikiRecycleBinContentStoreInterface} for filesystem storage.
 *
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@Named(FileSystemStoreUtils.HINT)
@Singleton
public class FilesystemRecycleBinContentStore implements XWikiRecycleBinContentStoreInterface
{
    /**
     * Some utilities for getting attachment files, locks, and backup files.
     */
    @Inject
    private FilesystemStoreTools fileTools;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Provider<DeletedDocumentContentFileSerializer> serializerProvider;

    @Override
    public String getHint()
    {
        return FileSystemStoreUtils.HINT;
    }

    @Override
    public void save(XWikiDocument document, long index, boolean bTransaction) throws XWikiException
    {
        final XWikiContext xcontext = this.xcontextProvider.get();

        final XWikiHibernateTransaction transaction = new XWikiHibernateTransaction(xcontext);

        final File contentFile =
            this.fileTools.getDeletedDocumentFileProvider(document.getDocumentReferenceWithLocale(), index)
                .getDeletedDocumentContentFile();
        DeletedDocumentContentFileSerializer serializer = this.serializerProvider.get();
        serializer.init(document, StandardCharsets.UTF_8.name());
        new FileSaveTransactionRunnable(contentFile, fileTools.getTempFile(contentFile),
            fileTools.getBackupFile(contentFile), fileTools.getLockForFile(contentFile), serializer).runIn(transaction);

        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                "Exception while saving deleted document content.", e);
        }
    }

    @Override
    public XWikiDeletedDocumentContent get(DocumentReference reference, long index, boolean bTransaction)
        throws XWikiException
    {
        final File contentFile =
            this.fileTools.getDeletedDocumentFileProvider(reference, index).getDeletedDocumentContentFile();

        if (contentFile.exists()) {
            return new XWikiFileDeletedDocumentContent(contentFile, StandardCharsets.UTF_8);
        }

        return null;
    }

    @Override
    public void delete(DocumentReference reference, long index, boolean bTransaction) throws XWikiException
    {
        final XWikiContext xcontext = this.xcontextProvider.get();

        final XWikiHibernateTransaction transaction = new XWikiHibernateTransaction(xcontext);

        final File contentFile =
            this.fileTools.getDeletedDocumentFileProvider(reference, index).getDeletedDocumentContentFile();
        new FileDeleteTransactionRunnable(contentFile, this.fileTools.getBackupFile(contentFile),
            this.fileTools.getLockForFile(contentFile)).runIn(transaction);

        try {
            transaction.start();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                "Exception while deleting deleted document content.", e);
        }
    }
}
