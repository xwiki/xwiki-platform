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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.store.hibernate.HibernateAdapter;
import org.xwiki.store.hibernate.HibernateStoreException;
import org.xwiki.store.hibernate.internal.MySQLHibernateAdapter;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Extended by migrations which need to resize columns to the maximum index supported by MySQL: 768.
 *
 * @version $Id$
 * @since 13.4.7
 * @since 13.10.3
 * @since 14.0RC1
 */
public abstract class AbstractResizeMigration extends AbstractHibernateDataMigration
{
    private static final int MAXSIZE_MIN = 720;

    private static final int MAXSIZE_MAX = 768;

    private static final Version MYSQL57 = new DefaultVersion("5.7");

    private static final Version MARIADB102 = new DefaultVersion("10.2");

    @Inject
    private Logger logger;

    @Inject
    private HibernateStore hibernateStore;

    @Override
    public String getDescription()
    {
        return "Increase the maximum size of all the columns potentially containing a document reference"
            + " to the maximum index supported by MySQL.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(130200001);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Nothing to do here, everything's done as part of Liquibase pre update
    }

    private void warnDatabaseTooOld(String databaseName, Version databaseVersion)
    {
        this.logger.warn(
            "The migration cannot run on {} versions lower than {}. The short String limitation will remain 255.",
            databaseName, databaseVersion);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // Check the version of the database server
        if (this.hibernateStore.getDatabaseProductName() == DatabaseProduct.MYSQL) {
            DatabaseMetaData databaMetadata = this.hibernateStore.getDatabaseMetaData();

            try {
                String productName = databaMetadata.getDatabaseProductName();

                String versionString = databaMetadata.getDatabaseProductVersion();
                Version version = new DefaultVersion(versionString);

                if (productName.equalsIgnoreCase("mariadb")) {
                    // Impossible to apply this migration on MariaDB lower than 10.2
                    if (version.compareTo(MARIADB102) < 0) {
                        warnDatabaseTooOld("MariaDB", MARIADB102);

                        return false;
                    }
                } else {
                    // Impossible to apply this migration on MySQL lower than 5.7
                    if (version.compareTo(MYSQL57) < 0) {
                        warnDatabaseTooOld("MySQL", MYSQL57);

                        return false;
                    }
                }
            } catch (SQLException e) {
                this.logger.warn("Failed to get database information: {}", ExceptionUtils.getRootCauseMessage(e));
            }
        } else if (this.hibernateStore.getDatabaseProductName() == DatabaseProduct.MSSQL) {
            // Impossible to apply this migration on Microsoft SQL Server
            this.logger.warn("The migration cannot run on Microsoft SQL Server");

            return false;
        }

        return true;
    }

    private void appendXmlAttribute(String property, String value, StringBuilder stringBuilder)
    {
        stringBuilder.append(' ');
        stringBuilder.append(property);
        stringBuilder.append("=\"");
        stringBuilder.append(value);
        stringBuilder.append("\" ");
    }

    private void updateColumn(Column column, DatabaseMetaData databaseMetaData, Set<String> dynamicTables,
        Map<String, String> rowFormats, StringBuilder builder, Session session)
        throws SQLException, HibernateStoreException
    {
        int expectedLenght = column.getLength();

        if (expectedLenght >= MAXSIZE_MIN && expectedLenght <= MAXSIZE_MAX) {
            Integer databaseSize = getDatabaseSize(column, databaseMetaData);

            // Skip the update if the column does not exist of if its size if greater or equals already
            if (databaseSize != null && databaseSize.intValue() < expectedLenght) {
                update(column, dynamicTables, rowFormats, builder, session);
            }
        }
    }

    private Integer getDatabaseSize(Column column, DatabaseMetaData databaseMetaData) throws SQLException
    {
        String databaseName = this.hibernateStore.getAdapter().getDatabaseFromWikiName();
        String tableName = this.hibernateStore.getAdapter().getTableName(column.getValue().getTable());
        String columnName = this.hibernateStore.getConfiguredColumnName(column);

        ResultSet resultSet;
        if (this.hibernateStore.getAdapter().isCatalog()) {
            resultSet = databaseMetaData.getColumns(databaseName, null, tableName, columnName);
        } else {
            resultSet = databaseMetaData.getColumns(null, databaseName, tableName, columnName);
        }

        if (resultSet.next()) {
            return resultSet.getInt("COLUMN_SIZE");
        }

        return null;
    }

    private void updateProperty(Property property, DatabaseMetaData databaseMetaData, Set<String> dynamicTables,
        Map<String, String> rowFormats, StringBuilder builder, Session session)
        throws SQLException, HibernateStoreException
    {
        if (property != null) {
            updateValue(property.getValue(), databaseMetaData, dynamicTables, rowFormats, builder, session);
        }
    }

    private void updateValue(Value value, DatabaseMetaData databaseMetaData, Set<String> dynamicTables,
        Map<String, String> rowFormats, StringBuilder builder, Session session)
        throws SQLException, HibernateStoreException
    {
        if (value instanceof Collection collection) {
            Table collectionTable = collection.getCollectionTable();

            for (Iterator<Column> it = collectionTable.getColumnIterator(); it.hasNext();) {
                updateColumn(it.next(), databaseMetaData, dynamicTables, rowFormats, builder, session);
            }
        } else if (value != null) {
            for (Iterator<Selectable> it = value.getColumnIterator(); it.hasNext();) {
                Selectable selectable = it.next();
                if (selectable instanceof Column column) {
                    updateColumn(column, databaseMetaData, dynamicTables, rowFormats, builder, session);
                }
            }
        }
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        StringBuilder builder = new StringBuilder();

        try (SessionImplementor session = (SessionImplementor) this.hibernateStore.getSessionFactory().openSession()) {
            JdbcConnectionAccess jdbcConnectionAccess = session.getJdbcConnectionAccess();

            try (Connection connection = jdbcConnectionAccess.obtainConnection()) {
                DatabaseMetaData databaseMetaData = connection.getMetaData();

                java.util.Collection<PersistentClass> bindings =
                    this.hibernateStore.getConfigurationMetadata().getEntityBindings();

                Set<String> updatedTables = new HashSet<>();

                // Gather existing tables
                List<PersistentClass> existingTables = new ArrayList<>(bindings.size());
                for (PersistentClass entity : this.hibernateStore.getConfigurationMetadata().getEntityBindings()) {
                    if (exists(entity, databaseMetaData)) {
                        existingTables.add(entity);
                    }
                }

                // Cleanup specific to MySQL/MariaDB
                Map<String, String> rowFormats = null;
                MySQLHibernateAdapter adapter = getMySQLAdapter();
                if (adapter != null) {
                    // Make sure all MySQL/MariaDB tables use the expected row format (required to support key prefix
                    // length limit up to 3072 bytes)
                    rowFormats = adapter.getRowFormats(session);
                    for (PersistentClass entity : existingTables) {
                        setTableRowFormat(entity, rowFormats, builder, updatedTables, session);
                    }

                    // Remove combined UNIQUE KEY affecting xwikiattrecyclebin#XDA_FILENAME column since those are not
                    // supposed to exist anymore and it can prevent the resize on MySQL/MariaDB
                    removeAttachmentRecycleFilenameMultiKey(builder, session);
                    removeRecycleFilenameMultiKey(builder, session);
                }

                // Update columns for which the size changed
                updateColumns(existingTables, databaseMetaData, builder, updatedTables, rowFormats, session);
            }
        } catch (Exception e) {
            throw new DataMigrationException("Error while extracting metadata", e);
        }

        if (builder.length() > 0) {
            String script = String.format("<changeSet author=\"xwiki\" id=\"R%s\">%s</changeSet>",
                getVersion().getVersion(), builder.toString());

            this.logger.debug("Liquibase script: {}", script);

            return script;
        }

        return null;
    }

    private void removeAttachmentRecycleFilenameMultiKey(StringBuilder builder, Session session)
    {
        PersistentClass persistentClass =
            this.hibernateStore.getConfigurationMetadata().getEntityBinding(DeletedAttachment.class.getName());
        String tableName = this.hibernateStore.getAdapter().getTableName(persistentClass);

        removeFilenameMultiKey(tableName, builder, session);
    }

    private void removeRecycleFilenameMultiKey(StringBuilder builder, Session session)
    {
        PersistentClass persistentClass =
            this.hibernateStore.getConfigurationMetadata().getEntityBinding(XWikiDeletedDocument.class.getName());
        String tableName = this.hibernateStore.getAdapter().getTableName(persistentClass);

        removeFilenameMultiKey(tableName, builder, session);
    }

    private void removeFilenameMultiKey(String tableName, StringBuilder builder, Session session)
    {
        String databaseName = this.hibernateStore.getAdapter().getDatabaseFromWikiName();

        for (String key : getUniqueKeys(databaseName, tableName, session)) {
            builder.append("<dropUniqueConstraint");
            appendXmlAttribute("constraintName", key, builder);
            appendXmlAttribute("tableName", tableName, builder);
            builder.append("/>");
        }
    }

    private List<String> getUniqueKeys(String databaseName, String tableName, Session session)
    {
        NativeQuery<String> query =
            session.createNativeQuery("SELECT DISTINCT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "WHERE TABLE_SCHEMA = :schema AND TABLE_NAME = :table AND CONSTRAINT_TYPE = 'UNIQUE'");
        query.setParameter("schema", databaseName);
        query.setParameter("table", tableName);

        return query.list();
    }

    private void updateColumns(List<PersistentClass> existingTables, DatabaseMetaData databaseMetaData,
        StringBuilder builder, Set<String> dynamicTables, Map<String, String> rowFormats, Session session)
        throws SQLException, HibernateStoreException
    {
        for (PersistentClass entity : existingTables) {
            // Find properties to update
            for (Iterator<Property> it = entity.getPropertyIterator(); it.hasNext();) {
                updateProperty(it.next(), databaseMetaData, dynamicTables, rowFormats, builder, session);
            }

            // Check the key
            updateValue(entity.getKey(), databaseMetaData, dynamicTables, rowFormats, builder, session);
        }
    }

    private MySQLHibernateAdapter getMySQLAdapter()
    {
        HibernateAdapter adapter = this.hibernateStore.getAdapter();

        return adapter instanceof MySQLHibernateAdapter mysqlApater ? mysqlApater : null;
    }

    private void setTableRowFormat(PersistentClass entity, Map<String, String> rowFormats, StringBuilder builder,
        Set<String> updatedTables, Session session) throws HibernateStoreException
    {
        String tableName = getMySQLAdapter().getTableName(entity);
        boolean compressed = entity.getMetaAttribute(HibernateAdapter.META_ATTRIBUTE_COMPRESSED) != null;

        setTableRowFormat(tableName, compressed, rowFormats, builder, updatedTables, session);
    }

    private void setTableRowFormat(String tableName, boolean compressed, Map<String, String> rowFormats,
        StringBuilder builder, Set<String> updatedTables, Session session) throws HibernateStoreException
    {
        MySQLHibernateAdapter adapater = (MySQLHibernateAdapter) this.hibernateStore.getAdapter();

        String statement = adapater.getAlterRowFormatString(tableName, compressed, rowFormats, session);

        if (statement != null) {
            this.logger.debug("Converting raw format for table [{}]", tableName);

            builder.append("<sql>");
            builder.append(statement);
            builder.append("</sql>");

            updatedTables.add(tableName);
        }
    }

    private boolean exists(PersistentClass entity, DatabaseMetaData databaseMetaData) throws SQLException
    {
        String databaseName = this.hibernateStore.getAdapter().getDatabaseFromWikiName();
        String tableName = this.hibernateStore.getAdapter().getTableName(entity);

        ResultSet resultSet;
        if (this.hibernateStore.getAdapter().isCatalog()) {
            resultSet = databaseMetaData.getTables(databaseName, null, tableName, null);
        } else {
            resultSet = databaseMetaData.getTables(null, databaseName, tableName, null);
        }

        return resultSet.next();
    }

    private void update(Column column, Set<String> dynamicTables, Map<String, String> rowFormats, StringBuilder builder,
        Session session) throws HibernateStoreException
    {
        MySQLHibernateAdapter mysqlAdapater = getMySQLAdapter();

        if (mysqlAdapater != null) {
            JdbcEnvironment jdbcEnvironment =
                this.hibernateStore.getConfigurationMetadata().getDatabase().getJdbcEnvironment();
            String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter()
                .format(column.getValue().getTable().getQualifiedTableName(), this.hibernateStore.getDialect());

            String quotedColumn = column.getQuotedName(this.hibernateStore.getDialect());

            this.logger.debug("Updating column [{}] in table [{}]", quotedColumn, tableName);

            // Make sure all MySQL/MariaDB tables use the expected row format (required to support key prefix
            // length limit up to 3072 bytes)
            // Check again in case it's a special table which was missing in the previous pass
            setTableRowFormat(tableName, false, rowFormats, builder, dynamicTables, session);

            // Not using <modifyDataType> here because Liquibase ignores attributes likes "NOT NULL"
            builder.append("<sql>");
            builder.append(this.hibernateStore.getDialect().getAlterTableString(tableName));
            builder.append(" MODIFY ");
            builder.append(quotedColumn);
            builder.append(' ');
            builder.append(getDataType(column, this.hibernateStore.getConfigurationMetadata()));
            builder.append("</sql>");
        } else {
            builder.append("<modifyDataType");
            appendXmlAttribute("tableName", this.hibernateStore.getAdapter().getTableName(column.getValue().getTable()),
                builder);
            appendXmlAttribute("columnName", this.hibernateStore.getConfiguredColumnName(column), builder);
            appendXmlAttribute("newDataType",
                column.getSqlType(this.hibernateStore.getDialect(), this.hibernateStore.getConfigurationMetadata()),
                builder);
            builder.append("/>");
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
}
