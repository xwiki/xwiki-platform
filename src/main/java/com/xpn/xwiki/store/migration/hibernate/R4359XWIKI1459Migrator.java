package com.xpn.xwiki.store.migration.hibernate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI1459: keep document history in a separate table
 * @version $Id: $ 
 */
public class R4359XWIKI1459Migrator extends AbstractXWikiHibernateMigrator
{
    /** {@inheritDoc} */
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(4359);
    }
    /** {@inheritDoc} */
    public void migrate(XWikiHibernateMigrationManager manager, final XWikiContext context) throws XWikiException
    {
        // migrate data
        if (manager.getStore(context).executeWrite(context, true, new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                try {
                    Statement stmt = session.connection().createStatement();
                    ResultSet rs;
                    try {
                        rs = stmt.executeQuery("select XWD_ID, XWD_ARCHIVE from xwikidoc");
                    } catch (SQLException e) {
                        // most likely there is no XWD_ARCHIVE column, so migration is not needed
                        // is there easier way to find what column is not exist?
                        return null;
                    }
                    while (rs.next()) {
                        long docId = Long.parseLong(rs.getString(1));
                        String sArchive = rs.getString(2);
                        if (sArchive==null)
                            continue;
                        XWikiDocumentArchive docArchive = new XWikiDocumentArchive(docId);
                        docArchive.setArchive(sArchive);
                        context.getWiki().getVersioningStore().saveXWikiDocArchive(docArchive, false, context);
                    }
                    stmt.close();
                } catch (SQLException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, 
                        XWikiException.ERROR_XWIKI_STORE_MIGRATION, "XWIKI-1459 migration failed", e);
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
