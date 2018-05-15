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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Base class used by migrations on store type field.
 *
 * @version $Id$
 * @since 10.4RC1
 */
public abstract class AbstractStoreTypeDataMigration extends AbstractHibernateDataMigration
{
    @Inject
    protected FilesystemStoreTools fstools;

    @Inject
    protected DocumentReferenceResolver<String> resolver;

    @Inject
    protected Logger logger;

    protected final String tableName;

    protected final String updateQuery;

    /**
     * @param tableName the name of the table linked to the attachment table
     * @param fieldName the name of the field containing the store id
     */
    public AbstractStoreTypeDataMigration(String tableName, String fieldName)
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

        List<Long> otherAttachments = new ArrayList<>(attachments.size());

        for (Object[] attachment : attachments) {
            Long id = (Long) attachment[0];
            String filename = (String) attachment[1];
            String fullName = (String) attachment[2];

            DocumentReference documentReference = this.resolver.resolve(fullName, wikiReference);

            AttachmentReference attachmentReference = new AttachmentReference(filename, documentReference);

            if (isFile(attachmentReference)) {
                fileAttachments.add(id);
            } else {
                otherAttachments.add(id);
            }
        }

        // Set file store
        setStore(session, fileAttachments, FileSystemStoreUtils.HINT);

        // Set configured store
        setOtherStore(otherAttachments, session);
    }

    protected void setOtherStore(List<Long> otherAttachments, Session session)
    {

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
}
