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
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.internal.FileSystemStoreUtils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration for XWIKI-14697. Make sure all attachment have a store id.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component
@Named("R99000XWIKI14697")
@Singleton
public class R99000XWIKI14697DataMigration extends AbstractHibernateDataMigration
{
    private static final String QUERY_UPDATE_ATTACHMENT = "UPDATE XWikiAttachment WHERE id IN (?) SET xmlStore = ?";

    @Inject
    private FilesystemStoreTools fstools;

    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource configuration;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Make sure all existing attachments have a store id.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(99000);
    }

    @Override
    public void hibernateMigrate() throws XWikiException, DataMigrationException
    {
        // Make sure all hibernate attachment have the right store
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Void>()
        {
            @Override
            public Void doInHibernate(Session session) throws HibernateException
            {
                Query query = session.createQuery(
                    "UPDATE XWikiAttachment WHERE id IN (SELECT id FROM XWikiAttachmentContent) SET xmlStore = ?");
                query.setString(0, XWikiHibernateBaseStore.HINT);
                query.executeUpdate();

                return null;
            }
        });

        // Get all remaining attachments in the database and for each one try to find if filesystem store was used (and
        // if not use whatever is in the configuration)
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
            + " FROM XWikiAttachment as attachment, XWikiDocument as document WHERE attachment.xmlStore <> ?");
        selectQuery.setString(0, XWikiHibernateBaseStore.HINT);
        List<Object[]> attachments = selectQuery.list();

        WikiReference wikiReference = getXWikiContext().getWikiReference();

        List<Long> fileAttachments = new ArrayList<>(attachments.size());
        List<Long> configuredAttachments = new ArrayList<>(attachments.size());

        for (Object[] attachment : attachments) {
            Long id = (Long) attachment[0];
            String filename = (String) attachment[1];
            String fullName = (String) attachment[2];

            DocumentReference documentReference = this.resolver.resolve(fullName, wikiReference);

            if (this.fstools.attachmentExist(new AttachmentReference(filename, documentReference))) {
                fileAttachments.add(id);
            } else {
                configuredAttachments.add(id);
            }
        }

        // Set file store
        if (!fileAttachments.isEmpty()) {
            Query query = session.createQuery(QUERY_UPDATE_ATTACHMENT);
            query.setParameter(0, fileAttachments);
            query.setString(1, FileSystemStoreUtils.HINT);
            query.executeUpdate();
        }

        // Set configured store
        if (!configuredAttachments.isEmpty()) {
            String configuredStore = this.configuration.getProperty("xwiki.store.attachment.hint");

            if (configuredStore != null) {
                Query query = session.createQuery(QUERY_UPDATE_ATTACHMENT);
                query.setParameter(0, configuredAttachments);
                query.setString(1, configuredStore);
                query.executeUpdate();
            } else {
                this.logger.warn("The following attachment with the following ids have unknown content store: ",
                    configuredAttachments);
            }
        }
    }
}
