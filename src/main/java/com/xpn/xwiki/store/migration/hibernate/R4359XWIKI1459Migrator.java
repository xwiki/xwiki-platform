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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI1459: keep document history in a separate table
 * @version $Id: $ 
 */
public class R4359XWIKI1459Migrator extends AbstractXWikiHibernateMigrator
{
    /** logger. */
    private static final Log LOG = LogFactory.getLog(R4359XWIKI1459Migrator.class);

    /**
     * {@inheritDoc}
     * @see AbstractXWikiHibernateMigrator#getName()
     */
    public String getName()
    {
        return "R4359XWIKI1459";
    }

    /**
     * {@inheritDoc}
     * @see com.xpn.xwiki.store.migration.hibernate.AbstractXWikiHibernateMigrator#getDescription()
     */
    public String getDescription()
    {
        return "See http://jira.xwiki.org/jira/browse/XWIKI-1459";
    }

    /** {@inheritDoc} */
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(4359);
    }
    /** {@inheritDoc} */
    public void migrate(XWikiHibernateMigrationManager manager, final XWikiContext context) throws XWikiException
    {
        migrate(manager, context, LOG);
    }

    /**
     * The reason for the logger is to let the R6079XWIKI1878Migrator reuse the same code.
     */
    protected void migrate(XWikiHibernateMigrationManager manager, final XWikiContext context, final Log logger)
        throws XWikiException
    {
        // migrate data
        if (manager.getStore(context).executeWrite(context, true, new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                try {
                    Statement stmt = session.connection().createStatement();
                    ResultSet rs;
                    try {
                        rs = stmt.executeQuery("select XWD_ID, XWD_ARCHIVE, XWD_FULLNAME from xwikidoc where XWD_ARCHIVE is not null order by XWD_VERSION");
                    } catch (SQLException e) {
                        // most likely there is no XWD_ARCHIVE column, so migration is not needed
                        // is there easier way to find what column is not exist?
                        return null;
                    }
                    Transaction originalTransaction = ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).getTransaction(context);
                    ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).setSession(null, context);
                    ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).setTransaction(null, context);
                    PreparedStatement deleteStatement = session.connection().prepareStatement("update xwikidoc set XWD_ARCHIVE=null where XWD_ID=?");

                    while (rs.next()) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Updating document [" + rs.getString(3) + "]...");
                        }
                        long docId = Long.parseLong(rs.getString(1));
                        String sArchive = rs.getString(2);
                        XWikiDocumentArchive docArchive = new XWikiDocumentArchive(docId);
                        docArchive.setArchive(sArchive);
                        context.getWiki().getVersioningStore().saveXWikiDocArchive(docArchive, true, context);
                        deleteStatement.setLong(1, docId);
                        deleteStatement.executeUpdate();
                    }
                    deleteStatement.close();
                    stmt.close();
                    ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).setSession(session, context);
                    ((XWikiHibernateVersioningStore)context.getWiki().getVersioningStore()).setTransaction(originalTransaction, context);
                } catch (SQLException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                }
                return Boolean.TRUE;
            }
        })==null) return;
        // drop old column
        // I think it is not needed. this column do not harm, but may be useful. User can delete it by self in any time.
        /*manager.getStore(context).executeWrite(context, true, new HibernateCallBack() {
            public Object doInHibernate(Session session) throws Exception
            {
                Connection connection = session.connection();
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("ALTER TABLE xwikidoc DROP COLUMN XWD_ARCHIVE");
                connection.commit();
                return null;
            }
        });*/
    }
}
