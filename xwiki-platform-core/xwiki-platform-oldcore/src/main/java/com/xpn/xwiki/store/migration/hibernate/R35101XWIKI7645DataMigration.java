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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.web.Utils;

/**
 * Migration for XWIKI-7564: Manually change the SQL type of long binary columns from LONG RAW to proper BLOBs when the
 * underlying database is Oracle. More specifically, this migrator changes the attachment content and attachment archive
 * columns from LONG RAW to BLOB, and rebuilds the indexes on those tables afterwards. The columns must be switched to
 * BLOB since this is the expected column type when using the new mapping files. Rebuilding the indexes is needed
 * because changing a table's columns automatically invalidates the indexes on those tables, and with unusable indexes
 * any new insertion in those tables will trigger an exception.
 *
 * @version $Id$
 * @since 3.5.1
 */
@Component
@Named("R35101XWIKI7645")
@Singleton
public class R35101XWIKI7645DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "See https://jira.xwiki.org/browse/XWIKI-7645";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(35101);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        boolean shouldExecute = false;
        try {
            getStore().beginTransaction(getXWikiContext());
            // Run this migration if the database isn't new
            shouldExecute = getStore().getDatabaseProductName() == DatabaseProduct.ORACLE;
            getStore().endTransaction(getXWikiContext(), false);
        } catch (XWikiException ex) {
            // Shouldn't happen, ignore
        } catch (DataMigrationException ex) {
            // Shouldn't happen, ignore
        }
        return shouldExecute;
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.doWork(new R35101Work());
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Hibernate {@link Work} class doing the actual work of this migrator.
     *
     * @version $Id$
     */
    private static final class R35101Work implements Work
    {
        @Override
        public void execute(Connection connection) throws SQLException
        {
            String[][] tablesToFix = new String[][] {
                { "XWIKIATTACHMENT_CONTENT", "XWA_CONTENT" },
                { "XWIKIATTACHMENT_ARCHIVE", "XWA_ARCHIVE" } };
            Statement stmt = connection.createStatement();
            PreparedStatement getIndexesQuery = connection.prepareStatement(
                "SELECT index_name FROM all_indexes WHERE table_owner=? AND table_name=? AND index_type='NORMAL'");

            for (String[] table : tablesToFix) {
                try {
                    stmt.execute("ALTER TABLE " + table[0] + " MODIFY (" + table[1] + " blob)");
                } catch (SQLException ex) {
                    // This exception is thrown when this migrator isn't really needed. This happens when migrating from
                    // a version between 3.2 and 3.5, which do use the proper table structure, but we can't easily
                    // distinguish between a pre-3.2 database and a post-3.2 database.
                    // ORA-22859 on Oracle 11g, but ORA-22858 on Oracle 10g should be ignored.
                    if (ex.getMessage().contains("ORA-22859") || ex.getMessage().contains("ORA-22858")) {
                        return;
                    } else {
                        throw ex;
                    }
                }
                getIndexesQuery.setString(1, getSchemaFromWikiName(Utils.getContext().getWikiId()));
                getIndexesQuery.setString(2, table[0]);
                ResultSet indexes = getIndexesQuery.executeQuery();
                while (indexes.next()) {
                    String index = indexes.getString(1);
                    stmt.execute("ALTER INDEX " + index + " REBUILD");
                }
            }
        }

        /**
         * The actual schema name isn't always the same as the virtual wiki name. Two settings in xwiki.cfg can change
         * this. For the main wiki, by default "xwiki" is used as the schema name, but the {@code xwiki.db} setting can
         * override this. Also, the {@code xwiki.db.prefix} can define a prefix that should be appended to all schema
         * names, so that all the wikis in a farm can have a common prefix. And on Oracle, the schema name is always
         * uppercased.
         *
         * @param wikiName the name of the virtual wiki for which to compute the schema name
         * @return the schema name corresponding to the virtual wiki, in UPPERCASE
         */
        private String getSchemaFromWikiName(String wikiName)
        {
            if (wikiName == null) {
                return null;
            }

            XWikiContext context = Utils.getContext();
            XWiki wiki = context.getWiki();

            String schema;
            if (context.isMainWiki(wikiName)) {
                // Main wiki database, by default is "xwiki", but can be changed in xwiki.cfg
                schema = wiki.Param("xwiki.db");
                if (schema == null) {
                    schema = wikiName;
                }
            } else {
                // Virtual wiki database name is the name of the wiki
                schema = wikiName.replace('-', '_');
            }

            // Apply an optional prefix defined in xwiki.cfg
            String prefix = wiki.Param("xwiki.db.prefix", "");
            schema = prefix + schema;

            // Oracle schema names are UPPERCASE
            return schema.toUpperCase();
        }

    }
}
