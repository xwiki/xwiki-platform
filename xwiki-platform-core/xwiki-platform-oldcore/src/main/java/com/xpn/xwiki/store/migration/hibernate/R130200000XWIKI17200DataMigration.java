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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.stats.impl.RefererStats;
import com.xpn.xwiki.stats.impl.VisitStats;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * This migration aims at applying the fix done on https://jira.xwiki.org/browse/XWIKI-15215 (change the type of some
 * column so that they fit in the maximum allowed maximum row size for the used table type) instances older than
 * 11.3RC1. Without this it's difficult to migrate to utf8mb3. It's also a requirement for
 * {@link AbstractResizeMigration}.
 * <p>
 * The columns are:
 * <ul>
 * <li>LegacyEvent/activitystream_events
 * <ul>
 * <li>url</li>
 * <li>title</li>
 * <li>body</li>
 * <li>param1</li>
 * <li>param2</li>
 * <li>param3</li>
 * <li>param4</li>
 * <li>param5</li>
 * </ul>
 * </li>
 * <li>RefererStats/xwikistatsreferer
 * <ul>
 * <li>referer</li>
 * </ul>
 * </li>
 * <li>VisitStats/xwikistatsvisit
 * <ul>
 * <li>userAgent</li>
 * <li>cookie</li>
 * </ul>
 * </li>
 * <li>XWikiPreferences/xwikipreferences
 * <ul>
 * <li>leftPanels</li>
 * <li>rightPanels</li>
 * <li>documentBundles</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @version $Id$
 * @since 13.2RC1
 * @since 12.10.6
 */
@Component
@Named("R130200000XWIKI17200")
@Singleton
public class R130200000XWIKI17200DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private Logger logger;

    @Inject
    private HibernateStore hibernateStore;

    @Override
    public String getDescription()
    {
        return "Make sure the database follow the currently expected type for some large string columns.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(130200000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Nothing to do here, everything's done as part of Liquibase pre update
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // Only really required for MySQL and MariaDB since https://jira.xwiki.org/browse/XWIKI-15215 was only
        // affecting MySQL and MariaDB
        DatabaseProduct databaseProductName = this.hibernateStore.getDatabaseProductName();
        return databaseProductName == DatabaseProduct.MYSQL || databaseProductName == DatabaseProduct.MARIADB;
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        StringBuilder builder = new StringBuilder();

        Map<String, List<String>> map = new HashMap<>();
        map.put("org.xwiki.eventstream.store.internal.LegacyEvent",
            Arrays.asList("url", "title", "body", "param1", "param2", "param3", "param4", "param5"));
        map.put(RefererStats.class.getName(), Arrays.asList("referer"));
        map.put(VisitStats.class.getName(), Arrays.asList("userAgent", "cookie"));
        map.put("XWiki.XWikiPreferences", Arrays.asList("leftPanels", "rightPanels", "documentBundles"));

        try (SessionImplementor session = (SessionImplementor) this.hibernateStore.getSessionFactory().openSession()) {
            JdbcConnectionAccess jdbcConnectionAccess = session.getJdbcConnectionAccess();

            try (Connection connection = jdbcConnectionAccess.obtainConnection()) {
                DatabaseMetaData databaseMetaData = connection.getMetaData();

                // Get rid of XWV_COOKIE key leftover if it exist
                deleteIndexIfExist(VisitStats.class, "xwv_cookie", databaseMetaData, builder);

                // Update properties
                for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                    for (String property : entry.getValue()) {
                        maybeUpdateField(databaseMetaData, entry.getKey(), property, builder);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataMigrationException("Error while extracting metadata", e);
        }

        if (builder.length() > 0) {
            return String.format("<changeSet author=\"xwiki\" id=\"R%s\">%s</changeSet>", getVersion().getVersion(),
                builder.toString());
        }

        return null;
    }

    private void deleteIndexIfExist(Class entityClass, String indexName, DatabaseMetaData databaseMetaData,
        StringBuilder builder) throws SQLException
    {
        String databaseName = this.hibernateStore.getDatabaseFromWikiName();
        PersistentClass persistentClass =
            this.hibernateStore.getConfigurationMetadata().getEntityBinding(entityClass.getName());
        String tableName = this.hibernateStore.getConfiguredTableName(persistentClass);

        ResultSet resultSet;
        if (this.hibernateStore.isCatalog()) {
            resultSet = databaseMetaData.getIndexInfo(databaseName, null, tableName, false, false);
        } else {
            resultSet = databaseMetaData.getIndexInfo(null, databaseName, tableName, false, false);
        }

        while (resultSet.next()) {
            String databaseIndexName = resultSet.getString("INDEX_NAME");
            if (indexName.equalsIgnoreCase(databaseIndexName)) {
                // Delete the index
                builder.append("<dropIndex indexName=\"");
                builder.append(databaseIndexName);
                builder.append("\"  tableName=\"");
                builder.append(tableName);
                builder.append("\"/>\n");

                break;
            }
        }
    }

    private void maybeUpdateField(DatabaseMetaData databaseMetaData, String entity, String propertyName,
        StringBuilder update) throws SQLException
    {
        Metadata configurationMetadata = this.hibernateStore.getConfigurationMetadata();

        PersistentClass persistentClass = configurationMetadata.getEntityBinding(entity);

        Property property = persistentClass.getProperty(propertyName);

        String tableName = this.hibernateStore.getConfiguredTableName(persistentClass);
        String columnName = this.hibernateStore.getConfiguredColumnName(persistentClass, propertyName);

        try (ResultSet resultSet = getColumns(databaseMetaData, tableName, columnName)) {
            if (resultSet.next()) {
                String currentColumnType = resultSet.getString("TYPE_NAME");

                Column column = (Column) property.getColumnIterator().next();
                String expectedColumnType =
                    this.hibernateStore.getDialect().getTypeName(column.getSqlTypeCode(configurationMetadata));

                if (!currentColumnType.equalsIgnoreCase(expectedColumnType)) {
                    int expectedLenght = column.getLength();

                    int currentColumnSize = resultSet.getInt("COLUMN_SIZE");

                    if (currentColumnSize > expectedLenght) {
                        column.setLength(currentColumnSize);
                    }

                    String dataType = getDataType(column, configurationMetadata);

                    // Not using <modifyDataType> here because Liquibase ignore attributes likes "NOT NULL"
                    update.append("<sql>");
                    update.append(this.hibernateStore.getDialect().getAlterTableString(tableName));
                    update.append(" MODIFY ");
                    update.append(column.getQuotedName(this.hibernateStore.getDialect()));
                    update.append(' ');
                    update.append(dataType);
                    update.append("</sql>");

                    this.logger.info("Updating the type of [{}.{}] to [{}]", tableName, columnName, dataType);
                }
            }
        }
    }

    private String getDataType(Column column, Metadata configurationMetadata)
    {
        Dialect dialect = this.hibernateStore.getDialect();

        StringBuilder builder = new StringBuilder(column.getSqlType(dialect, configurationMetadata));

        if (column.isNullable()) {
            builder.append(dialect.getNullColumnString());
        } else {
            builder.append(" not null");
        }

        return builder.toString();
    }

    private ResultSet getColumns(DatabaseMetaData databaseMetaData, String tableName, String columnName)
        throws SQLException
    {
        if (this.hibernateStore.isCatalog()) {
            return databaseMetaData.getColumns(this.hibernateStore.getDatabaseFromWikiName(), null, tableName,
                columnName);
        } else {
            return databaseMetaData.getColumns(null, this.hibernateStore.getDatabaseFromWikiName(), tableName,
                columnName);
        }
    }
}
