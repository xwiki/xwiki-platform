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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
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
        return "See http://jira.xwiki.org/jira/browse/XWIKI-1459";
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
            return (XWikiHibernateVersioningStore) componentManager
                .lookup(XWikiVersioningStoreInterface.class, "hibernate");
        } catch (ComponentLookupException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                String.format("Unable to reach the versioning store for database %s", getXWikiContext().getDatabase()),
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
                    Statement stmt = session.connection().createStatement();
                    ResultSet rs;
                    try {
                        // We place an empty character in archives for documents that have already
                        // been migrated so that we can re-execute this data migration and not start over.
                        // Note that we cannot use NULL since in old databases (prior to 1.1) the
                        // XWD_ARCHIVE column had a not null constraint and since this column has disappeared in 1.2
                        // and after, the hibernate update script will not have modified the nullability of it...
                        // (see http://jira.xwiki.org/jira/browse/XWIKI-2074).
                        rs =
                            stmt
                                .executeQuery(
                                    "select XWD_ID, XWD_ARCHIVE, XWD_FULLNAME from xwikidoc"
                                    + " where (XWD_ARCHIVE is not null and XWD_ARCHIVE <> ' ') order by XWD_VERSION");
                    } catch (SQLException e) {
                        // most likely there is no XWD_ARCHIVE column, so migration is not needed
                        // is there easier way to find what column is not exist?
                        return null;
                    }

                    XWikiContext context = getXWikiContext();
                    XWikiHibernateVersioningStore versioningStore = getVersioningStore();
                    Transaction originalTransaction = versioningStore.getTransaction(context);
                    versioningStore.setSession(null, context);
                    versioningStore.setTransaction(null, context);
                    PreparedStatement deleteStatement =
                        session.connection().prepareStatement("update xwikidoc set XWD_ARCHIVE=' ' where XWD_ID=?");

                    while (rs.next()) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Updating document [{}]...", rs.getString(3));
                        }
                        long docId = Long.parseLong(rs.getString(1));
                        String sArchive = rs.getString(2);

                        // In some weird cases it can happen that the XWD_ARCHIVE field is empty
                        // (that shouldn't happen but we've seen it happening).
                        // In this case just ignore the archive...
                        if (sArchive.trim().length() != 0) {
                            XWikiDocumentArchive docArchive = new XWikiDocumentArchive(docId);
                            try {
                                docArchive.setArchive(sArchive);
                            } catch (XWikiException e) {
                                logger.warn(
                                    "The RCS archive for [{}] is broken. Internal error [{}]."
                                    + " The history for this document has been reset.",
                                    rs.getString(3), e.getMessage());
                            }
                            getVersioningStore().saveXWikiDocArchive(docArchive, true, context);
                        } else {
                            logger.warn("Empty revision found for document [{}]. Ignoring non-fatal error.",
                                rs.getString(3));
                        }
                        deleteStatement.setLong(1, docId);
                        deleteStatement.executeUpdate();
                    }
                    deleteStatement.close();
                    stmt.close();
                    versioningStore.setSession(session, context);
                    versioningStore.setTransaction(originalTransaction, context);
                } catch (SQLException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                }

                return Boolean.TRUE;
            }
        });
    }
}
