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

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-7726: Unable to delete attachments larger than 10 mb using the jetty + hsql distribution. Early
 * versions of the HSQLDialect ignored the specified minimum length of LOB columns, creating them with the default
 * length of 16M. Thus, the precision of existing CLOB and BLOB columns must be manually extended to the required 1G.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("R42000XWIKI7726")
@Singleton
public class R42000XWIKI7726DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "Increase the size of the standard BLOB and CLOB columns which were created with the default 16M size.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(42000);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {

        try {
            return getStore().getDatabaseProductName() == DatabaseProduct.HSQLDB;
        } catch (DataMigrationException ex) {
            return false;
        }
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.doWork(new R42000Work());
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Hibernate {@link Work} class doing the actual work of this migrator.
     *
     * @version $Id$
     */
    private static final class R42000Work implements Work
    {
        @Override
        public void execute(Connection connection) throws SQLException
        {
            processColumn("XWIKIRCS", "XWR_PATCH", connection);
            processColumn("XWIKIRECYCLEBIN", "XDD_XML", connection);
            processColumn("XWIKIATTRECYCLEBIN", "XDA_XML", connection);
            processColumn("XWIKIATTACHMENT_CONTENT", "XWA_CONTENT", connection);
            processColumn("XWIKIATTACHMENT_ARCHIVE", "XWA_ARCHIVE", connection);
        }

        /**
         * Increase the size of one column.
         *
         * @param tableName the name of the table to process
         * @param columnName the name of the column to process
         * @param connection the database connection to use
         * @throws SQLException in case anything goes wrong
         */
        private void processColumn(String tableName, String columnName, Connection connection)
            throws SQLException
        {
            String command = "ALTER TABLE %s ALTER COLUMN %s SET DATA TYPE %s(%d)";
            PreparedStatement getCurrentColumnType = connection.prepareStatement(
                "select DATA_TYPE from INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME=?");
            getCurrentColumnType.setString(1, tableName);
            getCurrentColumnType.setString(2, columnName);
            ResultSet result = getCurrentColumnType.executeQuery();
            if (!result.next()) {
                return;
            }
            String currentColumnType = result.getString(1);
            result.close();
            getCurrentColumnType.close();
            connection.createStatement().execute(
                String.format(command, tableName, columnName, currentColumnType, 1 << 30));
        }
    }
}
