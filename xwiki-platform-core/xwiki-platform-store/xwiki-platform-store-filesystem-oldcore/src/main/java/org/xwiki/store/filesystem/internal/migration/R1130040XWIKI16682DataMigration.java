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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migrations for XWIKI-16682. Fix attachments content store id missed because of the bug.
 *
 * @version $Id$
 * @since 11.3.4
 * @since 11.7.1
 * @since 11.8RC1
 */
@Component
// Should be 1103040 but it has to be bigger than WatchlistLeftoversCleaner...
@Named("R1130040XWIKI16682")
@Singleton
public class R1130040XWIKI16682DataMigration extends AbstractStoreTypeDataMigration
{
    /**
     * The default constructor.
     */
    public R1130040XWIKI16682DataMigration()
    {
        super("XWikiAttachmentContent", "contentStore");
    }

    @Override
    public String getDescription()
    {
        return "Fix attachments content store id missed because of a bug in migration R1100000XWIKI15620";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // Should be 1103040 but it has to be bigger than WatchlistLeftoversCleaner...
        return new XWikiDBVersion(1130040);
    }

    @Override
    public void migrate() throws DataMigrationException
    {
        // This migration should only be executed if upgrading from > 11.0
        int version = getCurrentDBVersion().getVersion();
        if (version >= R1100000XWIKI15620DataMigration.VERSION) {
            super.migrate();
        } else {
            this.logger
                .info("Skipping the migration (it's only needed when migrating from a version greater than 11.0)");
        }
    }

    @Override
    protected void doWork(Session session)
    {
        org.hibernate.query.Query selectQuery =
            session.createQuery("SELECT attachment.id, attachment.filename, document.fullName"
                + " FROM XWikiAttachment as attachment, XWikiDocument as document"
                + " WHERE attachment.docId = document.id AND (attachment.contentStore is NULL)");

        List<Object[]> attachments = selectQuery.list();

        if (!attachments.isEmpty()) {
            try {
                setStore(attachments, session);
            } catch (IOException e) {
                throw new HibernateException("Failed to fix missed attachment", e);
            }
        }
    }

    private void setStore(List<Object[]> attachments, Session session) throws IOException
    {
        WikiReference wikiReference = getXWikiContext().getWikiReference();

        List<Long> fileAttachments = new ArrayList<>(attachments.size());

        for (Object[] attachment : attachments) {
            Long id = (Long) attachment[0];
            String filename = (String) attachment[1];
            String fullName = (String) attachment[2];

            DocumentReference documentReference = this.resolver.resolve(fullName, wikiReference);

            AttachmentReference attachmentReference = new AttachmentReference(filename, documentReference);

            File attachmentDirectory = this.fstools.getAttachmentDir(attachmentReference);

            if (attachmentDirectory.exists() && R1100000XWIKI15620DataMigration
                .migrateAttachmentFiles(attachmentDirectory, attachmentReference.getName(), this.logger)) {
                // Update the content store id
                fileAttachments.add(id);
            }
        }

        // Set file store
        setStore(session, fileAttachments, FileSystemStoreUtils.HINT);
    }

    @Override
    protected boolean isFile(AttachmentReference attachmentReference)
    {
        // Not used
        return false;
    }
}
