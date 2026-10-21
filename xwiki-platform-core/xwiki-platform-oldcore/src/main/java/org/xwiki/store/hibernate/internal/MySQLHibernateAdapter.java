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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.NativeQuery;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.hibernate.AbstractHibernateAdapter;
import org.xwiki.store.hibernate.HibernateAdapter;
import org.xwiki.store.hibernate.HibernateStoreException;

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

    private static final String SCHEMA_PARAMETER = "schema";

    private static final String ALTER_TABLE = "ALTER TABLE ";

    /**
     * The maximum length of an InnoDB index key with the DYNAMIC and COMPRESSED row formats (the ones used by XWiki).
     */
    private static final long MAX_KEY_LENGTH = 3072;

    private static final String TYPE_TIME = "time";

    private static final String TYPE_TIMESTAMP = "timestamp";

    private static final String TYPE_DATETIME = "datetime";

    /**
     * The size in an index key of the non string types which don't depend on their definition.
     */
    private static final Map<String, Integer> FIXED_TYPE_LENGTHS =
        Map.ofEntries(Map.entry("tinyint", 1), Map.entry("smallint", 2), Map.entry("mediumint", 3),
            Map.entry("int", 4), Map.entry("integer", 4), Map.entry("bigint", 8), Map.entry("float", 4),
            Map.entry("double", 8), Map.entry("real", 8), Map.entry("year", 1), Map.entry("date", 3),
            Map.entry(TYPE_TIME, 3), Map.entry(TYPE_TIMESTAMP, 4), Map.entry(TYPE_DATETIME, 5));

    /**
     * The size in an index key of the types which are not explicitly supported (a pessimistic estimation).
     */
    private static final int DEFAULT_TYPE_LENGTH = 8;

    private static final Set<String> FRACTIONAL_TYPES = Set.of(TYPE_TIME, TYPE_TIMESTAMP, TYPE_DATETIME);

    @Override
    public void repairDatabase(Metadata metadata) throws HibernateStoreException
    {
        try (Session session = getSessionFactory().openSession()) {
            convertToDatabaseCollation(metadata, session);
        } catch (Exception e) {
            throw new HibernateStoreException(
                "Failed to convert the tables to the collation configured on the database", e);
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
            return ALTER_TABLE + getQualifiedTableName(tableName) + " ROW_FORMAT=" + expectedRowFormat;
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
     * Convert the tables of the current wiki database to the character set and collation configured on the database.
     * <p>
     * <strong>WARNING: the foreign keys involving the converted string columns are dropped and are only re-created by
     * the Hibernate schema update, so XWiki must be restarted right after calling this method (unless it's called
     * right before the schema update, as done by {@link #repairDatabase(Metadata)}).</strong>
     *
     * @param metadata the configuration of the database, used to know which tables to convert
     * @param session the session in which to execute the conversion
     * @see #getConvertToDatabaseCollationStatements(Metadata, Session)
     * @since 18.10.0RC1
     */
    public void convertToDatabaseCollation(Metadata metadata, Session session)
    {
        for (String statement : getConvertToDatabaseCollationStatements(metadata, session)) {
            this.logger.info("Executing [{}]", statement);

            session.getTransaction().begin();
            session.createNativeQuery(statement).executeUpdate();
            session.getTransaction().commit();
        }
    }

    /**
     * Compute the statements to execute to convert each table of the Hibernate mapping which does not use the
     * character set and collation configured on the current wiki database (or which contains a column which does not
     * use them).
     * <p>
     * In addition to the {@code CONVERT TO CHARACTER SET} of each of those tables, the statements:
     * <ul>
     * <li>drop the foreign keys involving their string columns, since MySQL and MariaDB refuse to convert a column used
     * in a foreign key</li>
     * <li>drop their indexes which would exceed the maximum key length with the new character set (typically custom
     * indexes, since the ones of the Hibernate mapping are designed to fit), since they would make the conversion
     * fail; a warning gives the definition of each of them, so that it can be re-created with a shorter prefix</li>
     * </ul>
     * <p>
     * <strong>WARNING: the dropped foreign keys are only re-created by the Hibernate schema update, so XWiki must be
     * restarted after executing those statements (unless they are executed right before the schema update, as done by
     * {@link #repairDatabase(Metadata)}).</strong>
     *
     * @param metadata the configuration of the database, used to know which tables to convert
     * @param session the session in which to execute the queries
     * @return the statements to execute in that order, empty if all the tables already use the database collation
     * @since 18.10.0RC1
     */
    public List<String> getConvertToDatabaseCollationStatements(Metadata metadata, Session session)
    {
        String databaseName = getDatabaseFromWikiName();

        Object[] databaseDefaults = getDatabaseDefaults(databaseName, session);
        if (databaseDefaults == null) {
            return List.of();
        }

        String characterSet = (String) databaseDefaults[0];
        String collation = (String) databaseDefaults[1];

        Set<String> tables = getTablesToConvert(metadata, databaseName, collation, session);
        if (tables.isEmpty()) {
            return List.of();
        }

        this.logger.info("Tables {} of database [{}] need to be converted to character set [{}] and collation [{}]",
            tables, databaseName, characterSet, collation);

        List<String> statements = new ArrayList<>();

        // Drop the foreign keys involving string columns of the tables to convert since MySQL/MariaDB refuse to convert
        // those columns
        addDropForeignKeyStatements(databaseName, tables, session, statements);

        // Drop the indexes which would exceed the maximum key length with the new character set
        addDropIndexStatements(databaseName, tables, getMaxCharacterLength(characterSet, session), session,
            statements);

        // Convert the tables
        tables.stream().map(tableName -> ALTER_TABLE + getQualifiedTableName(tableName) + " CONVERT TO CHARACTER SET "
            + characterSet + " COLLATE " + collation).forEach(statements::add);

        return statements;
    }

    private void addDropForeignKeyStatements(String databaseName, Set<String> tables, Session session,
        List<String> statements)
    {
        for (Object[] foreignKey : getStringForeignKeys(databaseName, session)) {
            String tableName = (String) foreignKey[0];

            if (tables.contains(tableName) || tables.contains(foreignKey[2])) {
                statements.add(ALTER_TABLE + getQualifiedTableName(tableName) + " DROP FOREIGN KEY "
                    + quote((String) foreignKey[1]));
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
                    index.name, index.length, MAX_KEY_LENGTH, index.getDefinition());

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
        query.setParameter("characterSet", characterSet);

        return query.uniqueResult().intValue();
    }

    private Set<String> getTablesToConvert(Metadata metadata, String databaseName, String collation, Session session)
    {
        NativeQuery<String> query = session.createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
            + "WHERE TABLE_SCHEMA = :schema AND TABLE_TYPE = 'BASE TABLE' AND (TABLE_COLLATION <> :collation "
            + "OR TABLE_NAME IN (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS "
            + "WHERE TABLE_SCHEMA = :schema AND COLLATION_NAME <> :collation))");
        query.setParameter(SCHEMA_PARAMETER, databaseName);
        query.setParameter("collation", collation);

        // Only touch the tables of the Hibernate mapping
        Set<String> mappedTables = metadata.collectTableMappings().stream()
            .map(table -> getTableName(table).toLowerCase()).collect(Collectors.toSet());

        return query.list().stream().filter(tableName -> mappedTables.contains(tableName.toLowerCase()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<Object[]> getStringForeignKeys(String databaseName, Session session)
    {
        NativeQuery<Object[]> query = session.createNativeQuery(
            "SELECT DISTINCT k.TABLE_NAME, k.CONSTRAINT_NAME, k.REFERENCED_TABLE_NAME "
                + "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k JOIN INFORMATION_SCHEMA.COLUMNS c "
                + "ON c.TABLE_SCHEMA = k.TABLE_SCHEMA AND c.TABLE_NAME = k.TABLE_NAME "
                + "AND c.COLUMN_NAME = k.COLUMN_NAME "
                + "WHERE k.TABLE_SCHEMA = :schema AND k.REFERENCED_TABLE_NAME IS NOT NULL "
                + "AND c.COLLATION_NAME IS NOT NULL");
        query.setParameter(SCHEMA_PARAMETER, databaseName);

        return query.list();
    }

    private Collection<Index> getIndexes(String databaseName, Set<String> tables, int maxCharacterLength,
        Session session)
    {
        NativeQuery<Object[]> query = session.createNativeQuery(
            "SELECT s.TABLE_NAME, s.INDEX_NAME, s.COLUMN_NAME, s.SUB_PART, c.DATA_TYPE, c.COLLATION_NAME, "
                + "c.CHARACTER_MAXIMUM_LENGTH, c.CHARACTER_OCTET_LENGTH, c.NUMERIC_PRECISION, c.DATETIME_PRECISION, "
                + "s.NON_UNIQUE "
                + "FROM INFORMATION_SCHEMA.STATISTICS s JOIN INFORMATION_SCHEMA.COLUMNS c "
                + "ON c.TABLE_SCHEMA = s.TABLE_SCHEMA AND c.TABLE_NAME = s.TABLE_NAME "
                + "AND c.COLUMN_NAME = s.COLUMN_NAME "
                + "WHERE s.TABLE_SCHEMA = :schema AND s.INDEX_NAME <> 'PRIMARY' "
                + "ORDER BY s.TABLE_NAME, s.INDEX_NAME, s.SEQ_IN_INDEX");
        query.setParameter(SCHEMA_PARAMETER, databaseName);

        Map<List<String>, Index> indexes = new LinkedHashMap<>();
        for (Object[] part : query.list()) {
            String tableName = (String) part[0];

            if (tables.contains(tableName)) {
                Index index = indexes.computeIfAbsent(List.of(tableName, (String) part[1]),
                    key -> new Index(key.get(0), key.get(1), ((Number) part[10]).intValue() == 0));

                index.columns.add(part[3] != null ? part[2] + "(" + part[3] + ')' : (String) part[2]);
                index.length += getKeyPartLength(part, maxCharacterLength);
            }
        }

        return indexes.values();
    }

    /**
     * @param part the index part (sub part, type, collation, character length, octet length, numeric precision and
     *            datetime precision start at index 3)
     * @param maxCharacterLength the maximum number of bytes of a character in the new character set
     * @return the number of bytes taken by the part in the index key, once the table is converted
     */
    private long getKeyPartLength(Object[] part, int maxCharacterLength)
    {
        Number subPart = (Number) part[3];
        String type = (String) part[4];

        // Non binary strings (the sub part and the maximum length are expressed in characters)
        if (part[5] != null) {
            long characters = subPart != null ? subPart.longValue() : ((Number) part[6]).longValue();

            return characters * maxCharacterLength;
        }

        // Binary strings (the sub part and the maximum length are expressed in bytes)
        if (part[7] != null) {
            return subPart != null ? subPart.longValue() : ((Number) part[7]).longValue();
        }

        if ("bit".equals(type)) {
            return (((Number) part[8]).longValue() + 7) / 8;
        }

        long length = FIXED_TYPE_LENGTHS.getOrDefault(type, DEFAULT_TYPE_LENGTH);

        if (FRACTIONAL_TYPES.contains(type) && part[9] != null) {
            length += (((Number) part[9]).longValue() + 1) / 2;
        }

        return length;
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

        String getDefinition()
        {
            return (this.unique ? "CREATE UNIQUE INDEX " : "CREATE INDEX ") + this.name + " ON " + this.tableName
                + " (" + String.join(", ", this.columns) + ")";
        }
    }
}
