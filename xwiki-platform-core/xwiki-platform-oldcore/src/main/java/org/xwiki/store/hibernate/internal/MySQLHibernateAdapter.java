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
package org.xwiki.store.hibernate.internal;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.NativeQuery;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.store.hibernate.AbstractHibernateAdapter;
import org.xwiki.store.hibernate.HibernateAdapter;
import org.xwiki.store.hibernate.HibernateStoreException;

import com.xpn.xwiki.internal.store.hibernate.HibernateStore;

/**
 * The default {@link HibernateAdapter} for MySQL. Based on MySQL 5.7 specifications.
 * 
 * @version $Id$
 * @since 17.1.0RC1
 */
@Component
@Singleton
@Named(MySQLHibernateAdapter.HINT)
public class MySQLHibernateAdapter extends AbstractHibernateAdapter
{
    /**
     * The role hint of the component.
     */
    public static final String HINT = "mysql";

    /**
     * The xwiki.properties property controlling the conversion of the tables to the database collation at startup.
     */
    private static final String PROPERTY_FIX_TABLE_COLLATION = "store.hibernate.mysql.fixTableCollation";

    private static final String SCHEMA_PARAMETER = "schema";

    private static final String ALTER_TABLE = "ALTER TABLE ";

    private static final String ROW_FORMAT = " ROW_FORMAT=";

    private static final String CHARACTER_SET_PARAMETER = "characterSet";

    /**
     * The suffix of the binary collations (e.g. utf8mb4_bin), the only ones supported by XWiki.
     */
    private static final String BINARY_COLLATION_SUFFIX = "_bin";

    /**
     * The maximum number of bytes of a character in the character sets able to store any character (e.g. utf8mb4).
     */
    private static final int UTF8MB4_MAX_CHARACTER_LENGTH = 4;

    /**
     * The maximum length of an InnoDB index key with the DYNAMIC and COMPRESSED row formats (the ones used by XWiki).
     */
    private static final long MAX_KEY_LENGTH = 3072;

    /**
     * The size in an index key of a non string column (a pessimistic estimation, enough to tell if a key fits).
     */
    private static final int NON_STRING_LENGTH = 8;

    /**
     * The row formats which limit the index keys to 767 bytes.
     */
    private static final Set<String> LEGACY_ROW_FORMATS = Set.of("compact", "redundant");

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @Inject
    private HibernateStore store;

    @Override
    public void repairDatabase(Metadata metadata) throws HibernateStoreException
    {
        if (!this.xwikiProperties.getProperty(PROPERTY_FIX_TABLE_COLLATION, true)) {
            this.logger.debug("Skipping the conversion of the tables to the database collation since [{}] is disabled",
                PROPERTY_FIX_TABLE_COLLATION);

            return;
        }

        try (Session session = getSessionFactory().openSession()) {
            convertToDatabaseCollation(metadata, session);
        } catch (Exception e) {
            throw new HibernateStoreException(String.format("Failed to convert the tables to the database character"
                + " set and a binary collation. The conversion at startup can be disabled by setting [%s] to false in"
                + " xwiki.properties.", PROPERTY_FIX_TABLE_COLLATION), e);
        }
    }

    @Override
    public void updateDatabaseAfter(Metadata metadata, Session session) throws HibernateStoreException
    {
        updateRowFormats(metadata, session);
    }

    protected void updateRowFormats(Metadata metadata, Session session) throws HibernateStoreException
    {
        // Gather tables row formats
        Map<String, String> rowFormats = getRowFormats(session);

        boolean compressionAllowed = isCompressionAllowed();

        // Make sure each table row format matches the configuration
        for (PersistentClass entity : metadata.getEntityBindings()) {
            // Get the exact table name for the entity
            String tableName = getTableName(entity);

            // Check if the table is configured to be compressed
            boolean compressed = compressionAllowed && entity.getMetaAttribute(META_ATTRIBUTE_COMPRESSED) != null;

            // Compute the query statement
            String statement = getAlterRowFormatString(tableName, compressed, rowFormats, session);
            if (statement != null) {
                // Create the query
                NativeQuery<?> query = session.createNativeQuery(statement);

                // Execute the query
                this.logger.info("Changing the table row type (currently [{}]) with [{}]", rowFormats.get(tableName),
                    statement);
                session.getTransaction().begin();
                query.executeUpdate();
                session.getTransaction().commit();
            }
        }
    }

    /**
     * @param tableName the name of the table to modify
     * @param compressed true if the table should be compressed
     * @param rowFormats the current row formats
     * @param session the session in which to execute the query
     * @return the SQL statement to execute if the table is not already compressed
     * @throws HibernateStoreException when failing to check if the table is already compressed
     */
    public String getAlterRowFormatString(String tableName, boolean compressed, Map<String, String> rowFormats,
        Session session) throws HibernateStoreException
    {
        String rowFormat = rowFormats.get(tableName);

        this.logger.debug("Row format for table [{}]: [{}]", tableName, rowFormat);

        String expectedRowFormat = compressed ? getCompressedRowFormat() : getDefaultRowFormat();

        if (!StringUtils.equalsIgnoreCase(rowFormat, expectedRowFormat)) {
            // Qualify the table with the database of the current wiki: the statement is executed on a session obtained
            // straight from the session factory, whose pooled connection is still on the database selected by whoever
            // used it last. An unqualified table would be resolved in that other database.
            return ALTER_TABLE + getQualifiedTableName(tableName) + ROW_FORMAT + expectedRowFormat;
        }

        return null;
    }

    /**
     * @param tableName the name of the table to qualify
     * @return the table name prefixed with the database of the current wiki, so that the statement does not depend on
     *         the database currently selected in the JDBC connection
     */
    private String getQualifiedTableName(String tableName)
    {
        String database = getDatabaseFromWikiName();

        if (database == null) {
            return tableName;
        }

        return escapeDatabaseName(database) + '.' + tableName;
    }

    /**
     * @param session the session in which to execute the query
     * @return the row format of each table in the current database
     * @throws IllegalStateException when there is no current wiki database
     */
    public Map<String, String> getRowFormats(Session session)
    {
        String currentDatabase = getDatabaseFromWikiName();
        if (currentDatabase == null) {
            throw new IllegalStateException("Cannot get the tables row formats without a current wiki database");
        }

        NativeQuery<Object[]> query = session.createNativeQuery("SELECT DISTINCT NAME, ROW_FORMAT FROM "
            + getInformationShemaInnoDBTables() + " WHERE LOWER(NAME) LIKE '" + currentDatabase.toLowerCase() + "/%'");

        List<Object[]> results = query.list();

        Map<String, String> rowFormats = HashMap.newHashMap(results.size());
        for (Object[] entry : results) {
            String completeName = (String) entry[0];
            String tableName = completeName.substring(completeName.indexOf('/') + 1);
            rowFormats.put(tableName, (String) entry[1]);
        }

        return rowFormats;
    }

    /**
     * @return the table where to find the InnoDB tables metadata
     */
    public String getInformationShemaInnoDBTables()
    {
        return "INFORMATION_SCHEMA.INNODB_SYS_TABLES";
    }

    /**
     * @return the row format to use by default
     */
    public String getDefaultRowFormat()
    {
        return "Dynamic";
    }

    /**
     * @return the row format to use for compressed tables
     */
    public String getCompressedRowFormat()
    {
        return "Compressed";
    }

    @Override
    public boolean isCompressionAllowed()
    {
        return getCompressionAllowedConfiguration().orElse(true);
    }

    /**
     * @return true if a unique index longer than the maximum key length is supported (by transparently switching it
     *         to a hash based index)
     * @since 18.10.0RC1
     */
    protected boolean isLongUniqueIndexSupported()
    {
        return false;
    }

    /**
     * Convert the tables of the current wiki database to the character set configured on the database, with a binary
     * collation, and re-create the foreign keys dropped for the conversion.
     *
     * @param metadata the configuration of the database, used to know which tables to convert
     * @param session the session in which to execute the conversion
     * @see #getConvertToDatabaseCollationStatements(Metadata, Session)
     * @since 18.10.0RC1
     */
    public void convertToDatabaseCollation(Metadata metadata, Session session)
    {
        Conversion conversion = prepareConversion(metadata, session);
        List<String> statements = conversion.statements;
        if (statements.isEmpty()) {
            return;
        }

        this.logger.info("Converting the tables of database [{}] ([{}] statements to execute). Each"
            + " converted table is rebuilt, which can take a long time for big tables: don't stop XWiki in the"
            + " meantime (a restart would resume the conversion anyway). The conversion at startup can be disabled by"
            + " setting [{}] to false in xwiki.properties.", getDatabaseFromWikiName(), statements.size(),
            PROPERTY_FIX_TABLE_COLLATION);

        long conversionStart = System.currentTimeMillis();
        for (int i = 0; i < statements.size(); ++i) {
            String statement = statements.get(i);

            this.logger.info("Executing statement [{}/{}]: [{}]", i + 1, statements.size(), statement);

            long statementStart = System.currentTimeMillis();
            session.getTransaction().begin();
            session.createNativeQuery(statement).executeUpdate();
            session.getTransaction().commit();

            this.logger.info("Statement [{}/{}] executed in [{}] ms", i + 1, statements.size(),
                System.currentTimeMillis() - statementStart);
        }

        recreateForeignKeys(conversion.foreignKeyStatements, session);

        this.logger.info("Tables of database [{}] converted in [{}] ms", getDatabaseFromWikiName(),
            System.currentTimeMillis() - conversionStart);
    }

    /**
     * Re-create the foreign keys dropped for the conversion, without the foreign key checks: this way they are added
     * without rebuilding the tables (the conversion does not modify the data, so it does not need to be checked
     * again). A failure is only reported with a warning since the Hibernate schema update, which runs right after,
     * re-creates the missing foreign keys of the Hibernate mapping anyway (rebuilding the table).
     */
    private void recreateForeignKeys(List<String> statements, Session session)
    {
        if (statements.isEmpty()) {
            return;
        }

        // The foreign key checks are a connection setting, so all the statements must use the same connection
        session.doWork(connection -> {
            try (Statement jdbcStatement = connection.createStatement()) {
                jdbcStatement.execute("SET FOREIGN_KEY_CHECKS=0");
                try {
                    for (String statement : statements) {
                        recreateForeignKey(statement, jdbcStatement);
                    }
                } finally {
                    // Don't give back the connection to the pool without the foreign key checks
                    jdbcStatement.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            }
        });
    }

    private void recreateForeignKey(String statement, Statement jdbcStatement)
    {
        this.logger.info("Re-creating foreign key: [{}]", statement);

        try {
            jdbcStatement.execute(statement);
        } catch (SQLException e) {
            this.logger.warn("Failed to re-create a foreign key with [{}]: {}. The Hibernate schema update re-creates"
                + " the missing foreign keys of the Hibernate mapping, the other ones have to be re-created manually.",
                statement, ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Compute the statements to execute to convert each standard table of the Hibernate mapping which does not use the
     * character set configured on the current wiki database with a binary collation (or which contains a column which
     * does not use them). XWiki requires a binary collation, so the target collation is the one configured on the
     * database when it's binary (e.g. {@code utf8mb4_bin}), and the binary collation of the database character set
     * otherwise (e.g. {@code utf8mb4_bin} for {@code utf8mb4_0900_ai_ci}).
     * <p>
     * The tables of the custom mappings of the XWiki classes are not converted, since their structure is controlled by
     * the users (and might not support a character set using more bytes per character, for example).
     * <p>
     * In addition to the {@code CONVERT TO CHARACTER SET} of each of those tables (which also switches a legacy or
     * unexpected row format to the expected one, so that the table is rebuilt only once), the statements:
     * <ul>
     * <li>drop the foreign keys involving their string columns, since MySQL and MariaDB refuse to convert a column used
     * in a foreign key</li>
     * <li>drop their indexes which would exceed the maximum key length with the new character set (typically custom
     * indexes, since the ones of the Hibernate mapping are designed to fit), since they would make the conversion
     * fail; a warning gives the definition of each of them, so that it can be re-created with a shorter prefix</li>
     * </ul>
     * <p>
     * Only the conversions which cannot lose anything are done: a table which can contain characters not supported by
     * the database character set (e.g. a {@code utf8mb4} table in a {@code utf8mb3} database) is not converted, and
     * neither are the tables linked to it by a foreign key on string columns, since that foreign key could not be
     * re-created.
     * <p>
     * <strong>WARNING: the dropped foreign keys are not re-created by those statements (unlike
     * {@link #convertToDatabaseCollation(Metadata, Session)}), only by the Hibernate schema update when XWiki is
     * restarted.</strong>
     *
     * @param metadata the configuration of the database, used to know which tables to convert
     * @param session the session in which to execute the queries
     * @return the statements to execute in that order, empty if all the tables already use the expected collation
     * @since 18.10.0RC1
     */
    public List<String> getConvertToDatabaseCollationStatements(Metadata metadata, Session session)
    {
        return prepareConversion(metadata, session).statements;
    }

    private Conversion prepareConversion(Metadata metadata, Session session)
    {
        Conversion conversion = new Conversion();

        String databaseName = getDatabaseFromWikiName();

        Object[] databaseDefaults = getDatabaseDefaults(databaseName, session);
        if (databaseDefaults == null) {
            return conversion;
        }

        String characterSet = (String) databaseDefaults[0];
        // XWiki requires a binary collation, whatever the database is configured with
        String databaseCollation = (String) databaseDefaults[1];
        String collation = isBinary(databaseCollation) ? databaseCollation : characterSet + BINARY_COLLATION_SUFFIX;

        // The collations (other than the expected one) used by each table to convert
        Map<String, Set<String>> tables = getTablesToConvert(metadata, databaseName, collation, session);
        if (tables.isEmpty()) {
            return conversion;
        }

        int maxCharacterLength = getMaxCharacterLength(characterSet, session);
        List<ForeignKey> foreignKeys = getStringForeignKeys(databaseName, session);

        if (maxCharacterLength < UTF8MB4_MAX_CHARACTER_LENGTH) {
            removeWideTables(databaseName, characterSet, maxCharacterLength, tables, foreignKeys, session);
            if (tables.isEmpty()) {
                return conversion;
            }
        }

        this.logger.info("Tables {} of database [{}] need to be converted to character set [{}] and collation [{}]",
            tables.keySet(), databaseName, characterSet, collation);

        // Drop the foreign keys involving string columns of the tables to convert since MySQL/MariaDB refuse to convert
        // those columns (and remember how to re-create them)
        addForeignKeyStatements(tables.keySet(), foreignKeys, conversion);

        // Drop the indexes which would exceed the maximum key length with the new character set
        addDropIndexStatements(databaseName, tables.keySet(), maxCharacterLength, session, conversion.statements);

        // Convert the tables
        Map<String, String> rowFormats = getRowFormats(session);
        Map<String, Boolean> entityTables = getEntityTables(metadata);
        for (String tableName : tables.keySet()) {
            StringBuilder statement = new StringBuilder(ALTER_TABLE).append(getQualifiedTableName(tableName));

            // Switching the row format in the same statement rebuilds the table only once, and makes sure the
            // conversion gets the maximum key length of the DYNAMIC and COMPRESSED row formats
            String rowFormat =
                getExpectedRowFormat(rowFormats.get(tableName), entityTables.get(tableName.toLowerCase()));
            if (rowFormat != null) {
                statement.append(ROW_FORMAT).append(rowFormat).append(',');
            }

            conversion.statements.add(statement.append(" CONVERT TO CHARACTER SET ").append(characterSet)
                .append(" COLLATE ").append(collation).toString());
        }

        return conversion;
    }

    private boolean isBinary(String collation)
    {
        return collation.toLowerCase().endsWith(BINARY_COLLATION_SUFFIX);
    }

    /**
     * @param rowFormat the current row format of the table, {@code null} if unknown
     * @param compressed true if the table should be compressed, {@code null} if it's not the table of an entity (e.g.
     *            a collection table)
     * @return the row format to switch the table to, {@code null} to keep the current one
     */
    private String getExpectedRowFormat(String rowFormat, Boolean compressed)
    {
        if (rowFormat == null) {
            return null;
        }

        String expectedRowFormat;
        if (compressed != null) {
            // The row format of the entity tables is enforced right after the schema update anyway
            expectedRowFormat = compressed ? getCompressedRowFormat() : getDefaultRowFormat();
        } else if (LEGACY_ROW_FORMATS.contains(rowFormat.toLowerCase())) {
            expectedRowFormat = getDefaultRowFormat();
        } else {
            return null;
        }

        return StringUtils.equalsIgnoreCase(rowFormat, expectedRowFormat) ? null : expectedRowFormat;
    }

    /**
     * @param metadata the configuration of the database
     * @return the (lower case) name of the table of each entity, associated with true if it should be compressed
     */
    private Map<String, Boolean> getEntityTables(Metadata metadata)
    {
        boolean compressionAllowed = isCompressionAllowed();

        Map<String, Boolean> entityTables = new HashMap<>();
        for (PersistentClass entity : metadata.getEntityBindings()) {
            entityTables.put(getTableName(entity).toLowerCase(), compressionAllowed && isCompressed(entity));
        }

        return entityTables;
    }

    /**
     * Remove from the tables to convert the ones which can contain characters not supported by the database character
     * set, and the ones linked to them by a foreign key on string columns (which could not be re-created since the
     * columns would not use the same character set anymore).
     */
    private void removeWideTables(String databaseName, String characterSet, int maxCharacterLength,
        Map<String, Set<String>> tables, List<ForeignKey> foreignKeys, Session session)
    {
        NativeQuery<String> query = session.createNativeQuery("SELECT DISTINCT c.TABLE_NAME "
            + "FROM INFORMATION_SCHEMA.COLUMNS c JOIN INFORMATION_SCHEMA.CHARACTER_SETS s "
            + "ON s.CHARACTER_SET_NAME = c.CHARACTER_SET_NAME "
            + "WHERE c.TABLE_SCHEMA = :schema AND s.MAXLEN > :maxLength");
        query.setParameter(SCHEMA_PARAMETER, databaseName);
        query.setParameter("maxLength", maxCharacterLength);

        Set<String> wideTables =
            query.list().stream().filter(tables::containsKey).collect(Collectors.toCollection(TreeSet::new));

        boolean changed = !wideTables.isEmpty();
        while (changed) {
            changed = false;
            for (ForeignKey foreignKey : foreignKeys) {
                changed |= addLinkedTable(foreignKey.tableName, foreignKey.referencedTableName, tables, wideTables);
                changed |= addLinkedTable(foreignKey.referencedTableName, foreignKey.tableName, tables, wideTables);
            }
        }

        if (wideTables.isEmpty()) {
            return;
        }

        // Keeping a binary table in a wider character set is fine, but a non binary one is a problem
        Set<String> nonBinaryTables = wideTables.stream()
            .filter(tableName -> !tables.get(tableName).stream().allMatch(this::isBinary))
            .collect(Collectors.toCollection(TreeSet::new));
        if (!nonBinaryTables.isEmpty()) {
            this.logger.warn("Tables {} of database [{}] use a non binary collation, which XWiki does not support, but"
                + " won't be converted to its character set [{}] since they (or a table linked to them by a foreign"
                + " key) can contain characters it does not support. Converting them to utf8mb4_bin is recommended.",
                nonBinaryTables, databaseName, characterSet);
        }

        this.logger.debug("Tables {} of database [{}] won't be converted to its character set [{}] since they (or a"
            + " table linked to them by a foreign key) can contain characters it does not support", wideTables,
            databaseName, characterSet);

        tables.keySet().removeAll(wideTables);
    }

    private boolean addLinkedTable(String tableName, String linkedTableName, Map<String, Set<String>> tables,
        Set<String> wideTables)
    {
        return wideTables.contains(tableName) && tables.containsKey(linkedTableName)
            && wideTables.add(linkedTableName);
    }

    private void addForeignKeyStatements(Set<String> tables, List<ForeignKey> foreignKeys, Conversion conversion)
    {
        for (ForeignKey foreignKey : foreignKeys) {
            if (tables.contains(foreignKey.tableName) || tables.contains(foreignKey.referencedTableName)) {
                String alterTable = ALTER_TABLE + getQualifiedTableName(foreignKey.tableName);
                String name = quote(foreignKey.name);

                conversion.statements.add(alterTable + " DROP FOREIGN KEY " + name);
                conversion.foreignKeyStatements.add(alterTable + " ADD CONSTRAINT " + name + " FOREIGN KEY"
                    + toColumnList(foreignKey.columns) + " REFERENCES " + quote(foreignKey.referencedSchema) + '.'
                    + foreignKey.referencedTableName + toColumnList(foreignKey.referencedColumns) + " ON UPDATE "
                    + foreignKey.updateRule + " ON DELETE " + foreignKey.deleteRule);
            }
        }
    }

    private void addDropIndexStatements(String databaseName, Set<String> tables, int maxCharacterLength,
        Session session, List<String> statements)
    {
        for (Index index : getIndexes(databaseName, tables, maxCharacterLength, session)) {
            if (index.length > MAX_KEY_LENGTH && !(index.unique && isLongUniqueIndexSupported())) {
                this.logger.warn(
                    "Index [{}] (key length of [{}] bytes after the conversion) exceeds the maximum key length of [{}]"
                        + " bytes and will be dropped. It can be re-created with a shorter prefix using [{}].",
                    index.name, index.length, MAX_KEY_LENGTH, getIndexDefinition(index));

                statements.add(ALTER_TABLE + getQualifiedTableName(index.tableName) + " DROP INDEX "
                    + quote(index.name));
            }
        }
    }

    private String quote(String identifier)
    {
        // Escaping a database name is actually quoting it as any other identifier
        return escapeDatabaseName(identifier);
    }

    private String getIndexDefinition(Index index)
    {
        return (index.unique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ") + quote(index.name) + " ON "
            + quote(getDatabaseFromWikiName()) + '.' + quote(index.tableName) + toColumnList(index.columns);
    }

    private String toColumnList(List<String> columns)
    {
        return " (" + String.join(", ", columns) + ')';
    }

    private Object[] getDatabaseDefaults(String databaseName, Session session)
    {
        NativeQuery<Object[]> query = session.createNativeQuery(
            "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM INFORMATION_SCHEMA.SCHEMATA "
                + "WHERE SCHEMA_NAME = :schema");
        query.setParameter(SCHEMA_PARAMETER, databaseName);

        List<Object[]> results = query.list();

        return results.isEmpty() ? null : results.get(0);
    }

    private int getMaxCharacterLength(String characterSet, Session session)
    {
        NativeQuery<Number> query = session.createNativeQuery(
            "SELECT MAXLEN FROM INFORMATION_SCHEMA.CHARACTER_SETS WHERE CHARACTER_SET_NAME = :characterSet");
        query.setParameter(CHARACTER_SET_PARAMETER, characterSet);

        Number maxLength = query.uniqueResult();
        if (maxLength == null) {
            throw new IllegalStateException(String.format("Unknown character set [%s]", characterSet));
        }

        return maxLength.intValue();
    }

    private Map<String, Set<String>> getTablesToConvert(Metadata metadata, String databaseName, String collation,
        Session session)
    {
        NativeQuery<Object[]> query = session.createNativeQuery("SELECT TABLE_NAME, TABLE_COLLATION "
            + "FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = :schema AND TABLE_TYPE = 'BASE TABLE' "
            + "AND TABLE_COLLATION <> :collation UNION SELECT TABLE_NAME, COLLATION_NAME "
            + "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = :schema AND COLLATION_NAME <> :collation");
        query.setParameter(SCHEMA_PARAMETER, databaseName);
        query.setParameter("collation", collation);

        Set<String> standardTables = getStandardTables(metadata);

        Map<String, Set<String>> tables = new TreeMap<>();
        for (Object[] tableCollation : query.list()) {
            String tableName = (String) tableCollation[0];

            if (standardTables.contains(tableName.toLowerCase())) {
                tables.computeIfAbsent(tableName, key -> new HashSet<>()).add((String) tableCollation[1]);
            }
        }

        return tables;
    }

    /**
     * @param metadata the configuration of the database
     * @return the (lower case) names of the tables of the Hibernate mapping, except the ones of the custom mappings of
     *         the XWiki classes
     */
    private Set<String> getStandardTables(Metadata metadata)
    {
        Set<String> tables = metadata.collectTableMappings().stream().map(table -> getTableName(table).toLowerCase())
            .collect(Collectors.toCollection(HashSet::new));

        for (PersistentClass entity : metadata.getEntityBindings()) {
            String tableName = getTableName(entity);

            // The custom mapping of an XWiki class is injected with a table name derived from the class name
            if (tableName.equalsIgnoreCase(this.store.toDynamicMappingTableName(entity.getEntityName()))) {
                tables.remove(tableName.toLowerCase());
            }
        }

        return tables;
    }

    private List<ForeignKey> getStringForeignKeys(String databaseName, Session session)
    {
        NativeQuery<Object[]> query = session.createNativeQuery(
            "SELECT k.TABLE_NAME, k.CONSTRAINT_NAME, k.REFERENCED_TABLE_SCHEMA, k.REFERENCED_TABLE_NAME, "
                + "r.UPDATE_RULE, r.DELETE_RULE, k.COLUMN_NAME, k.REFERENCED_COLUMN_NAME, c.COLLATION_NAME "
                + "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS r "
                + "ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA AND r.TABLE_NAME = k.TABLE_NAME "
                + "AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME JOIN INFORMATION_SCHEMA.COLUMNS c "
                + "ON c.TABLE_SCHEMA = k.TABLE_SCHEMA AND c.TABLE_NAME = k.TABLE_NAME "
                + "AND c.COLUMN_NAME = k.COLUMN_NAME "
                + "WHERE k.TABLE_SCHEMA = :schema AND k.REFERENCED_TABLE_NAME IS NOT NULL "
                + "ORDER BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.ORDINAL_POSITION");
        query.setParameter(SCHEMA_PARAMETER, databaseName);

        Map<List<String>, ForeignKey> foreignKeys = new LinkedHashMap<>();
        for (Object[] part : query.list()) {
            ForeignKey foreignKey = foreignKeys.computeIfAbsent(List.of((String) part[0], (String) part[1]),
                key -> new ForeignKey(part));

            foreignKey.columns.add(quote((String) part[6]));
            foreignKey.referencedColumns.add(quote((String) part[7]));
            foreignKey.string |= part[8] != null;
        }

        // Only the foreign keys involving string columns prevent the conversion
        return foreignKeys.values().stream().filter(foreignKey -> foreignKey.string).toList();
    }

    private Collection<Index> getIndexes(String databaseName, Set<String> tables, int maxCharacterLength,
        Session session)
    {
        NativeQuery<Object[]> query = session.createNativeQuery(
            "SELECT s.TABLE_NAME, s.INDEX_NAME, s.COLUMN_NAME, s.SUB_PART, c.COLLATION_NAME, "
                + "c.CHARACTER_MAXIMUM_LENGTH, c.CHARACTER_OCTET_LENGTH, s.NON_UNIQUE "
                + "FROM INFORMATION_SCHEMA.STATISTICS s JOIN INFORMATION_SCHEMA.COLUMNS c "
                + "ON c.TABLE_SCHEMA = s.TABLE_SCHEMA AND c.TABLE_NAME = s.TABLE_NAME "
                + "AND c.COLUMN_NAME = s.COLUMN_NAME "
                + "WHERE s.TABLE_SCHEMA = :schema AND s.INDEX_NAME <> 'PRIMARY' "
                // Only B-tree indexes are subject to the maximum key length (not FULLTEXT or SPATIAL ones)
                + "AND s.INDEX_TYPE = 'BTREE' "
                + "ORDER BY s.TABLE_NAME, s.INDEX_NAME, s.SEQ_IN_INDEX");
        query.setParameter(SCHEMA_PARAMETER, databaseName);

        Map<List<String>, Index> indexes = new LinkedHashMap<>();
        for (Object[] part : query.list()) {
            String tableName = (String) part[0];

            if (tables.contains(tableName)) {
                Index index = indexes.computeIfAbsent(List.of(tableName, (String) part[1]),
                    key -> new Index(key.get(0), key.get(1), ((Number) part[7]).intValue() == 0));

                String column = quote((String) part[2]);
                index.columns.add(part[3] != null ? column + "(" + part[3] + ')' : column);
                index.length += getKeyPartLength(part, maxCharacterLength);
            }
        }

        return indexes.values();
    }

    /**
     * @param part the index part (sub part, collation, character length and octet length start at index 3)
     * @param maxCharacterLength the maximum number of bytes of a character in the new character set
     * @return the number of bytes taken by the part in the index key once the table is converted (a pessimistic
     *         estimation for the non string types)
     */
    private long getKeyPartLength(Object[] part, int maxCharacterLength)
    {
        Number subPart = (Number) part[3];

        // Non binary strings (the sub part and the maximum length are expressed in characters)
        if (part[4] != null) {
            return (subPart != null ? subPart : (Number) part[5]).longValue() * maxCharacterLength;
        }

        // Binary strings (the sub part and the maximum length are expressed in bytes)
        if (part[6] != null) {
            return (subPart != null ? subPart : (Number) part[6]).longValue();
        }

        return NON_STRING_LENGTH;
    }

    private static final class Conversion
    {
        private final List<String> statements = new ArrayList<>();

        private final List<String> foreignKeyStatements = new ArrayList<>();
    }

    private static final class ForeignKey
    {
        private final String tableName;

        private final String name;

        private final String referencedSchema;

        private final String referencedTableName;

        private final String updateRule;

        private final String deleteRule;

        private final List<String> columns = new ArrayList<>();

        private final List<String> referencedColumns = new ArrayList<>();

        private boolean string;

        /**
         * @param part the first part of the foreign key (table, name, referenced schema, referenced table, update rule
         *            and delete rule)
         */
        ForeignKey(Object[] part)
        {
            this.tableName = (String) part[0];
            this.name = (String) part[1];
            this.referencedSchema = (String) part[2];
            this.referencedTableName = (String) part[3];
            this.updateRule = (String) part[4];
            this.deleteRule = (String) part[5];
        }
    }

    private static final class Index
    {
        private final String tableName;

        private final String name;

        private final boolean unique;

        private final List<String> columns = new ArrayList<>();

        private long length;

        Index(String tableName, String name, boolean unique)
        {
            this.tableName = tableName;
            this.name = name;
            this.unique = unique;
        }
    }
}
