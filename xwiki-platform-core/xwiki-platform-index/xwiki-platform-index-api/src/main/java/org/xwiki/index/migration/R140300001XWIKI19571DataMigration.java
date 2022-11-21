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
package org.xwiki.index.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.xwiki.component.annotation.Component;
import org.xwiki.doc.tasks.XWikiDocumentIndexingTask;
import org.xwiki.index.internal.TasksStore;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migrate XWikiDocumentIndexingTask by saving the content of the table in memory. Then, after hibernate migration, when
 * XWikiDocumentIndexingTask is created again, the rows are copied from memory to the new table.
 *
 * @version $Id$
 * @since 14.3RC1
 * @deprecated link storage and indexing moved to Solr (implemented in xwiki-platform-search-solr-api)
 */
@Component
@Singleton
@Named(R140300001XWIKI19571DataMigration.HINT)
@Deprecated(since = "14.8RC1")
public class R140300001XWIKI19571DataMigration extends AbstractHibernateDataMigration
{
    /**
     * The hint for this component.
     */
    public static final String HINT = "R140300001XWIKI19571";

    @Inject
    private HibernateStore hibernateStore;

    @Inject
    private Provider<TasksStore> tasksStore;

    private List<XWikiDocumentIndexingTask> tasks = new ArrayList<>();

    @Override
    public String getDescription()
    {
        return "Save the content of xwikidocumentindexingqueue, remove it, let Hibernate recreate it with a new schema"
            + " and finally re-insert the saved content in xwikidocumentindexingqueue";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140300001);
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        SessionFactory sessionFactory = this.hibernateStore.getSessionFactory();
        try (SessionImplementor session = (SessionImplementor) sessionFactory.openSession()) {
            this.hibernateStore.setWiki(session);
            JdbcConnectionAccess jdbcConnectionAccess = session.getJdbcConnectionAccess();
            try (Connection connection = jdbcConnectionAccess.obtainConnection()) {
                DatabaseMetaData databaseMetaData = connection.getMetaData();

                PersistentClass persistentClass =
                    this.hibernateStore.getConfigurationMetadata()
                        .getEntityBinding(XWikiDocumentIndexingTask.class.getName());
                String tableName = this.hibernateStore.getConfiguredTableName(persistentClass);
                String changeSet = "";
                if (exists(databaseMetaData, tableName)) {
                    saveTasks(session, persistentClass, tableName);
                    changeSet = String.format("<changeSet author=\"xwikiorg\" id=\"%s0\">\n"
                        + "  <dropTable tableName=\"%s\"/>"
                        + "\n</changeSet>\n"
                        + "\n", HINT, tableName);
                }
                return changeSet;
            }
        } catch (SQLException e) {
            throw new DataMigrationException("Error while loading the existing tasks.", e);
        } catch (XWikiException e) {
            throw new DataMigrationException("Error while setting the current wiki in the hibernate session.", e);
        }
    }

    @Override
    protected void hibernateMigrate() throws XWikiException
    {
        for (XWikiDocumentIndexingTask task : this.tasks) {
            this.tasksStore.get().addTask(getXWikiContext().getWikiId(), task);
        }
        // Clear the tasks before the migration of the next wiki.
        this.tasks.clear();
    }

    /**
     * Check if the table exists for XWikiDocumentIndexingTask.
     *
     * @param databaseMetaData the database metadata
     * @param tableName the table name to search for
     * @return {@code true} if the table exists for {@link XWikiDocumentIndexingTask}, {@code false} otherwise
     * @throws SQLException in case of error when accessing the tables metadata
     */
    private boolean exists(DatabaseMetaData databaseMetaData, String tableName) throws SQLException
    {
        String databaseName = this.hibernateStore.getDatabaseFromWikiName();

        ResultSet resultSet;
        if (this.hibernateStore.isCatalog()) {
            resultSet = databaseMetaData.getTables(databaseName, null, tableName, null);
        } else {
            resultSet = databaseMetaData.getTables(null, databaseName, tableName, null);
        }

        return resultSet.next();
    }

    /**
     * Select all the rows for the table of {@link XWikiDocumentIndexingTask} and save them in memory.
     *
     * @param session the current session
     * @param entity the entity to select the rows from
     * @param tableName the table name of the entity
     * @throws SQLException in case of error when retrieving the rows
     */
    private void saveTasks(SessionImplementor session, PersistentClass entity, String tableName) throws SQLException
    {
        String docIdColumnName = this.hibernateStore.getConfiguredColumnName(entity, "docId");
        String versionColumnName = this.hibernateStore.getConfiguredColumnName(entity, "version");
        String typeColumnName = this.hibernateStore.getConfiguredColumnName(entity, "type");
        String instanceIdColumnName = this.hibernateStore.getConfiguredColumnName(entity, "instanceId");
        String timestampColumnName = this.hibernateStore.getConfiguredColumnName(entity, "timestamp");

        // We can't execute this query with Hibernate since the entity correspond to the new schema, which is 
        // updated later in the migration.
        String sql = String.format("select %s, %s, %s, %s, %s from %s", docIdColumnName, versionColumnName,
            typeColumnName, instanceIdColumnName, timestampColumnName, tableName);
        List<Object[]> resultList = session.createSQLQuery(sql).getResultList();
        for (Object[] resultSet : resultList) {
            XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
            task.setDocId(((Number) resultSet[0]).longValue());
            task.setVersion(cleanupVersion((String) resultSet[1]));
            task.setType((String) resultSet[2]);
            task.setInstanceId((String) resultSet[3]);
            task.setTimestamp((Timestamp) resultSet[4]);
            this.tasks.add(task);
        }
    }

    private String cleanupVersion(String version)
    {
        return Optional.ofNullable(version).orElse("").replace("-", "");
    }
}
