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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-7771: Fix the LOBs wrongly created by the 3.2-3.5 mapping files for PostgreSQL.
 *
 * @version $Id$
 * @since 3.5.1
 */
@Component
@Named("R35102XWIKI7771")
@Singleton
public class R35102XWIKI7771DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "See http://jira.xwiki.org/browse/XWIKI-7771";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(35102);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        boolean shouldExecute = false;
        try {
            getStore().beginTransaction(getXWikiContext());
            // Run this migration if the database isn't new
            shouldExecute = (startupVersion.getVersion() > 0
                && getStore().getDatabaseProductName() == DatabaseProduct.POSTGRESQL);
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
                session.doWork(new R35102Work("xwikircs", "xwr_patch", "xwr_docid", "History for document with ID"));
                session.doWork(new R35102Work("xwikirecyclebin", "xdd_xml", "xdd_id", "Deleted document with ID"));
                session.doWork(new R35102Work("xwikiattrecyclebin", "xda_xml", "xda_id", "Deleted attachment with ID"));
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Hibernate {@link Work} class reads back LOB data into Text fields.
     *
     * @version $Id$
     */
    private static class R35102Work implements Work
    {
        /** The name of the table to fix. */
        private String tableName;

        /** The name of the column to fix. */
        private String columnName;

        /** The name of the column holding an identifier which can be printed in the logs when a migration fails. */
        private String idColumnName;

        /** What kind of data is corrupt when a migration fails. */
        private String dataType;

        /** Logging helper object. */
        private Logger logger = LoggerFactory.getLogger(R35102Work.class);

        /**
         * Constructor specifying the table and columns to fix.
         *
         * @param tableName the name of the table to fix
         * @param columnName the name of the column to fix
         * @param idColumnName the name of the column containing an identifier that should be printed in the logs when a
         *        migration fails
         * @param dataType the data type printed in the logs when a migration fails
         */
        public R35102Work(String tableName, String columnName, String idColumnName, String dataType)
        {
            this.tableName = tableName;
            this.columnName = columnName;
            this.idColumnName = idColumnName;
            this.dataType = dataType;
        }

        @Override
        public void execute(Connection connection) throws SQLException
        {
            Statement stmt = connection.createStatement();
            ResultSet lobs = stmt.executeQuery("SELECT " + this.columnName + ", " + this.idColumnName + " FROM "
                + this.tableName + ";");
            Map<String, Long> lobsToProcess = new HashMap<String, Long>();
            while (lobs.next()) {
                // If we're not migrating data created by a version between 3.2 and 3.5, then the data is already OK
                if (StringUtils.isNumeric(lobs.getString(1))) {
                    lobsToProcess.put(lobs.getString(1), lobs.getLong(2));
                }
            }
            PreparedStatement inlineLob = connection.prepareStatement(MessageFormat.format("UPDATE {0} SET {1} ="
                + " convert_from(loread(lo_open({1}::int, 262144), 10000000), ''LATIN1'') WHERE {1} = ?",
                this.tableName, this.columnName));
            PreparedStatement emptyLob = connection.prepareStatement(MessageFormat.format("UPDATE {0} SET {1} = ''''"
                + " WHERE {1} = ?", this.tableName, this.columnName));
            PreparedStatement removeLob = connection.prepareStatement("select lo_unlink(?)");
            for (Entry<String, Long> lob : lobsToProcess.entrySet()) {
                try {
                    inlineLob.setString(1, lob.getKey());
                    inlineLob.executeUpdate();
                    // We commit early since any error will invalidate the whole transaction
                    removeLob.setLong(1, Long.valueOf(lob.getKey()));
                    removeLob.execute();
                    connection.commit();
                } catch (SQLException ex) {
                    if (ex.getMessage().contains("0x00")) {
                        // The hibernate mapping file was broken between 3.2 and 3.5 for PostgreSQL, and any non-ASCII
                        // characters written to the database got broken since for each character, only the last 8 bits
                        // of the character's unicode value was sent to the database. There's no way of getting back the
                        // missing bytes, we can just empty the value set in this row.
                        // Start a new transaction
                        connection.rollback();
                        this.logger.warn(this.dataType + " [{}] cannot be recovered",
                            lob.getValue());
                        emptyLob.setString(1, lob.getKey());
                        emptyLob.executeUpdate();
                        removeLob.setLong(1, Long.valueOf(lob.getKey()));
                        removeLob.execute();
                        connection.commit();
                    } else {
                        throw ex;
                    }
                }
            }
        }
    }
}
