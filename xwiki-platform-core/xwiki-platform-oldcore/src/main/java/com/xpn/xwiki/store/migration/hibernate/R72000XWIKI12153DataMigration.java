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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-12153: Implement nested spaces support at database level.
 * <p>
 * Convert existing values in documents space field from single space name to complete space local reference.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("R72000XWIKI12153")
@Singleton
public class R72000XWIKI12153DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public String getDescription()
    {
        return "Convert document space name into space reference";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(72000);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.doWork(new R72000Work());
                return Boolean.TRUE;
            }
        });
    }

    private final class R72000Work implements Work
    {
        @Override
        public void execute(Connection connection) throws SQLException
        {
            // Search for all document spaces that should be escaped
            try (Statement selectStatement = connection.createStatement()) {
                try (ResultSet result =
                    selectStatement.executeQuery("select DISTINCT XWD_WEB from xwikidoc"
                        + " where XWD_WEB like '%.%' OR XWD_WEB like '%\\\\%' OR XWD_WEB like '%:%'")) {
                    convert(connection, result);
                }
            }
        }
    }

    private void convert(Connection connection, ResultSet result) throws SQLException
    {
        if (result.next()) {
            try (PreparedStatement statement =
                connection.prepareStatement("UPDATE xwikidoc set XWD_WEB = ? WHERE XWD_WEB = ?")) {
                do {
                    addBatch(statement, result.getString(1));
                } while (result.next());

                // Do all the changes
                statement.executeBatch();
            }
        }
    }

    private void addBatch(PreparedStatement statement, String spaceName) throws SQLException
    {
        // Convert the space name into a space reference
        String spaceReference = this.serializer.serialize(new EntityReference(spaceName, EntityType.SPACE));

        statement.setString(1, spaceReference);
        statement.setString(2, spaceName);

        // Add a conversion to the list
        statement.addBatch();
    }
}
