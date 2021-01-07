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

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.ExceptionHandlerCollectingImpl;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * This migration aims at fixing the Database Foreign Key names after the upgrade of Hibernate performed in XWiki 11.5.
 * The migration performs the following:
 *   1. it iterates on all tables to retrieve each foreign key
 *   2. for each foreign key it checks if the schema declares a foreign key with the same identifier
 *   3. if there's no foreign key with the same identifier, it checks if there's a foreign key concerning the same
 *      column and table
 *   4. if it finds one then it means and old foreign key existed with a different name: in that case we are just
 *      dropping the foreign key. The right foreign key with the appropriate name will be automatically recreated
 *      by hibernate.
 *
 * @version $Id$
 * @since 13.0
 * @since 12.10.1
 * @since 12.6.6
 */
@Component
@Named("R121001000XWIKI18145")
@Singleton
public class R121001000XWIKI18145DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private Logger logger;

    @Inject
    private HibernateStore hibernateStore;

    @Override
    public String getDescription()
    {
        return "Ensure that foreign keys have the proper name.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // We kept the class name of the original change performed for 12.10.1 version,
        // but we did change the version here for XWiki 12.6.6 so that the upgrade is taken into account
        // before any possible change in 12.6.7+
        return new XWikiDBVersion(120606000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Nothing to do here, everything's done as part of Liquibase update
    }

    /**
     * Prepare all the information to retrieve the database information.
     * This method returns both the requested Database information, but also the translation isolator: this one must be
     * used to release the connection.
     *
     * @param database the database for which to retrieve the information.
     * @param namespaceName the name of the database.
     * @return a pair containing the requested database information and the transaction isolator to release
     *          the connection
     * @throws DataMigrationException if the {@link ServiceRegistry} provided by the database is not an instance
     *                                of {@link ServiceRegistryImplementor}. In theory this should never happen.
     */
    private Pair<DatabaseInformation, DdlTransactionIsolator>
        retrieveDatabaseInformation(Database database, Namespace.Name namespaceName) throws DataMigrationException
    {
        ServiceRegistry serviceRegistry = database.getServiceRegistry();
        // We need the serviceRegistry to be a ServiceRegistryImplementor so that we can inject it
        // in HibernateSchemaManagementTool: if that's not the case we'll face a NPE.
        if (!(serviceRegistry instanceof ServiceRegistryImplementor)) {
            throw new DataMigrationException("Cannot perform this migration: serviceRegistry is not an instance of "
                + "ServiceRegistryImplementor");
        }

        // Code inspired by org.hibernate.tool.hbm2ddl.SchemaUpdate and
        // org.hibernate.tool.schema.internal.AbstractSchemaMigrator
        Map config = new HashMap(serviceRegistry.getService(ConfigurationService.class).getSettings());
        ExecutionOptions options =
            SchemaManagementToolCoordinator.buildExecutionOptions(config, new ExceptionHandlerCollectingImpl());
        HibernateSchemaManagementTool tool = new HibernateSchemaManagementTool();
        tool.injectServices((ServiceRegistryImplementor) serviceRegistry);

        JdbcContext jdbcContext = tool.resolveJdbcContext(options.getConfigurationValues());
        DdlTransactionIsolator ddlTransactionIsolator = tool.getDdlTransactionIsolator(jdbcContext);

        // Code inspired by org.hibernate.tool.schema.internal.Helper
        JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
        try {
            return Pair.of(
                new DatabaseInformationImpl(serviceRegistry, jdbcEnvironment, ddlTransactionIsolator, namespaceName),
                ddlTransactionIsolator);
        } catch (SQLException e) {
            throw new DataMigrationException("Cannot retrieve DatabaseInformation", e);
        }
    }

    /**
     * Search in the given table information for a foreign key matching the one given in parameter but with a different
     * identifier. Note that if a foreign key with same identifier is immediately found, the method returns null: we
     * consider they're not conflicting.
     *
     * @param foreignKey the schema foreign key for which we want to check if there's a conflicting key.
     * @param tableInformation the table information of the current database.
     * @param identifierHelper an helper to handle hibernate identifiers.
     * @return a foreign key information if there's a conflicting key, else {@code null}.
     */
    private ForeignKeyInformation getConflictingForeignKey(
        ForeignKey foreignKey, TableInformation tableInformation, IdentifierHelper identifierHelper)
    {
        Identifier identifier = identifierHelper.toIdentifier(foreignKey.getName());

        // We only check for conflicts if there's no key with the exact same name:
        // the rationale here is that if there's a key with the same name, the current key already exist and it's not
        // a conflict.
        if (tableInformation.getForeignKey(identifier) == null) {
            for (ForeignKeyInformation foreignKeyInformation : tableInformation.getForeignKeys()) {
                if (isForeignKeyMatching(foreignKey, foreignKeyInformation, identifierHelper)) {
                    return foreignKeyInformation;
                }
            }
        }
        return null;
    }

    /**
     * Check if the schema foreign key and the database foreign key are matching.
     * The identifiers of both keys are different so we are checking if they concerns same tables and columns.
     *
     * @param foreignKey the schema foreign key.
     * @param foreignKeyInformation the database foreign key from same table.
     * @param identifierHelper an hibernate helper to handle identifiers.
     * @return {@code true} if the keys are matching.
     */
    private boolean isForeignKeyMatching(ForeignKey foreignKey, ForeignKeyInformation foreignKeyInformation,
        IdentifierHelper identifierHelper)
    {
        boolean result = false;

        // the foreign key from the schema only gives partial information:
        // it gives the referenced table and the referencing column and table.
        // We don't have the referenced column but here we infer that if the 3 information we have are matching
        // then the keys are matching: we don't use many foreign keys in XWiki and they're not complex.
        Table referencedTable = foreignKey.getReferencedTable();
        List<Column> columns = foreignKey.getColumns();
        List<ForeignKeyInformation.ColumnReferenceMapping> columnMappings =
            StreamSupport.stream(foreignKeyInformation.getColumnReferenceMappings().spliterator(), false).collect(
                Collectors.toList());

        // We only have keys mapping one column to another in XWiki, so it simplifies the check.
        if (columnMappings.size() == 1 && columns.size() == 1) {
            ForeignKeyInformation.ColumnReferenceMapping mapping = columnMappings.get(0);
            ColumnInformation referencedColumnMetadata = mapping.getReferencedColumnMetadata();
            ColumnInformation referencingColumnMetadata = mapping.getReferencingColumnMetadata();
            Column referencingColumn = columns.get(0);

            result = identifierHelper.toIdentifier(referencingColumn.getName())
                .equals(referencingColumnMetadata.getColumnIdentifier())
                && referencedColumnMetadata.getContainingTableInformation().getName().getTableName()
                .equals(referencedTable.getNameIdentifier());
        }

        return result;
    }

    private void addXmlProperty(String property, String value, StringBuilder stringBuilder)
    {
        stringBuilder.append(property);
        stringBuilder.append("=\"");
        stringBuilder.append(value);
        stringBuilder.append("\" ");
    }

    private void buildLoggerForeignKeyName(StringBuilder stringBuilder, String namePart, boolean last)
    {
        stringBuilder.append(namePart);
        if (!last) {
            stringBuilder.append(".");
        }
    }

    private void writeChanges(StringBuilder changes, Table table, ForeignKey foreignKey,
        ForeignKeyInformation conflictingForeignKey)
    {
        StringBuilder foreignKeyName = new StringBuilder();
        changes.append("<dropForeignKeyConstraint ");
        if (!StringUtils.isEmpty(table.getCatalog())) {
            this.addXmlProperty("baseTableCatalogName", table.getCatalog(), changes);
            this.buildLoggerForeignKeyName(foreignKeyName, table.getCatalog(), false);
        }
        if (!StringUtils.isEmpty(table.getSchema())) {
            this.addXmlProperty("baseTableSchemaName", table.getSchema(), changes);
            this.buildLoggerForeignKeyName(foreignKeyName, table.getSchema(), false);
        }
        this.addXmlProperty("baseTableName", table.getName(), changes);
        this.buildLoggerForeignKeyName(foreignKeyName, table.getName(), false);

        String constraintName = conflictingForeignKey.getForeignKeyIdentifier().getCanonicalName();
        this.addXmlProperty("constraintName", constraintName, changes);
        this.buildLoggerForeignKeyName(foreignKeyName, constraintName, true);

        changes.append("/>");

        logger.info("Dropping foreign key [{}]: this key will be recreated with the following name: [{}]",
            foreignKeyName.toString(), foreignKey.getName());
    }

    private Namespace.Name retrieveNamespaceName(IdentifierHelper identifierHelper)
    {
        String dbName = this.hibernateStore.getDatabaseFromWikiName();
        boolean isCatalog = this.hibernateStore.isCatalog();

        Namespace.Name result;
        if (isCatalog) {
            result = new Namespace.Name(identifierHelper.toIdentifier(dbName), null);
        } else {
            result = new Namespace.Name(null, identifierHelper.toIdentifier(dbName));
        }

        return result;
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        Database database = this.getStore().getMetadata().getDatabase();
        IdentifierHelper identifierHelper = database.getJdbcEnvironment().getIdentifierHelper();

        // retrieve tables information from the schema.
        Collection<Table> tables = database.getDefaultNamespace().getTables();

        Namespace.Name namespaceName = retrieveNamespaceName(identifierHelper);

        DdlTransactionIsolator transactionIsolator = null;
        boolean hasChanges = false;
        StringBuilder changes = new StringBuilder();
        try {
            // retrieve database information from the current DB.
            Pair<DatabaseInformation, DdlTransactionIsolator> dbInfoAndIsolator =
                this.retrieveDatabaseInformation(database, namespaceName);

            DatabaseInformation databaseInformation = dbInfoAndIsolator.getLeft();
            transactionIsolator = dbInfoAndIsolator.getRight();

            // We iterate over all tables of the schema first
            for (Table table : tables) {

                TableInformation tableInformation =
                    databaseInformation.getTableInformation(namespaceName, table.getNameIdentifier());

                // if the table doesn't exist yet in the database, we're safe.
                if (tableInformation != null) {
                    // And over all foreign keys for each table
                    for (Iterator<ForeignKey> it = table.getForeignKeyIterator(); it.hasNext();) {
                        ForeignKey foreignKey = it.next();

                        ForeignKeyInformation conflictingForeignKey =
                            getConflictingForeignKey(foreignKey, tableInformation, identifierHelper);

                        // We found a conflicting foreign key: it means that the name changed so we need to drop the
                        // currently existing foreign key so that hibernate is able to recreate it back just after.
                        if (conflictingForeignKey != null) {
                            this.writeChanges(changes, table, foreignKey, conflictingForeignKey);
                            hasChanges = true;
                        }
                    }
                }
            }
        } finally {
            // Ensure to close the connection.
            if (transactionIsolator != null) {
                transactionIsolator.release();
            }
        }
        String result = "";

        if (hasChanges) {
            result = String.format("<changeSet author=\"xwikiorg\" id=\"R%s\">%s</changeSet>",
                this.getVersion().getVersion(), changes.toString());
        }
        return result;
    }
}
