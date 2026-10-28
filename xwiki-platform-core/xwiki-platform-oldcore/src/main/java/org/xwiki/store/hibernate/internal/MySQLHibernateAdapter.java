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
import java.util.LinkedHashSet;
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
     * The xwiki.properties property controlling the conversion of the tables to the database character set (with a
     * binary collation) at startup.
     */
    private static final String PROPERTY_FIX_TABLE_ENCODING = "store.hibernate.mysql.fixTableEncoding";

    private static final String SCHEMA_PARAMETER = "schema";

    private static final String FROM_BASE_TABLES =
        "FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = :schema AND TABLE_TYPE = 'BASE TABLE' ";

    private static final String ALTER_TABLE = "ALTER TABLE ";

    private static final String ROW_FORMAT = " ROW_FORMAT=";

    private static final String CONVERT_CHARACTER_SET = " CHARACTER SET ";

    private static final String COLLATE = " COLLATE ";

    private static final String CHARACTER_SET_PARAMETER = "characterSet";

    /**
     * The suffix of the binary collations (e.g. utf8mb4_bin), the only ones supported by XWiki.
     */
    private static final String BINARY_COLLATION_SUFFIX = "_bin";

    /**
     * The maximum number of bytes of a character in the character sets able to store any character (e.g. utf8mb4).
     */
    private static final int UTF8MB4_MAX_CHARACTER_LENGTH = 4;

    private static final String UTF8MB4 = "utf8mb4";

    private static final String UTF8MB4_BIN = UTF8MB4 + BINARY_COLLATION_SUFFIX;

    /**
     * The character sets kept when configured on the database ({@code utf8} is the old name of {@code utf8mb3}).
     */
    private static final Set<String> SUPPORTED_CHARACTER_SETS = Set.of(UTF8MB4, "utf8mb3", "utf8");

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

    /**
     * The only storage engine whose tables are converted.
     */
    private static final String INNODB_ENGINE = "InnoDB";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @Inject
    private HibernateStore store;

    @Override
    public void repairDatabase(Metadata metadata) throws HibernateStoreException
    {
        if (Boolean.FALSE.equals(this.xwikiProperties.getProperty(PROPERTY_FIX_TABLE_ENCODING, Boolean.class))) {
            this.logger.debug("Skipping the conversion of the tables to the database collation since [{}] is disabled",
                PROPERTY_FIX_TABLE_ENCODING);

            return;
        }

        try (Session session = getSessionFactory().openSession()) {
            convertToDatabaseCollation(metadata, session);
        } catch (Exception e) {
            throw new HibernateStoreException(String.format("Failed to convert the tables to the database character"
                + " set and a binary collation. The conversion at startup can be disabled by setting [%s] to false in"
                + " xwiki.properties.", PROPERTY_FIX_TABLE_ENCODING), e);
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
            return quote(tableName);
        }

        return quote(database) + '.' + quote(tableName);
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

        // Unlike the InnoDB system tables (INNODB_TABLES on MySQL 8, INNODB_SYS_TABLES on MariaDB), TABLES is the same
        // on MySQL and MariaDB, and it does not encode the special characters of the database and table names
        // (e.g. "my.wiki" is "my@002ewiki" in the InnoDB system tables)
        NativeQuery<Object[]> query = session.createNativeQuery(
            "SELECT TABLE_NAME, ROW_FORMAT " + FROM_BASE_TABLES + "AND ENGINE = '" + INNODB_ENGINE + "'");
        query.setParameter(SCHEMA_PARAMETER, currentDatabase);

        List<Object[]> results = query.list();

        Map<String, String> rowFormats = HashMap.newHashMap(results.size());
        for (Object[] entry : results) {
            rowFormats.put((String) entry[0], (String) entry[1]);
        }

        return rowFormats;
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
     * Convert the current wiki database and its tables to the expected character set and collation, and re-create the
     * foreign keys dropped for the conversion.
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
            PROPERTY_FIX_TABLE_ENCODING);

        long conversionStart = System.currentTimeMillis();
        try {
            executeStatements(statements, session);
        } catch (RuntimeException e) {
            // Re-create the dropped foreign keys even when the conversion fails: they don't exist anymore when the
            // conversion is resumed at the next startup, so the ones which are not part of the Hibernate mapping
            // would be lost otherwise
            try {
                recreateForeignKeys(conversion.foreignKeyStatements, session);
            } catch (RuntimeException recreateException) {
                e.addSuppressed(recreateException);
            }

            throw e;
        }

        recreateForeignKeys(conversion.foreignKeyStatements, session);

        this.logger.info("Tables of database [{}] converted in [{}] ms", getDatabaseFromWikiName(),
            System.currentTimeMillis() - conversionStart);
    }

    private void executeStatements(List<String> statements, Session session)
    {
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
    }

    /**
     * Re-create the foreign keys dropped for the conversion, without the foreign key checks: this way they are added
     * without rebuilding the tables (the conversion does not modify the data, so it does not need to be checked
     * again). Note that the way the values are compared changes when going from a case insensitive collation to a
     * binary one: a row which only matched its referenced row case insensitively (e.g. {@code abc} referencing
     * {@code ABC}) does not match it anymore after the conversion, and it is not reported since the foreign keys are
     * not checked. A failure is only reported with a warning since the Hibernate schema update, which runs right after,
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
     * expected character set and collation (or which contains a column which does not use them):
     * <ul>
     * <li>the character set configured on the current wiki database when it's {@code utf8mb4} or {@code utf8mb3}
     * (unless a standard table needs {@code utf8mb4}, see below), and {@code utf8mb4} otherwise (e.g. for
     * {@code latin1}, which cannot hold most of the translations of the standard XWiki flavor)</li>
     * <li>XWiki requires a binary collation, so the collation configured on the database when it's a binary collation
     * of that character set (e.g. {@code utf8mb4_bin}), and the binary collation of that character set otherwise (e.g.
     * {@code utf8mb4_bin} for {@code utf8mb4_0900_ai_ci})</li>
     * </ul>
     * The default character set and collation of the database are changed too when they don't match, so that the
     * tables created later use the expected ones.
     * <p>
     * The tables of the custom mappings of the XWiki classes, and the tables which are not part of the Hibernate
     * mapping, are only converted when they are linked by a foreign key on string columns to a table being converted
     * (otherwise that foreign key could not be re-created), since their structure is controlled by the users.
     * <p>
     * In addition to the {@code CONVERT TO CHARACTER SET} of each of those tables (which also switches a legacy or
     * unexpected row format to the expected one, so that the table is rebuilt only once), the statements:
     * <ul>
     * <li>drop the foreign keys involving their string columns, since MySQL and MariaDB refuse to convert a column used
     * in a foreign key</li>
     * <li>drop their indexes which would exceed the maximum key length with the new character set (typically custom
     * indexes, since the ones of the Hibernate mapping are designed to fit), since they would make the conversion
     * fail; a warning gives the definition of each of them, so that it can be re-created with a shorter prefix</li>
     * <li>drop the foreign keys which need one of those indexes, since MySQL and MariaDB refuse to drop the index
     * otherwise</li>
     * </ul>
     * <p>
     * Nothing is lost by the conversion: when a standard table can contain characters not supported by the database
     * character set (e.g. a {@code utf8mb4} table in a {@code utf8mb3} database), the database and all its tables are
     * converted to {@code utf8mb4} instead, since having some standard tables in {@code utf8mb3} and others in
     * {@code utf8mb4} does not make sense. A table linked by a foreign key on string columns to a table of another
     * database is not converted, since that foreign key could not be re-created. The tables which don't use the InnoDB
     * storage engine are not converted either, since the maximum key length and the row formats taken into account are
     * the InnoDB ones.
     * <p>
     * <strong>WARNING: the dropped foreign keys are not re-created by those statements (unlike
     * {@link #convertToDatabaseCollation(Metadata, Session)}), only by the Hibernate schema update when XWiki is
     * restarted.</strong>
     *
     * @param metadata the configuration of the database, used to know which tables to convert
     * @param session the session in which to execute the queries
     * @return the statements to execute in that order, empty if the database and all the tables already use the
     *         expected collation
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

        Target target = getTarget(metadata, databaseName, (String) databaseDefaults[0], (String) databaseDefaults[1],
            session, conversion);
        Map<String, Set<String>> tables = target.tables;
        if (tables.isEmpty()) {
            return conversion;
        }

        List<ForeignKey> foreignKeys = getForeignKeys(databaseName, session);

        addLinkedTables(databaseName, target.candidateTables, tables, foreignKeys);

        removeCrossDatabaseTables(databaseName, tables, foreignKeys);
        if (tables.isEmpty()) {
            return conversion;
        }

        this.logger.info("Tables {} of database [{}] need to be converted to character set [{}] and collation [{}]",
            tables.keySet(), databaseName, target.characterSet, target.collation);

        // The indexes which would exceed the maximum key length with the new character set
        List<Index> tooLongIndexes =
            getTooLongIndexes(databaseName, tables.keySet(), target.maxCharacterLength, session);

        // Drop the foreign keys involving string columns of the tables to convert since MySQL/MariaDB refuse to convert
        // those columns, and the ones which need an index to drop (and remember how to re-create them)
        addForeignKeyStatements(getForeignKeysToDrop(databaseName, tables.keySet(), tooLongIndexes, foreignKeys),
            conversion);

        // Drop the indexes which would exceed the maximum key length with the new character set
        for (Index index : tooLongIndexes) {
            conversion.statements.add(
                ALTER_TABLE + getQualifiedTableName(index.tableName) + " DROP INDEX " + quote(index.name));
        }

        addConvertStatements(metadata, tables.keySet(), target.characterSet, target.collation, session, conversion);

        return conversion;
    }

    /**
     * Resolve the expected character set and collation, the standard tables which don't use them, and fix the database
     * default character set and collation.
     */
    private Target getTarget(Metadata metadata, String databaseName, String databaseCharacterSet,
        String databaseCollation, Session session, Conversion conversion)
    {
        // The character set configured on the database is kept when it's utf8mb4 or utf8mb3, but the other ones
        // (e.g. latin1) cannot hold most of the translations of the standard XWiki flavor, so they are replaced with
        // utf8mb4
        String characterSet = SUPPORTED_CHARACTER_SETS.contains(databaseCharacterSet.toLowerCase())
            ? databaseCharacterSet : UTF8MB4;
        // XWiki requires a binary collation, whatever the database is configured with. Going from a NO PAD collation
        // (e.g. utf8mb4_0900_ai_ci, the MySQL 8 default) to a PAD SPACE one (e.g. utf8mb4_bin) makes the unique keys
        // which only differ by trailing spaces collide: that case is not supported, the conversion of such a table
        // fails and the duplicate values have to be fixed manually.
        String collation = isBinary(databaseCollation) && isCollationOf(databaseCollation, characterSet)
            ? databaseCollation : characterSet + BINARY_COLLATION_SUFFIX;

        Target target = new Target(characterSet, collation, getMaxCharacterLength(characterSet, session));
        Set<String> nonInnoDBTables = new TreeSet<>();
        target.setTables(metadata, getTablesToConvert(metadata, databaseName, collation, nonInnoDBTables, session));

        // Having some standard tables in utf8mb3 and others in utf8mb4 does not make sense, so when one of them can
        // contain characters not supported by the database character set (e.g. a utf8mb4 table in a utf8mb3
        // database), the database and all its tables are converted to utf8mb4 instead, so that nothing is lost
        if (target.maxCharacterLength < UTF8MB4_MAX_CHARACTER_LENGTH) {
            Set<String> wideTables =
                getWideTables(databaseName, target.maxCharacterLength, target.tables.keySet(), session);
            if (!wideTables.isEmpty()) {
                this.logger.info("Tables {} of database [{}] can contain characters not supported by its character set"
                    + " [{}], so the database and all its tables will be converted to [{}] instead", wideTables,
                    databaseName, characterSet, UTF8MB4);

                target = new Target(UTF8MB4, UTF8MB4_BIN, UTF8MB4_MAX_CHARACTER_LENGTH);
                nonInnoDBTables.clear();
                target.setTables(metadata,
                    getTablesToConvert(metadata, databaseName, UTF8MB4_BIN, nonInnoDBTables, session));
            }
        }

        if (!nonInnoDBTables.isEmpty()) {
            this.logger.warn("Tables {} of database [{}] don't use collation [{}] but won't be converted since they"
                + " don't use the InnoDB storage engine. Switching them to InnoDB is recommended, they will then be"
                + " converted at the next startup.", nonInnoDBTables, databaseName, target.collation);
        }

        addDatabaseStatement(databaseName, databaseCharacterSet, databaseCollation, target.characterSet,
            target.collation, conversion);

        return target;
    }

    private boolean isCollationOf(String collation, String characterSet)
    {
        return collation.toLowerCase().startsWith(characterSet.toLowerCase() + '_');
    }

    /**
     * Fix the default character set and collation of the database so that the tables created later (starting with the
     * ones created by the Hibernate schema update right after) use the expected ones.
     */
    private void addDatabaseStatement(String databaseName, String databaseCharacterSet, String databaseCollation,
        String characterSet, String collation, Conversion conversion)
    {
        if (!databaseCharacterSet.equalsIgnoreCase(characterSet) || !databaseCollation.equalsIgnoreCase(collation)) {
            this.logger.info("The default character set and collation of database [{}] will be changed from [{}] and"
                + " [{}] to [{}] and [{}]", databaseName, databaseCharacterSet, databaseCollation, characterSet,
                collation);

            conversion.statements.add("ALTER DATABASE " + quote(databaseName) + CONVERT_CHARACTER_SET + characterSet
                + COLLATE + collation);
        }
    }

    private void addConvertStatements(Metadata metadata, Set<String> tables, String characterSet, String collation,
        Session session, Conversion conversion)
    {
        Map<String, String> rowFormats = getRowFormats(session);
        Map<String, Boolean> entityTables = getEntityTables(metadata);
        for (String tableName : tables) {
            StringBuilder statement = new StringBuilder(ALTER_TABLE).append(getQualifiedTableName(tableName));

            // Switching the row format in the same statement rebuilds the table only once, and makes sure the
            // conversion gets the maximum key length of the DYNAMIC and COMPRESSED row formats
            String rowFormat =
                getExpectedRowFormat(rowFormats.get(tableName), entityTables.get(tableName.toLowerCase()));
            if (rowFormat != null) {
                statement.append(ROW_FORMAT).append(rowFormat).append(',');
            }

            conversion.statements.add(statement.append(" CONVERT TO").append(CONVERT_CHARACTER_SET)
                .append(characterSet).append(COLLATE).append(collation).toString());
        }
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
     * Add to the tables to convert the other tables which need to be converted and are linked by a foreign key on
     * string columns to a table to convert, since that foreign key could not be re-created otherwise (the columns would
     * not use the same character set and collation anymore).
     */
    private void addLinkedTables(String databaseName, Map<String, Set<String>> candidateTables,
        Map<String, Set<String>> tables, List<ForeignKey> foreignKeys)
    {
        Set<String> linkedTables = new TreeSet<>();

        boolean changed = true;
        while (changed) {
            changed = false;
            for (ForeignKey foreignKey : foreignKeys) {
                if (foreignKey.string && foreignKey.isLocal(databaseName)) {
                    changed |= addLinkedTable(foreignKey.tableName, foreignKey.referencedTableName, candidateTables,
                        tables, linkedTables);
                    changed |= addLinkedTable(foreignKey.referencedTableName, foreignKey.tableName, candidateTables,
                        tables, linkedTables);
                }
            }
        }

        if (!linkedTables.isEmpty()) {
            this.logger.info("Tables {} of database [{}] are not standard tables of the Hibernate mapping, but will be"
                + " converted too since they are linked by a foreign key to a table to convert", linkedTables,
                databaseName);
        }
    }

    private boolean addLinkedTable(String tableName, String linkedTableName, Map<String, Set<String>> candidateTables,
        Map<String, Set<String>> tables, Set<String> linkedTables)
    {
        if (tables.containsKey(tableName) && !tables.containsKey(linkedTableName)
            && candidateTables.containsKey(linkedTableName)) {
            tables.put(linkedTableName, candidateTables.get(linkedTableName));
            linkedTables.add(linkedTableName);

            return true;
        }

        return false;
    }

    /**
     * Remove from the tables to convert the ones linked by a foreign key on string columns to a table of another
     * database, and the ones linked to them by a foreign key on string columns, since that foreign key could not be
     * re-created after the conversion (the columns would not use the same character set and collation anymore).
     */
    private void removeCrossDatabaseTables(String databaseName, Map<String, Set<String>> tables,
        List<ForeignKey> foreignKeys)
    {
        Set<String> crossDatabaseTables = new TreeSet<>();
        for (ForeignKey foreignKey : foreignKeys) {
            if (foreignKey.string && !foreignKey.isLocal(databaseName)) {
                if (foreignKey.isTable(databaseName, foreignKey.tableSchema, foreignKey.tableName, tables.keySet())) {
                    crossDatabaseTables.add(foreignKey.tableName);
                }
                if (foreignKey.isTable(databaseName, foreignKey.referencedSchema, foreignKey.referencedTableName,
                    tables.keySet())) {
                    crossDatabaseTables.add(foreignKey.referencedTableName);
                }
            }
        }
        addExcludedLinkedTables(databaseName, tables, crossDatabaseTables, foreignKeys);

        if (!crossDatabaseTables.isEmpty()) {
            this.logger.warn("Tables {} of database [{}] won't be converted since they (or a table linked to them by a"
                + " foreign key) are linked by a foreign key to a table of another database, which could not be"
                + " re-created after the conversion", crossDatabaseTables, databaseName);

            tables.keySet().removeAll(crossDatabaseTables);
        }
    }

    /**
     * @return the passed tables which can contain characters not supported by a character set using at most the passed
     *         number of bytes per character
     */
    private Set<String> getWideTables(String databaseName, int maxCharacterLength, Set<String> tables,
        Session session)
    {
        NativeQuery<String> query = session.createNativeQuery("SELECT DISTINCT c.TABLE_NAME "
            + "FROM INFORMATION_SCHEMA.COLUMNS c JOIN INFORMATION_SCHEMA.CHARACTER_SETS s "
            + "ON s.CHARACTER_SET_NAME = c.CHARACTER_SET_NAME "
            + "WHERE c.TABLE_SCHEMA = :schema AND s.MAXLEN > :maxLength");
        query.setParameter(SCHEMA_PARAMETER, databaseName);
        query.setParameter("maxLength", maxCharacterLength);

        return query.list().stream().filter(tables::contains).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Add to the passed tables the tables to convert linked to them (directly or not) by a foreign key on string
     * columns.
     */
    private void addExcludedLinkedTables(String databaseName, Map<String, Set<String>> tables,
        Set<String> excludedTables, List<ForeignKey> foreignKeys)
    {
        boolean changed = !excludedTables.isEmpty();
        while (changed) {
            changed = false;
            for (ForeignKey foreignKey : foreignKeys) {
                if (foreignKey.string && foreignKey.isLocal(databaseName)) {
                    changed |= addExcludedTable(foreignKey.tableName, foreignKey.referencedTableName, tables,
                        excludedTables);
                    changed |= addExcludedTable(foreignKey.referencedTableName, foreignKey.tableName, tables,
                        excludedTables);
                }
            }
        }
    }

    private boolean addExcludedTable(String tableName, String linkedTableName, Map<String, Set<String>> tables,
        Set<String> excludedTables)
    {
        return excludedTables.contains(tableName) && tables.containsKey(linkedTableName)
            && excludedTables.add(linkedTableName);
    }

    /**
     * @return the foreign keys involving string columns of the tables to convert, and the foreign keys which need one
     *         of the indexes to drop
     */
    private Set<ForeignKey> getForeignKeysToDrop(String databaseName, Set<String> tables, List<Index> droppedIndexes,
        List<ForeignKey> foreignKeys)
    {
        Set<ForeignKey> foreignKeysToDrop = new LinkedHashSet<>();
        for (ForeignKey foreignKey : foreignKeys) {
            boolean converted = foreignKey.string
                && (foreignKey.isTable(databaseName, foreignKey.tableSchema, foreignKey.tableName, tables)
                    || foreignKey.isTable(databaseName, foreignKey.referencedSchema, foreignKey.referencedTableName,
                        tables));
            if (converted || droppedIndexes.stream().anyMatch(index -> foreignKey.needs(databaseName, index))) {
                foreignKeysToDrop.add(foreignKey);
            }
        }

        return foreignKeysToDrop;
    }

    private void addForeignKeyStatements(Set<ForeignKey> foreignKeys, Conversion conversion)
    {
        for (ForeignKey foreignKey : foreignKeys) {
            String alterTable = ALTER_TABLE + quote(foreignKey.tableSchema) + '.' + quote(foreignKey.tableName);
            String name = quote(foreignKey.name);
            String addStatement = alterTable + " ADD CONSTRAINT " + name + " FOREIGN KEY"
                + toQuotedColumnList(foreignKey.columns) + " REFERENCES " + quote(foreignKey.referencedSchema) + '.'
                + quote(foreignKey.referencedTableName) + toQuotedColumnList(foreignKey.referencedColumns)
                + " ON UPDATE " + foreignKey.updateRule + " ON DELETE " + foreignKey.deleteRule;

            // Keep a trace of the definition, in case the foreign key cannot be re-created automatically (e.g.
            // XWiki is stopped in the middle of the conversion)
            this.logger.info("Foreign key [{}] of table [{}] will be dropped for the conversion. It can be"
                + " re-created using [{}].", foreignKey.name, foreignKey.tableName, addStatement);

            conversion.statements.add(alterTable + " DROP FOREIGN KEY " + name);
            conversion.foreignKeyStatements.add(addStatement);
        }
    }

    private List<Index> getTooLongIndexes(String databaseName, Set<String> tables, int maxCharacterLength,
        Session session)
    {
        List<Index> tooLongIndexes = new ArrayList<>();
        for (Index index : getIndexes(databaseName, tables, maxCharacterLength, session)) {
            if (index.length > MAX_KEY_LENGTH && !(index.unique && isLongUniqueIndexSupported())) {
                if (this.logger.isWarnEnabled()) {
                    this.logger.warn("Index [{}] (key length of [{}] bytes after the conversion) exceeds the maximum"
                        + " key length of [{}] bytes and will be dropped. It can be re-created with a shorter prefix"
                        + " using [{}].", index.name, index.length, MAX_KEY_LENGTH, getIndexDefinition(index));
                }

                tooLongIndexes.add(index);
            }
        }

        return tooLongIndexes;
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

    private String toQuotedColumnList(List<String> columns)
    {
        return toColumnList(columns.stream().map(this::quote).toList());
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

    /**
     * @param nonInnoDBTables filled with the standard tables which don't use the expected collation, but cannot be
     *            converted since they don't use the InnoDB storage engine
     * @return the collations (other than the expected one) used by each InnoDB table which does not use the expected
     *         collation (or which contains a column which does not use it)
     */
    private Map<String, Set<String>> getTablesToConvert(Metadata metadata, String databaseName, String collation,
        Set<String> nonInnoDBTables, Session session)
    {
        Set<String> standardTables = getStandardTables(metadata);

        Map<String, Set<String>> tables = new TreeMap<>();
        for (Object[] tableCollation : queryTableCollations(databaseName, collation, session)) {
            String tableName = (String) tableCollation[0];

            // The maximum key length and the row formats taken into account by the conversion are the InnoDB ones
            // (e.g. a MyISAM index key cannot exceed 1000 bytes)
            if (INNODB_ENGINE.equalsIgnoreCase((String) tableCollation[2])) {
                tables.computeIfAbsent(tableName, key -> new HashSet<>()).add((String) tableCollation[1]);
            } else if (standardTables.contains(tableName.toLowerCase())) {
                nonInnoDBTables.add(tableName);
            }
        }

        return tables;
    }

    /**
     * @return the table, collation and engine of each table (or column) of the database which does not use the
     *         passed collation
     */
    private List<Object[]> queryTableCollations(String databaseName, String collation, Session session)
    {
        NativeQuery<Object[]> query = session.createNativeQuery("SELECT TABLE_NAME, TABLE_COLLATION, ENGINE "
            + FROM_BASE_TABLES + "AND TABLE_COLLATION <> :collation "
            + "UNION SELECT c.TABLE_NAME, c.COLLATION_NAME, t.ENGINE "
            + "FROM INFORMATION_SCHEMA.COLUMNS c JOIN INFORMATION_SCHEMA.TABLES t "
            + "ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME "
            + "WHERE c.TABLE_SCHEMA = :schema AND t.TABLE_TYPE = 'BASE TABLE' AND c.COLLATION_NAME <> :collation");
        query.setParameter(SCHEMA_PARAMETER, databaseName);
        query.setParameter("collation", collation);

        return query.list();
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

    /**
     * @return the foreign keys of the tables of the database, and the foreign keys of the other databases referencing
     *         them
     */
    private List<ForeignKey> getForeignKeys(String databaseName, Session session)
    {
        NativeQuery<Object[]> query = session.createNativeQuery(
            "SELECT k.TABLE_SCHEMA, k.TABLE_NAME, k.CONSTRAINT_NAME, k.REFERENCED_TABLE_SCHEMA, "
                + "k.REFERENCED_TABLE_NAME, r.UPDATE_RULE, r.DELETE_RULE, k.COLUMN_NAME, k.REFERENCED_COLUMN_NAME, "
                + "c.COLLATION_NAME "
                + "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS r "
                + "ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA AND r.TABLE_NAME = k.TABLE_NAME "
                + "AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME JOIN INFORMATION_SCHEMA.COLUMNS c "
                + "ON c.TABLE_SCHEMA = k.TABLE_SCHEMA AND c.TABLE_NAME = k.TABLE_NAME "
                + "AND c.COLUMN_NAME = k.COLUMN_NAME "
                + "WHERE (k.TABLE_SCHEMA = :schema OR k.REFERENCED_TABLE_SCHEMA = :schema) "
                + "AND k.REFERENCED_TABLE_NAME IS NOT NULL "
                + "ORDER BY k.TABLE_SCHEMA, k.TABLE_NAME, k.CONSTRAINT_NAME, k.ORDINAL_POSITION");
        query.setParameter(SCHEMA_PARAMETER, databaseName);

        Map<List<String>, ForeignKey> foreignKeys = new LinkedHashMap<>();
        for (Object[] part : query.list()) {
            ForeignKey foreignKey = foreignKeys.computeIfAbsent(
                List.of((String) part[0], (String) part[1], (String) part[2]), key -> new ForeignKey(part));

            foreignKey.columns.add((String) part[7]);
            foreignKey.referencedColumns.add((String) part[8]);
            // Only the foreign keys involving string columns prevent the conversion
            foreignKey.string |= part[9] != null;
        }

        return List.copyOf(foreignKeys.values());
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

                String column = (String) part[2];
                index.rawColumns.add(column);
                index.columns.add(part[3] != null ? quote(column) + "(" + part[3] + ')' : quote(column));
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

    /**
     * The expected character set and collation, and the tables which don't use them.
     */
    private final class Target
    {
        private final String characterSet;

        private final String collation;

        private final int maxCharacterLength;

        /**
         * The collations (other than the expected one) used by each InnoDB table which does not use the expected
         * collation.
         */
        private Map<String, Set<String>> candidateTables;

        /**
         * The standard tables to convert, with the collations (other than the expected one) they use.
         */
        private Map<String, Set<String>> tables;

        Target(String characterSet, String collation, int maxCharacterLength)
        {
            this.characterSet = characterSet;
            this.collation = collation;
            this.maxCharacterLength = maxCharacterLength;
        }

        void setTables(Metadata metadata, Map<String, Set<String>> candidateTables)
        {
            this.candidateTables = candidateTables;

            Set<String> standardTables = getStandardTables(metadata);
            this.tables = new TreeMap<>();
            candidateTables.forEach((tableName, collations) -> {
                if (standardTables.contains(tableName.toLowerCase())) {
                    this.tables.put(tableName, collations);
                }
            });
        }
    }

    private static final class Conversion
    {
        private final List<String> statements = new ArrayList<>();

        private final List<String> foreignKeyStatements = new ArrayList<>();
    }

    private static final class ForeignKey
    {
        private final String tableSchema;

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
         * @param part the first part of the foreign key (schema, table, name, referenced schema, referenced table,
         *            update rule and delete rule)
         */
        ForeignKey(Object[] part)
        {
            this.tableSchema = (String) part[0];
            this.tableName = (String) part[1];
            this.name = (String) part[2];
            this.referencedSchema = (String) part[3];
            this.referencedTableName = (String) part[4];
            this.updateRule = (String) part[5];
            this.deleteRule = (String) part[6];
        }

        /**
         * @return true if the table and the referenced table are both in the database
         */
        boolean isLocal(String databaseName)
        {
            return databaseName.equalsIgnoreCase(this.tableSchema)
                && databaseName.equalsIgnoreCase(this.referencedSchema);
        }

        /**
         * @return true if the passed table (one of the two sides of the foreign key) is one of the tables of the
         *         database
         */
        boolean isTable(String databaseName, String schema, String table, Collection<String> tables)
        {
            return databaseName.equalsIgnoreCase(schema) && tables.contains(table);
        }

        /**
         * @return true if the foreign key needs the index (its columns are the first columns of the index), which
         *         prevents dropping the index
         */
        boolean needs(String databaseName, Index index)
        {
            return (isTable(databaseName, this.tableSchema, this.tableName, List.of(index.tableName))
                && isPrefix(this.columns, index.rawColumns))
                || (isTable(databaseName, this.referencedSchema, this.referencedTableName, List.of(index.tableName))
                    && isPrefix(this.referencedColumns, index.rawColumns));
        }

        private static boolean isPrefix(List<String> columns, List<String> indexColumns)
        {
            if (columns.size() > indexColumns.size()) {
                return false;
            }

            for (int i = 0; i < columns.size(); ++i) {
                // Column names are case insensitive in MySQL and MariaDB
                if (!columns.get(i).equalsIgnoreCase(indexColumns.get(i))) {
                    return false;
                }
            }

            return true;
        }
    }

    private static final class Index
    {
        private final String tableName;

        private final String name;

        private final boolean unique;

        private final List<String> rawColumns = new ArrayList<>();

        /**
         * The quoted columns, with their prefix length if any.
         */
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
