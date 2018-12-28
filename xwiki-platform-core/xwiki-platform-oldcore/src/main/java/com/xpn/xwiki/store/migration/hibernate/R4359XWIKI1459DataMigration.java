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

package com.xpn.xwiki.store.migration.hibernate;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI1459: keep document history in a separate table.
 *
 * @version $Id$
 */
@Component
@Named("R4359XWIKI1459")
@Singleton
public class R4359XWIKI1459DataMigration extends AbstractHibernateDataMigration
{
    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "See https://jira.xwiki.org/browse/XWIKI-1459";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(4359);
    }

    /**
     * @return version store system for execute store-specific actions.
     * @throws XWikiException if the store could not be reached
     */
    private XWikiHibernateVersioningStore getVersioningStore() throws XWikiException
    {
        try {
            return (XWikiHibernateVersioningStore) this.componentManager
                .getInstance(XWikiVersioningStoreInterface.class, XWikiHibernateBaseStore.HINT);
        } catch (ComponentLookupException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                String.format("Unable to reach the versioning store for database %s", getXWikiContext().getWikiId()),
                e);
        }
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // migrate data
        getStore().executeWrite(getXWikiContext(), true, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                try {
                    List<Object[]> rs;
                    try {
                        // We place an empty character in archives for documents that have already
                        // been migrated so that we can re-execute this data migration and not start over.
                        // Note that we cannot use NULL since in old databases (prior to 1.1) the
                        // XWD_ARCHIVE column had a not null constraint and since this column has disappeared in 1.2
                        // and after, the hibernate update script will not have modified the nullability of it...
                        // (see https://jira.xwiki.org/browse/XWIKI-2074).
                        rs = session.createSQLQuery("select XWD_ID, XWD_ARCHIVE, XWD_FULLNAME from xwikidoc"
                            + " where (XWD_ARCHIVE is not null and XWD_ARCHIVE <> ' ') order by XWD_VERSION").list();
                    } catch (HibernateException e) {
                        // most likely there is no XWD_ARCHIVE column, so migration is not needed
                        // is there easier way to find what column is not exist?
                        return null;
                    }

                    XWikiContext context = getXWikiContext();
                    XWikiHibernateVersioningStore versioningStore = getVersioningStore();
                    Transaction originalTransaction = versioningStore.getTransaction(context);
                    versioningStore.setSession(null, context);
                    versioningStore.setTransaction(null, context);
                    SQLQuery deleteStatement =
                            session.createSQLQuery("update xwikidoc set XWD_ARCHIVE=' ' where XWD_ID=?");

                    for (Object[] result : rs) {
                        if (R4359XWIKI1459DataMigration.this.logger.isInfoEnabled()) {
                            R4359XWIKI1459DataMigration.this.logger
                                    .info("Updating document [{}]...", result[2].toString());
                        }
                        long docId = Long.parseLong(result[0].toString());
                        String sArchive = result[1].toString();

                        // In some weird cases it can happen that the XWD_ARCHIVE field is empty
                        // (that shouldn't happen but we've seen it happening).
                        // In this case just ignore the archive...
                        if (sArchive.trim().length() != 0) {
                            XWikiDocumentArchive docArchive = new XWikiDocumentArchive(docId);
                            try {
                                docArchive.setArchive(sArchive);
                            } catch (XWikiException e) {
                                R4359XWIKI1459DataMigration.this.logger.warn(
                                    "The RCS archive for [{}] is broken. Internal error [{}]."
                                        + " The history for this document has been reset.",
                                    result[2].toString(), e.getMessage());
                            }
                            getVersioningStore().saveXWikiDocArchive(docArchive, true, context);
                        } else {
                            R4359XWIKI1459DataMigration.this.logger.warn(
                                    "Empty revision found for document [{}]. Ignoring non-fatal error.",
                                    result[2].toString());
                        }
                        deleteStatement.setLong(1, docId);
                        deleteStatement.executeUpdate();
                    }
                    versioningStore.setSession(session, context);
                    versioningStore.setTransaction(originalTransaction, context);
                } catch (HibernateException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                }

                return Boolean.TRUE;
            }
        });
    }
}
