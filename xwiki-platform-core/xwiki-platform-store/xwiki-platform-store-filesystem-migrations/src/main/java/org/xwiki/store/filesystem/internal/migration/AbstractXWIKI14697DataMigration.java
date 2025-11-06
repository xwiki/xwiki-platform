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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.Session;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;

import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;

/**
 * Migrations for XWIKI-14697. Make sure all attachments have the right content and archive store id.
 *
 * @version $Id$
 * @since 9.10RC1
 */
public abstract class AbstractXWIKI14697DataMigration extends AbstractStoreTypeDataMigration
{
    private static final String PATH_SEPARATOR = "/";

    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    protected ConfigurationSource configuration;

    @Inject
    @Named("path")
    protected EntityReferenceSerializer<String> pathSerializer;

    /**
     * @param tableName the name of the table linked to the attachment table
     * @param fieldName the name of the field containing the store id
     */
    public AbstractXWIKI14697DataMigration(String tableName, String fieldName)
    {
        super(tableName, fieldName);
    }

    @Override
    protected void setOtherStore(List<Long> otherAttachments, Session session)
    {
        // Set configured store
        if (!otherAttachments.isEmpty()) {
            String configuredStore = this.configuration.getProperty("xwiki.store.attachment.hint");

            if (configuredStore != null) {
                setStore(session, otherAttachments, configuredStore);
            } else {
                this.logger.warn("The attachments with the following ids have unknown store: {}", otherAttachments);
            }
        }
    }

    protected BlobPath getDocumentDir(final DocumentReference docRef)
    {
        String serialized = this.pathSerializer.serialize(docRef, false);
        BlobPath path = BlobPath.parse(PATH_SEPARATOR + serialized);
        BlobPath docDir = path.resolve(THIS_DIR_NAME);

        // Add the locale
        Locale docLocale = docRef.getLocale();
        if (docLocale != null) {
            final BlobPath docLocalesDir = docDir.resolve(FilesystemStoreTools.DOCUMENT_LOCALES_DIR_NAME);
            final BlobPath docLocaleDir = docLocalesDir.resolve(
                docLocale.equals(Locale.ROOT) ? DOCUMENT_LOCALE_ROOT_NAME : docLocale.toString());
            docDir = docLocaleDir.resolve(THIS_DIR_NAME);
        }

        return docDir;
    }

    protected BlobPath getAttachmentDir(final AttachmentReference attachmentReference)
    {
        final BlobPath docDir = getDocumentDir(attachmentReference.getDocumentReference());
        final BlobPath attachmentsDir = docDir.resolve(FilesystemStoreTools.ATTACHMENTS_DIR_NAME);

        return attachmentsDir.resolve(URLEncoder.encode(attachmentReference.getName(), StandardCharsets.UTF_8));
    }
}
