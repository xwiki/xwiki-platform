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
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.HibernateException;
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

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.DatabaseProduct;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
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

    private void warnDatabaTooOld(String databaseName, Version databaseVersion)
    {
        this.logger.warn(
            "The migration cannot run on {} versions lower than {}. The short String limitation will remain 255.",
            databaseName, databaseVersion);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // Check the version of the database server
        DatabaseProduct databaseProductName = this.hibernateStore.getDatabaseProductName();

        if (databaseProductName == DatabaseProduct.MYSQL || databaseProductName == DatabaseProduct.MARIADB) {
            DatabaseMetaData databaMetadata = this.hibernateStore.getDatabaseMetaData();

            try {
                String versionString = databaMetadata.getDatabaseProductVersion();
                Version version = new DefaultVersion(versionString);

                if (databaseProductName == DatabaseProduct.MARIADB) {
                    // Impossible to apply this migration on MariaDB lower than 10.2
                    if (version.compareTo(MARIADB102) < 0) {
                        warnDatabaTooOld("MariaDB", MARIADB102);

                        return false;
                    }
                } else {
                    // Impossible to apply this migration on MySQL lower than 5.7
                    if (version.compareTo(MYSQL57) < 0) {
                        warnDatabaTooOld("MySQL", MYSQL57);

                        return false;
                    }
                }
            } catch (SQLException e) {
                this.logger.warn("Failed to get database information: {}", ExceptionUtils.getRootCauseMessage(e));
            }
        } else if (databaseProductName == DatabaseProduct.MSSQL) {
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
        StringBuilder builder) throws SQLException, DataMigrationException
    {
        int expectedLenght = column.getLength();

        if (expectedLenght >= MAXSIZE_MIN && expectedLenght <= MAXSIZE_MAX) {
            Integer databaseSize = getDatabaseSize(column, databaseMetaData);

            // Skip the update if the column does not exist of if its size if greater or equals already
            if (databaseSize != null && databaseSize.intValue() < expectedLenght) {
                update(column, dynamicTables, builder);
            }
        }
    }

    private Integer getDatabaseSize(Column column, DatabaseMetaData databaseMetaData) throws SQLException
    {
        String databaseName = this.hibernateStore.getDatabaseFromWikiName();
        String tableName = this.hibernateStore.getConfiguredTableName(column.getValue().getTable());
        String columnName = this.hibernateStore.getConfiguredColumnName(column);

        ResultSet resultSet;
        if (this.hibernateStore.isCatalog()) {
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
        StringBuilder builder) throws SQLException, DataMigrationException
    {
        if (property != null) {
            updateValue(property.getValue(), databaseMetaData, dynamicTables, builder);
        }
    }

    private void updateValue(Value value, DatabaseMetaData databaseMetaData, Set<String> dynamicTables,
        StringBuilder builder) throws SQLException, DataMigrationException
    {
        if (value instanceof Collection) {
            Table collectionTable = ((Collection) value).getCollectionTable();

            for (Iterator<Column> it = collectionTable.getColumnIterator(); it.hasNext(); ) {
                updateColumn(it.next(), databaseMetaData, dynamicTables, builder);
            }
        } else if (value != null) {
            for (Iterator<Selectable> it = value.getColumnIterator(); it.hasNext(); ) {
                Selectable selectable = it.next();
                if (selectable instanceof Column) {
                    updateColumn((Column) selectable, databaseMetaData, dynamicTables, builder);
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

                Set<String> dynamicTables = new HashSet<>();

                // Gather existing tables
                List<PersistentClass> existingTables = new ArrayList<>(bindings.size());
                for (PersistentClass entity : this.hibernateStore.getConfigurationMetadata().getEntityBindings()) {
                    if (exists(entity, databaseMetaData)) {
                        existingTables.add(entity);
                    }
                }

                // Cleanup specific to MySQL/MariaDB
                DatabaseProduct databaseProductName = this.hibernateStore.getDatabaseProductName();
                if (databaseProductName == DatabaseProduct.MYSQL || databaseProductName == DatabaseProduct.MARIADB) {
                    // Make sure all MySQL/MariaDB tables use a DYNAMIC row format (required to support key prefix
                    // length limit up to 3072 bytes)
                    for (PersistentClass entity : existingTables) {
                        setTableDYNAMIC(entity, builder, dynamicTables);
                    }

                    // Remove combined UNIQUE KEY affecting xwikiattrecyclebin#XDA_FILENAME column since those are not
                    // supposed to exist anymore and it can prevent the resize on MySQL/MariaDB
                    removeAttachmentRecycleFilenameMultiKey(builder);
                    removeRecycleFilenameMultiKey(builder);
                }

                // Update columns for which the size changed
                updateColumns(existingTables, databaseMetaData, builder, dynamicTables);
            }
        } catch (SQLException e) {
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

    private void removeAttachmentRecycleFilenameMultiKey(StringBuilder builder) throws DataMigrationException
    {
        PersistentClass persistentClass =
            this.hibernateStore.getConfigurationMetadata().getEntityBinding(DeletedAttachment.class.getName());
        String tableName = this.hibernateStore.getConfiguredTableName(persistentClass);

        removeFilenameMultiKey(tableName, builder);
    }

    private void removeRecycleFilenameMultiKey(StringBuilder builder) throws DataMigrationException
    {
        PersistentClass persistentClass =
            this.hibernateStore.getConfigurationMetadata().getEntityBinding(XWikiDeletedDocument.class.getName());
        String tableName = this.hibernateStore.getConfiguredTableName(persistentClass);

        removeFilenameMultiKey(tableName, builder);
    }

    private void removeFilenameMultiKey(String tableName, StringBuilder builder) throws DataMigrationException
    {
        String databaseName = this.hibernateStore.getDatabaseFromWikiName();

        for (String key : getUniqueKeys(databaseName, tableName)) {
            builder.append("<dropUniqueConstraint");
            appendXmlAttribute("constraintName", key, builder);
            appendXmlAttribute("tableName", tableName, builder);
            builder.append("/>");
        }
    }

    private List<String> getUniqueKeys(String databaseName, String tableName) throws DataMigrationException
    {
        try {
            return getStore().executeRead(getXWikiContext(), new HibernateCallback<List<String>>()
            {
                @Override
                public List<String> doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    NativeQuery<String> query = session
                        .createNativeQuery("SELECT DISTINCT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                            + "WHERE TABLE_SCHEMA = :schema AND TABLE_NAME = :table AND CONSTRAINT_TYPE = 'UNIQUE'");
                    query.setParameter("schema", databaseName);
                    query.setParameter("table", tableName);

                    return query.list();
                }
            });
        } catch (XWikiException e) {
            throw new DataMigrationException("Failed to get unique keys", e);
        }
    }

    private void updateColumns(List<PersistentClass> existingTables, DatabaseMetaData databaseMetaData,
        StringBuilder builder, Set<String> dynamicTables) throws SQLException, DataMigrationException
    {
        for (PersistentClass entity : existingTables) {
            // Find properties to update
            for (Iterator<Property> it = entity.getPropertyIterator(); it.hasNext(); ) {
                updateProperty(it.next(), databaseMetaData, dynamicTables, builder);
            }

            // Check the key
            updateValue(entity.getKey(), databaseMetaData, dynamicTables, builder);
        }
    }

    private void setTableDYNAMIC(PersistentClass entity, StringBuilder builder, Set<String> dynamicTables)
        throws DataMigrationException
    {
        String tableName = this.hibernateStore.getConfiguredTableName(entity);

        setTableDYNAMIC(tableName, builder, dynamicTables);
    }

    private void setTableDYNAMIC(String tableName, StringBuilder builder, Set<String> dynamicTables)
        throws DataMigrationException
    {
        if (!dynamicTables.contains(tableName) && !StringUtils.equalsIgnoreCase(getRowFormat(tableName), "Dynamic")) {
            this.logger.debug("Converting raw format for table [{}]", tableName);

            builder.append("<sql>");
            builder.append("ALTER TABLE ");
            builder.append(tableName);
            builder.append(" ROW_FORMAT=DYNAMIC");
            builder.append("</sql>");

            dynamicTables.add(tableName);
        }
    }

    private String getRowFormat(String tableName) throws DataMigrationException
    {
        try {
            return getStore().executeRead(getXWikiContext(), new HibernateCallback<String>()
            {
                @Override
                public String doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    NativeQuery<String> query =
                        session.createNativeQuery("SELECT DISTINCT ROW_FORMAT FROM INFORMATION_SCHEMA.TABLES "
                            + "WHERE TABLE_SCHEMA = :schema AND TABLE_NAME = :table");
                    query.setParameter("schema", hibernateStore.getDatabaseFromWikiName());
                    query.setParameter("table", tableName);

                    String rawFormat = query.uniqueResult();

                    logger.debug("Row format for table [{}]: {}", tableName, rawFormat);

                    return rawFormat;
                }
            });
        } catch (XWikiException e) {
            throw new DataMigrationException("Failed to get unique keys", e);
        }
    }

    private boolean exists(PersistentClass entity, DatabaseMetaData databaseMetaData) throws SQLException
    {
        String databaseName = this.hibernateStore.getDatabaseFromWikiName();
        String tableName = this.hibernateStore.getConfiguredTableName(entity);

        ResultSet resultSet;
        if (this.hibernateStore.isCatalog()) {
            resultSet = databaseMetaData.getTables(databaseName, null, tableName, null);
        } else {
            resultSet = databaseMetaData.getTables(null, databaseName, tableName, null);
        }

        return resultSet.next();
    }

    private void update(Column column, Set<String> dynamicTables, StringBuilder builder) throws DataMigrationException
    {
        DatabaseProduct productName = this.hibernateStore.getDatabaseProductName();

        this.logger.debug("Database product name: {}", productName);

        if (productName == DatabaseProduct.MYSQL || productName == DatabaseProduct.MARIADB) {
            JdbcEnvironment jdbcEnvironment =
                this.hibernateStore.getConfigurationMetadata().getDatabase().getJdbcEnvironment();
            String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter()
                .format(column.getValue().getTable().getQualifiedTableName(), this.hibernateStore.getDialect());

            String quotedColumn = column.getQuotedName(this.hibernateStore.getDialect());

            this.logger.debug("Updating column [{}] in table [{}]", quotedColumn, tableName);

            // Make sure all MySQL/MariaDB tables use a DYNAMIC row format (required to support key prefix
            // length limit up to 3072 bytes)
            // Check again in case it's a special table which was missed in the previous pass
            setTableDYNAMIC(tableName, builder, dynamicTables);

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
            appendXmlAttribute("tableName", this.hibernateStore.getConfiguredTableName(column.getValue().getTable()),
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
