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
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-12228: Provide API and probably storage for optimized space related queries.
 * <p>
 * Make sure xwikidocument and xwikispace tables are in sync.
 *
 * @version $Id$
 * @since 7.2M2
 */
@Component
@Named("R72001XWIKI12228")
@Singleton
public class R72001XWIKI12228DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> resolver;

    @Override
    public String getDescription()
    {
        return "Make sure xwikidocument and xwikispace tables are in sync";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(72001);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.doWork(new R72001Work());
                return Boolean.TRUE;
            }
        });
    }

    private class R72001Work implements Work
    {
        @Override
        public void execute(Connection connection) throws SQLException
        {
            // Copy hidden spaces
            copySpaces(true, connection);

            // Copy visible spaces
            copySpaces(true, connection);
        }

        private void copySpaces(boolean hidden, Connection connection) throws SQLException
        {
            // Get spaces
            List<EntityReference> spaces = new ArrayList<>();
            try (Statement selectStatement = connection.createStatement()) {
                try (ResultSet result =
                    selectStatement.executeQuery("select DISTINCT XWD_WEB from xwikidoc"
                        + " where XWD_WEB like '%.%' OR XWD_WEB like '%\\\\%' OR XWD_WEB like '%:%'")) {
                    convert(connection, result);
                }
            }

            // Create spaces in the xwikispace table
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
        String spaceReference = serializer.serialize(new EntityReference(spaceName, EntityType.SPACE));

        statement.setString(1, spaceReference);
        statement.setString(2, spaceName);

        // Add a conversion to the list
        statement.addBatch();
    }
}
