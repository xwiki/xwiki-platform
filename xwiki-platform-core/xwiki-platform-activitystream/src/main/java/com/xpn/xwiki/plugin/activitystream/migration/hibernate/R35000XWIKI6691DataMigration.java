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

package com.xpn.xwiki.plugin.activitystream.migration.hibernate;

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
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-6691: Change the mapping of the event requestId field to varchar(48). Since the even group ID is
 * always a GUID of length at most 36, it doesn't make sense to allocate 2000 characters for it. Since Hibernate cannot
 * alter existing columns to change their types, and we don't want to lose all the existing data in case of a migration,
 * we create a new column with the right type, and manually copy data between columns. After that we try to drop the old
 * column to free up some space.
 * 
 * @version $Id$
 * @since 3.5M1
 */
@Component
@Named("R35000XWIKI6691")
@Singleton
public class R35000XWIKI6691DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "See http://jira.xwiki.org/browse/XWIKI-6691";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(35000);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), true, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.doWork(new Work()
                {
                    @Override
                    public void execute(Connection connection) throws SQLException
                    {
                        Statement stmt = connection.createStatement();
                        stmt.addBatch("UPDATE activitystream_events SET ase_groupid = ase_requestid");
                        stmt.addBatch("ALTER TABLE activitystream_events DROP COLUMN ase_requestid");
                        stmt.executeBatch();
                    }
                });
                return Boolean.TRUE;
            }
        });
    }
}
