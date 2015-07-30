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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiSpace;
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
    /**
     * We don't really care what is the wiki since it's only used to go trough XWikiSpace utility methods.
     */
    private static final WikiReference WIKI = new WikiReference("wiki");

    @Inject
    private SpaceReferenceResolver<String> spaceResolver;

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
            // Copy visible spaces
            Collection<SpaceReference> visibleSpaces = createVisibleSpaces(connection);

            // Copy hidden spaces
            createHiddenSpaces(visibleSpaces, connection);
        }

        private String createSpaceQuery(boolean hidden)
        {
            StringBuilder query = new StringBuilder("select DISTINCT XWD_WEB from xwikidoc where");
            if (hidden) {
                query.append(" XWD_WEB not in (" + createSpaceQuery(false) + ")");
            } else {
                query.append(" XWD_HIDDEN <> true OR XWD_HIDDEN IS NULL");
            }

            return query.toString();
        }

        private Collection<SpaceReference> getVisibleSpaces(Connection connection) throws SQLException
        {
            Collection<SpaceReference> databaseSpaces;

            try (Statement selectStatement = connection.createStatement()) {
                try (ResultSet result = selectStatement.executeQuery(createSpaceQuery(false))) {
                    databaseSpaces = new ArrayList<>();

                    while (result.next()) {
                        databaseSpaces.add(spaceResolver.resolve(result.getString(1), WIKI));
                    }
                }
            }

            // Resolve nested spaces
            Set<SpaceReference> spaces = new HashSet<>(databaseSpaces);
            for (SpaceReference space : databaseSpaces) {
                for (EntityReference parent = space.getParent(); parent instanceof SpaceReference; parent =
                    parent.getParent()) {
                    spaces.add((SpaceReference) parent);
                }
            }

            return spaces;
        }

        private Collection<SpaceReference> getHiddenSpaces(Collection<SpaceReference> visibleSpaces,
            Connection connection) throws SQLException
        {
            Collection<SpaceReference> databaseSpaces;

            try (Statement selectStatement = connection.createStatement()) {
                try (ResultSet result = selectStatement.executeQuery(createSpaceQuery(true))) {
                    databaseSpaces = new ArrayList<>();

                    while (result.next()) {
                        databaseSpaces.add(spaceResolver.resolve(result.getString(1), WIKI));
                    }
                }
            }

            // Resolve nested spaces
            Set<SpaceReference> spaces = new HashSet<>(databaseSpaces);
            for (SpaceReference space : databaseSpaces) {
                for (EntityReference parent = space.getParent(); parent instanceof SpaceReference; parent =
                    parent.getParent()) {
                    if (!visibleSpaces.contains(parent)) {
                        spaces.add((SpaceReference) parent);
                    }
                }
            }

            return spaces;
        }

        private Collection<SpaceReference> createVisibleSpaces(Connection connection) throws SQLException
        {
            // Get spaces
            Collection<SpaceReference> spaces = getVisibleSpaces(connection);

            // Create spaces
            createSpaces(spaces, false, connection);

            return spaces;
        }

        private void createHiddenSpaces(Collection<SpaceReference> visibleSpaces, Connection connection)
            throws SQLException
        {
            // Get spaces
            Collection<SpaceReference> spaces = getHiddenSpaces(visibleSpaces, connection);

            // Create spaces
            createSpaces(spaces, true, connection);
        }

        private void createSpaces(Collection<SpaceReference> spaces, boolean hidden, Connection connection)
            throws SQLException
        {
            // Create spaces in the xwikispace table
            try (PreparedStatement statement =
                connection.prepareStatement("INSERT INTO xwikispace"
                    + " (XWS_ID, XWS_HIDDEN, XWS_REFERENCE, XWS_NAME, XWS_PARENT) VALUES (?, ?, ?, ?, ?)")) {
                for (SpaceReference space : spaces) {
                    addBatch(statement, new XWikiSpace(space, hidden));
                }

                // Do all the changes
                statement.executeBatch();
            }
        }
    }

    private void addBatch(PreparedStatement statement, XWikiSpace space) throws SQLException
    {
        // id
        statement.setLong(1, space.getId());

        // hidden
        statement.setBoolean(2, space.isHidden());

        // reference
        statement.setString(3, space.getReference());

        // name
        statement.setString(4, space.getName());

        // parent
        statement.setString(5, space.getParent());

        // Add a space to the list
        statement.addBatch();
    }
}
