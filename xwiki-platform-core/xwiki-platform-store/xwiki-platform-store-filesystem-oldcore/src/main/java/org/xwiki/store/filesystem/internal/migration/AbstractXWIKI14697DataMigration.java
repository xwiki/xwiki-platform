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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrations for XWIKI-14697. Make sure all attachments have the right content and archive store id.
 *
 * @version $Id$
 * @since 9.10RC1
 */
public abstract class AbstractXWIKI14697DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    protected FilesystemStoreTools fstools;

    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    protected ConfigurationSource configuration;

    @Inject
    protected DocumentReferenceResolver<String> resolver;

    @Inject
    @Named("path")
    protected EntityReferenceSerializer<String> pathSerializer;

    @Inject
    protected Logger logger;

    protected final String tableName;

    protected final String updateQuery;

    /**
     * @param tableName the name of the table linked to the attachment table
     * @param fieldName the name of the field containing the store id
     */
    public AbstractXWIKI14697DataMigration(String tableName, String fieldName)
    {
        this.tableName = tableName;
        this.updateQuery = "UPDATE XWikiAttachment SET " + fieldName + " = :store WHERE id IN (:ids)";
    }

    @Override
    public void hibernateMigrate() throws XWikiException, DataMigrationException
    {
        // Get all non hibernate attachments in the database and for each one try to find if filesystem store was used
        // (and if not use whatever is in the configuration)
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Void>()
        {
            @Override
            public Void doInHibernate(Session session) throws HibernateException
            {
                doWork(session);

                return null;
            }
        });
    }

    private void doWork(Session session)
    {
        Query selectQuery = session.createQuery("SELECT attachment.id, attachment.filename, document.fullName"
            + " FROM XWikiAttachment as attachment, XWikiDocument as document"
            + " WHERE attachment.docId = document.id AND attachment.id NOT IN (SELECT id FROM " + this.tableName + ")");

        List<Object[]> attachments = selectQuery.list();

        if (!attachments.isEmpty()) {
            setStore(attachments, session);
        }
    }

    private void setStore(List<Object[]> attachments, Session session)
    {
        WikiReference wikiReference = getXWikiContext().getWikiReference();

        List<Long> fileAttachments = new ArrayList<>(attachments.size());

        List<Long> configuredAttachments = new ArrayList<>(attachments.size());

        for (Object[] attachment : attachments) {
            Long id = (Long) attachment[0];
            String filename = (String) attachment[1];
            String fullName = (String) attachment[2];

            DocumentReference documentReference = this.resolver.resolve(fullName, wikiReference);

            AttachmentReference attachmentReference = new AttachmentReference(filename, documentReference);

            if (isFile(attachmentReference)) {
                fileAttachments.add(id);
            } else {
                configuredAttachments.add(id);
            }
        }

        // Set file store
        setStore(session, fileAttachments, FileSystemStoreUtils.HINT);

        // Set configured store
        if (!configuredAttachments.isEmpty()) {
            String configuredStore = this.configuration.getProperty("xwiki.store.attachment.hint");

            if (configuredStore != null) {
                setStore(session, configuredAttachments, configuredStore);
            } else {
                this.logger.warn("The following attachment with the following ids have unknown store: ",
                    configuredAttachments);
            }
        }
    }

    protected abstract boolean isFile(AttachmentReference attachmentReference);

    protected void setStore(Session session, List<Long> values, String store)
    {
        if (!values.isEmpty()) {
            Query query = session.createQuery(this.updateQuery);
            query.setParameter("store", store);
            query.setParameterList("ids", values);
            query.executeUpdate();
        }
    }

    protected File getDocumentDir(final DocumentReference docRef)
    {
        final File path = new File(this.fstools.getStorageLocationFile(), this.pathSerializer.serialize(docRef, false));
        File docDir = new File(path, FilesystemStoreTools.DOCUMENT_DIR_NAME);

        // Add the locale
        Locale docLocale = docRef.getLocale();
        if (docLocale != null) {
            final File docLocalesDir = new File(docDir, FilesystemStoreTools.DOCUMENT_LOCALES_DIR_NAME);
            final File docLocaleDir = new File(docLocalesDir,
                docLocale.equals(Locale.ROOT) ? FilesystemStoreTools.DOCUMENT_LOCALES_ROOT_NAME : docLocale.toString());
            docDir = new File(docLocaleDir, FilesystemStoreTools.DOCUMENTLOCALE_DIR_NAME);
        }

        return docDir;
    }

    protected File getAttachmentDir(final AttachmentReference attachmentReference)
    {
        final File docDir = getDocumentDir(attachmentReference.getDocumentReference());
        final File attachmentsDir = new File(docDir, FilesystemStoreTools.ATTACHMENT_DIR_NAME);

        try {
            return new File(attachmentsDir, URLEncoder.encode(attachmentReference.getName(), "UTF8"));
        } catch (UnsupportedEncodingException e) {
            throw new HibernateException("UTF8 is unknown", e);
        }
    }
}
