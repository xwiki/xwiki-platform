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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
 * Migration for XWIKI-7564: Manually change the SQL type of long binary columns from inline bytea to proper LOBs when
 * the underlying database is PostgreSQL.
 *
 * @version $Id$
 * @since 3.5.1
 */
@Component
@Named("R35100XWIKI7564")
@Singleton
public class R35100XWIKI7564DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "See https://jira.xwiki.org/browse/XWIKI-7564";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(35100);
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
                session.doWork(new R35100Work());
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Hibernate {@link Work} class that reads an SQL script file and executes them.
     *
     * @version $Id$
     */
    private static final class R35100Work implements Work
    {
        @Override
        public void execute(Connection connection) throws SQLException
        {
            try (Statement stmt = connection.createStatement()) {
                try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(this.getClass().getResourceAsStream("R35100XWIKI7564.sql"),
                        StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        stmt.addBatch(line);
                    }
                }

                stmt.executeBatch();
            } catch (BatchUpdateException ex) {
                if (ex.getNextException() != null
                    && ex.getNextException().getMessage().contains("function lowrite(integer, oid)")) {
                    // This exception is thrown when this migrator isn't really needed. This happens when migrating from
                    // a version between 3.2 and 3.5, which do use the proper table structure, but we can't easily
                    // distinguish between a pre-3.2 database and a post-3.2 database.
                    return;
                }
                throw ex;
            } catch (UnsupportedEncodingException ex) {
                // Should never happen, UTF-8 is always available
            } catch (IOException ex) {
                // Shouldn't happen, the script is supposed to be there
            }
        }
    }
}
