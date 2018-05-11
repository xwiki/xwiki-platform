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

package org.xwiki.store.filesystem.internal.migration;

import java.io.File;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileSystemStoreUtils;

/**
 * Migrations for XWIKI-15249. Make sure all attachments have the right content and archive store id.
 *
 * @version $Id$
 * @since 10.4RC1
 */
public abstract class AbstractXWIKI15249DataMigration extends AbstractStoreTypeDataMigration
{
    @Inject
    @Named(FileSystemStoreUtils.HINT)
    protected EntityReferenceSerializer<String> fileEntitySerializer;

    /**
     * @param tableName the name of the table linked to the attachment table
     * @param fieldName the name of the field containing the store id
     */
    public AbstractXWIKI15249DataMigration(String tableName, String fieldName)
    {
        super(tableName, fieldName);
    }

    protected File getDocumentContentDir(final DocumentReference documentReference)
    {
        File documentDir = new File(this.fstools.getStorageLocationFile(),
            this.fileEntitySerializer.serialize(documentReference, true));
        File documentContentDir = new File(documentDir, FilesystemStoreTools.DOCUMENT_DIR_NAME);

        // Add the locale
        Locale documentLocale = documentReference.getLocale();
        if (documentLocale != null) {
            final File documentLocalesDir =
                new File(documentContentDir, FilesystemStoreTools.DOCUMENT_LOCALES_DIR_NAME);
            final File documentLocaleDir = new File(documentLocalesDir, documentLocale.equals(Locale.ROOT)
                ? FilesystemStoreTools.DOCUMENT_LOCALES_ROOT_NAME : documentLocale.toString());
            documentContentDir = new File(documentLocaleDir, FilesystemStoreTools.DOCUMENTLOCALE_DIR_NAME);
        }

        return documentContentDir;
    }

    protected File getAttachmentDir(final AttachmentReference attachmentReference)
    {
        File docDir = getDocumentContentDir(attachmentReference.getDocumentReference());
        File attachmentsDir = new File(docDir, FilesystemStoreTools.ATTACHMENT_DIR_NAME);

        return new File(attachmentsDir, FileSystemStoreUtils.encode(attachmentReference.getName(), true));
    }
}
