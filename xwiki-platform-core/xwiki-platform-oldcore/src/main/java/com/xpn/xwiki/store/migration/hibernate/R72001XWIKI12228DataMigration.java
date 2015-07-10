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
import java.util.Collections;
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
            createSpaces(true, connection);

            // Copy visible spaces
            createSpaces(true, connection);
        }

        private List<EntityReference> getSpaces(boolean hidden, Connection connection) throws SQLException
        {
            List<EntityReference> spaces = Collections.emptyList();

            try (Statement selectStatement = connection.createStatement()) {
                StringBuilder query = new StringBuilder("select DISTINCT XWD_WEB from xwikidoc where");
                if (hidden) {
                    query.append("doc.hidden = true");
                } else {
                    query.append("doc.hidden <> false OR doc.hidden IS NULL");
                }

                try (ResultSet result = selectStatement.executeQuery(query.toString())) {
                    spaces = new ArrayList<>();

                    do {
                        spaces.add(resolver.resolve(result.getString(1), EntityType.SPACE));
                    } while (result.next());
                }
            }

            return spaces;
        }

        private void createSpaces(boolean hidden, Connection connection) throws SQLException
        {
            // Get spaces
            List<EntityReference> spaces = getSpaces(hidden, connection);

            // Create spaces in the xwikispace table
            try (PreparedStatement statement =
                connection.prepareStatement("INSERT INTO xwikispace"
                    + " (XWS_HIDDEN, XWS_REFERENCE, XWS_NAME, XWS_PARENT) VALUES (?, ?, ?, ?)")) {
                for (EntityReference space : spaces) {
                    addBatch(statement, space, hidden);
                }

                // Do all the changes
                statement.executeBatch();
            }
        }
    }

    private void addBatch(PreparedStatement statement, EntityReference space, boolean hidden) throws SQLException
    {
        // hidden
        statement.setBoolean(1, hidden);

        // reference
        statement.setString(2, this.serializer.serialize(space));

        // name
        statement.setString(3, space.getName());

        // parent
        statement.setString(4, this.serializer.serialize(space.getParent()));

        // Add a space to the list
        statement.addBatch();
    }
}
