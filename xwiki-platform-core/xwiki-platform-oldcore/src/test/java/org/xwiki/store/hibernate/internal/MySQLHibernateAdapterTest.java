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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.jdbc.Work;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.store.hibernate.HibernateAdapter;
import org.xwiki.store.hibernate.HibernateStoreException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link MySQLHibernateAdapter}.
 *
 * @version $Id$
 */
@ComponentTest
class MySQLHibernateAdapterTest
{
    private static final String CONVERT = " CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin";

    private static final String INNODB = "InnoDB";

    private static final String FIX_TABLE_ENCODING_PROPERTY = "store.hibernate.mysql.fixTableEncoding";

    @InjectMockComponents
    private MySQLHibernateAdapter adapter;

    @InjectMockComponents
    private MariaDBHibernateAdapter mariaDBAdapter;

    @MockComponent
    private WikiDescriptorManager wikis;

    @MockComponent
    private HibernateStore hibernateStore;

    @MockComponent
    private HibernateConfiguration hibernateConfiguration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private Metadata metadata;

    private Session session;

    private NativeQuery<Object[]> defaultsQuery;

    private NativeQuery<Object[]> tablesQuery;

    private NativeQuery<Number> maxLengthQuery;

    private NativeQuery<Object[]> foreignKeysQuery;

    private NativeQuery<?> alterStatement;

    @BeforeEach
    void beforeEach()
    {
        when(this.hibernateStore.getDialect()).thenReturn(new MySQLDialect());
        when(this.hibernateConfiguration.getDBPrefix()).thenReturn("");
        when(this.wikis.getMainWikiId()).thenReturn("xwiki");
    }

    @Test
    void getAlterRowFormatStringIsQualifiedWithTheCurrentWikiDatabase() throws Exception
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("subwiki");

        // The statement must name the database explicitly, otherwise it is executed against whichever database the
        // pooled JDBC connection happens to be on.
        assertEquals("ALTER TABLE `subwiki`.`xwikircs` ROW_FORMAT=Dynamic",
            this.adapter.getAlterRowFormatString("xwikircs", false, Map.of("xwikircs", "Compact"), null));
        assertEquals("ALTER TABLE `subwiki`.`xwikircs` ROW_FORMAT=Compressed",
            this.adapter.getAlterRowFormatString("xwikircs", true, Map.of(), null));
    }

    @Test
    void getAlterRowFormatStringWhenNoCurrentWiki() throws Exception
    {
        when(this.wikis.getCurrentWikiId()).thenReturn(null);

        assertEquals("ALTER TABLE `xwikircs` ROW_FORMAT=Dynamic",
            this.adapter.getAlterRowFormatString("xwikircs", false, Map.of(), null));
    }

    private PersistentClass mockEntity(String entityName, String tableName)
    {
        PersistentClass entity = mock(PersistentClass.class);
        when(entity.getEntityName()).thenReturn(entityName);
        when(entity.getTable()).thenReturn(new Table(tableName));

        return entity;
    }

    private void mockCollationQueries()
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("xwiki");

        Configuration configuration = mock(Configuration.class);
        when(this.hibernateStore.getConfiguration()).thenReturn(configuration);

        this.metadata = mock(Metadata.class);
        when(this.metadata.collectTableMappings()).thenReturn(List.of(new Table("xwikidoc"),
            new Table("xwikiproperties"), new Table("xwikistrings"), new Table("xwikilistitems"),
            new Table("activitystream_events"), new Table("xwikipreferences"), new Table("xwikicustom_Test_Custom")));
        PersistentClass documentEntity = mockEntity("com.xpn.xwiki.doc.XWikiDocument", "xwikidoc");
        // xwikiproperties is the table of a compressed entity
        PersistentClass propertiesEntity = mockEntity("com.xpn.xwiki.objects.BaseProperty", "xwikiproperties");
        MetaAttribute compressed = mock(MetaAttribute.class);
        when(propertiesEntity.getMetaAttribute(HibernateAdapter.META_ATTRIBUTE_COMPRESSED)).thenReturn(compressed);
        PersistentClass stringsEntity = mockEntity("com.xpn.xwiki.objects.StringProperty", "xwikistrings");
        PersistentClass eventsEntity =
            mockEntity("org.xwiki.eventstream.store.internal.LegacyEvent", "activitystream_events");
        // A static mapping of an XWiki class, part of the standard mapping
        PersistentClass preferencesEntity = mockEntity("XWiki.XWikiPreferences", "xwikipreferences");
        // The custom mapping of an XWiki class
        PersistentClass customEntity = mockEntity("Test.Custom", "xwikicustom_Test_Custom");
        when(this.metadata.getEntityBindings()).thenReturn(List.of(documentEntity, propertiesEntity, stringsEntity,
            eventsEntity, preferencesEntity, customEntity));
        when(this.hibernateStore.toDynamicMappingTableName(anyString()))
            .then(invocation -> "xwikicustom_" + invocation.<String>getArgument(0).replace('.', '_'));

        this.session = mock(Session.class);

        this.defaultsQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INFORMATION_SCHEMA.SCHEMATA"))).thenReturn(this.defaultsQuery);
        when(this.defaultsQuery.list()).thenReturn(List.<Object[]>of(new Object[] { "utf8mb4", "utf8mb4_bin" }));

        this.maxLengthQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INFORMATION_SCHEMA.CHARACTER_SETS")))
            .thenReturn(this.maxLengthQuery);
        when(this.maxLengthQuery.uniqueResult()).thenReturn(4L);

        this.tablesQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INFORMATION_SCHEMA.TABLES"))).thenReturn(this.tablesQuery);
        when(this.tablesQuery.list()).thenReturn(List.of(new Object[] { "xwikistrings", "utf8mb3_general_ci", INNODB },
            new Object[] { "activitystream_events", "utf8mb3_general_ci", INNODB },
            new Object[] { "custom_table", "utf8mb3_general_ci", INNODB },
            new Object[] { "xwikicustom_Test_Custom", "utf8mb3_general_ci", INNODB },
            new Object[] { "xwikipreferences", "utf8mb3_bin", INNODB },
            new Object[] { "xwikilistitems", "utf8mb3_bin", INNODB },
            new Object[] { "xwikiproperties", "utf8mb3_general_ci", INNODB }));

        this.foreignKeysQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INFORMATION_SCHEMA.KEY_COLUMN_USAGE")))
            .thenReturn(this.foreignKeysQuery);
        // schema, table, name, referenced schema, referenced table, update rule, delete rule, column, referenced
        // column, collation
        when(this.foreignKeysQuery.list()).thenReturn(List.of(
            new Object[] { "xwiki", "custom_table", "FKcustom", "xwiki", "xwikidoc", "CASCADE", "NO ACTION", "VALUE",
                "XWD_FULLNAME", "utf8mb3_general_ci" },
            // Not involving string columns
            new Object[] { "xwiki", "xwikilistitems", "FKnumbers", "xwiki", "xwikistrings", "RESTRICT", "RESTRICT",
                "XWL_ID", "XWS_ID", null },
            new Object[] { "xwiki", "xwikistrings", "FKstrings", "xwiki", "xwikiproperties", "RESTRICT", "RESTRICT",
                "XWS_ID", "XWP_ID", null },
            new Object[] { "xwiki", "xwikistrings", "FKstrings", "xwiki", "xwikiproperties", "RESTRICT", "RESTRICT",
                "XWS_NAME", "XWP_NAME", "utf8mb3_general_ci" }));

        // table, index, column, sub part, collation, max length, octet length, non unique
        NativeQuery<Object[]> indexesQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INFORMATION_SCHEMA.STATISTICS"))).thenReturn(indexesQuery);
        when(indexesQuery.list()).thenReturn(List.of(
            // 768 * 4 + 8 = 3080: too long
            new Object[] { "activitystream_events", "ase_page_date", "ase_page", null, "utf8mb3_general_ci", 768L,
                2304L, 1L },
            new Object[] { "activitystream_events", "ase_page_date", "ase_date", null, null, null, null, 1L },
            // 768 * 4 = 3072: fits
            new Object[] { "activitystream_events", "EVENT_PAGE", "ase_page", null, "utf8mb3_general_ci", 768L,
                2304L, 1L },
            // 200 * 4 = 800: fits
            new Object[] { "activitystream_events", "ase_param1", "ase_param1", 200L, "utf8mb3_general_ci",
                4294967295L, 4294967295L, 1L },
            // 3000 + 8 + 8 = 3016: fits
            new Object[] { "xwikistrings", "binary_index", "XWS_BINARY", null, null, 3000L, 3000L, 1L },
            new Object[] { "xwikistrings", "binary_index", "XWS_DATE", null, null, null, null, 1L },
            new Object[] { "xwikistrings", "binary_index", "XWS_ID", null, null, null, null, 1L },
            // 768 * 4 + 8 = 3080: too long
            new Object[] { "xwikistrings", "custom_unique", "XWS_VALUE", null, "utf8mb3_general_ci", 768L, 2304L,
                0L },
            new Object[] { "xwikistrings", "custom_unique", "XWS_ID", null, null, null, null, 0L },
            // Not a table to convert
            new Object[] { "custom_table", "custom_index", "VALUE", null, "utf8mb3_general_ci", 2000L, 6000L, 1L }));

        NativeQuery<Object[]> rowFormatsQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("ROW_FORMAT FROM INFORMATION_SCHEMA.TABLES")))
            .thenReturn(rowFormatsQuery);
        when(rowFormatsQuery.list()).thenReturn(List.of(new Object[] { "xwikidoc", "Compact" },
            new Object[] { "activitystream_events", "Redundant" }, new Object[] { "xwikiproperties", "Dynamic" },
            new Object[] { "xwikistrings", "Compact" }, new Object[] { "xwikilistitems", "Compact" },
            new Object[] { "xwikircs", "Compressed" }));
    }

    @Test
    void getConvertToDatabaseCollationStatements()
    {
        mockCollationQueries();

        // The legacy row formats are switched in the same statement, as well as the row format of the compressed
        // entity, and the custom mapping (unlike the static mapping of xwikipreferences) is left alone
        assertEquals(List.of("ALTER TABLE `xwiki`.`xwikistrings` DROP FOREIGN KEY `FKstrings`",
            "ALTER TABLE `xwiki`.`activitystream_events` DROP INDEX `ase_page_date`",
            "ALTER TABLE `xwiki`.`xwikistrings` DROP INDEX `custom_unique`",
            "ALTER TABLE `xwiki`.`activitystream_events` ROW_FORMAT=Dynamic," + CONVERT,
            "ALTER TABLE `xwiki`.`xwikilistitems` ROW_FORMAT=Dynamic," + CONVERT,
            "ALTER TABLE `xwiki`.`xwikipreferences`" + CONVERT,
            "ALTER TABLE `xwiki`.`xwikiproperties` ROW_FORMAT=Compressed," + CONVERT,
            "ALTER TABLE `xwiki`.`xwikistrings` ROW_FORMAT=Dynamic," + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Index [ase_page_date] (key length of [3080] bytes after the conversion) exceeds the maximum key"
            + " length of [3072] bytes and will be dropped. It can be re-created with a shorter prefix using"
            + " [CREATE INDEX `ase_page_date` ON `xwiki`.`activitystream_events` (`ase_page`, `ase_date`)].",
            this.logCapture.getMessage(0));
        assertEquals("Index [custom_unique] (key length of [3080] bytes after the conversion) exceeds the maximum key"
            + " length of [3072] bytes and will be dropped. It can be re-created with a shorter prefix using"
            + " [CREATE UNIQUE INDEX `custom_unique` ON `xwiki`.`xwikistrings` (`XWS_VALUE`, `XWS_ID`)].",
            this.logCapture.getMessage(1));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenCompressionNotAllowed()
    {
        mockCollationQueries();
        when(this.hibernateStore.getConfiguration().getProperty("xwiki.compressionAllowed")).thenReturn("false");
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikiproperties", "latin1_bin", INNODB }));

        assertEquals(List.of("ALTER TABLE `xwiki`.`xwikistrings` DROP FOREIGN KEY `FKstrings`",
            "ALTER TABLE `xwiki`.`xwikiproperties`" + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenMariaDB()
    {
        mockCollationQueries();

        // MariaDB supports too long unique indexes
        assertEquals(List.of("ALTER TABLE `xwiki`.`xwikistrings` DROP FOREIGN KEY `FKstrings`",
            "ALTER TABLE `xwiki`.`activitystream_events` DROP INDEX `ase_page_date`",
            "ALTER TABLE `xwiki`.`activitystream_events` ROW_FORMAT=Dynamic," + CONVERT,
            "ALTER TABLE `xwiki`.`xwikilistitems` ROW_FORMAT=Dynamic," + CONVERT,
            "ALTER TABLE `xwiki`.`xwikipreferences`" + CONVERT,
            "ALTER TABLE `xwiki`.`xwikiproperties` ROW_FORMAT=Compressed," + CONVERT,
            "ALTER TABLE `xwiki`.`xwikistrings` ROW_FORMAT=Dynamic," + CONVERT),
            this.mariaDBAdapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Index [ase_page_date] (key length of [3080] bytes after the conversion) exceeds the maximum key"
            + " length of [3072] bytes and will be dropped. It can be re-created with a shorter prefix using"
            + " [CREATE INDEX `ase_page_date` ON `xwiki`.`activitystream_events` (`ase_page`, `ase_date`)].",
            this.logCapture.getMessage(0));
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenUnknownCharacterSet()
    {
        mockCollationQueries();
        when(this.maxLengthQuery.uniqueResult()).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Unknown character set [utf8mb4]", exception.getMessage());
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenNothingToConvert()
    {
        mockCollationQueries();
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "custom_table", "latin1_bin", INNODB }));

        assertEquals(List.of(), this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenNonInnoDBTables()
    {
        mockCollationQueries();
        when(this.tablesQuery.list()).thenReturn(List.of(new Object[] { "xwikidoc", "utf8mb4_general_ci", INNODB },
            new Object[] { "xwikistrings", "utf8mb3_general_ci", "MyISAM" },
            new Object[] { "xwikiproperties", "latin1_bin", "Aria" },
            // Not a table of the mapping
            new Object[] { "custom_table", "latin1_bin", "MyISAM" }));

        // The InnoDB maximum key length and row formats don't apply to the other storage engines
        assertEquals(List.of("ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`",
            "ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Tables [xwikiproperties, xwikistrings] of database [xwiki] don't use collation [utf8mb4_bin] but"
            + " won't be converted since they don't use the InnoDB storage engine. Switching them to InnoDB is"
            + " recommended, they will then be converted at the next startup.", this.logCapture.getMessage(0));
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenDatabaseNotFound()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list()).thenReturn(List.of());

        assertEquals(List.of(), this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenNonBinaryDatabaseCollation()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "utf8mb4", "utf8mb4_0900_ai_ci" }));
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikidoc", "utf8mb4_0900_ai_ci", INNODB }));

        // The binary collation of the database character set is used instead, starting with the database default
        assertEquals(List.of("ALTER DATABASE `xwiki` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin",
            "ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`",
            "ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        verify(this.tablesQuery).setParameter("collation", "utf8mb4_bin");
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenOnlyDatabaseCollationIsWrong()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "utf8mb4", "utf8mb4_0900_ai_ci" }));
        when(this.tablesQuery.list()).thenReturn(List.of());

        // The tables created later (e.g. by the schema update) get the expected collation
        assertEquals(List.of("ALTER DATABASE `xwiki` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin"),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenLinkedToNonStandardTable()
    {
        mockCollationQueries();
        // custom_table is not part of the mapping, but it's linked to xwikidoc by a foreign key on string columns
        when(this.tablesQuery.list()).thenReturn(List.of(new Object[] { "xwikidoc", "utf8mb4_general_ci", INNODB },
            new Object[] { "custom_table", "utf8mb3_general_ci", INNODB }));

        // It's converted too (including dropping its too long index), otherwise the foreign key could not be
        // re-created
        assertEquals(List.of("ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`",
            "ALTER TABLE `xwiki`.`custom_table` DROP INDEX `custom_index`",
            "ALTER TABLE `xwiki`.`custom_table`" + CONVERT,
            "ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Index [custom_index] (key length of [8000] bytes after the conversion) exceeds the maximum key"
            + " length of [3072] bytes and will be dropped. It can be re-created with a shorter prefix using"
            + " [CREATE INDEX `custom_index` ON `xwiki`.`custom_table` (`VALUE`)].", this.logCapture.getMessage(0));
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenLinkedToAnotherDatabase()
    {
        mockCollationQueries();
        when(this.tablesQuery.list()).thenReturn(List.of(new Object[] { "xwikidoc", "utf8mb4_general_ci", INNODB },
            new Object[] { "xwikiproperties", "utf8mb4_general_ci", INNODB },
            new Object[] { "xwikistrings", "utf8mb4_general_ci", INNODB }));
        when(this.foreignKeysQuery.list()).thenReturn(List.of(
            new Object[] { "xwiki", "xwikistrings", "FKstrings", "xwiki", "xwikiproperties", "RESTRICT", "RESTRICT",
                "XWS_NAME", "XWP_NAME", "utf8mb3_general_ci" },
            // A foreign key of another database referencing a table to convert
            new Object[] { "otherwiki", "other_table", "FKother", "xwiki", "xwikiproperties", "RESTRICT",
                "RESTRICT", "VALUE", "XWP_NAME", "utf8mb3_general_ci" }));

        // Neither xwikiproperties nor xwikistrings (linked to it) are converted
        assertEquals(List.of("ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Tables [xwikiproperties, xwikistrings] of database [xwiki] won't be converted since they (or a"
            + " table linked to them by a foreign key) are linked by a foreign key to a table of another database,"
            + " which could not be re-created after the conversion", this.logCapture.getMessage(0));
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenDroppedIndexNeededByForeignKey()
    {
        mockCollationQueries();
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikilistitems", "utf8mb3_bin", INNODB }));
        NativeQuery<Object[]> indexesQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INFORMATION_SCHEMA.STATISTICS"))).thenReturn(indexesQuery);
        // 8 + 1000 * 4 = 4008: too long, and needed by the FKnumbers foreign key (on non string columns)
        when(indexesQuery.list()).thenReturn(List.of(
            new Object[] { "xwikilistitems", "long_index", "XWL_ID", null, null, null, null, 1L },
            new Object[] { "xwikilistitems", "long_index", "XWL_VALUE", null, "utf8mb3_bin", 1000L, 3000L, 1L }));

        // The foreign key is dropped (and re-created after the conversion) since it prevents dropping the index
        assertEquals(List.of("ALTER TABLE `xwiki`.`xwikilistitems` DROP FOREIGN KEY `FKnumbers`",
            "ALTER TABLE `xwiki`.`xwikilistitems` DROP INDEX `long_index`",
            "ALTER TABLE `xwiki`.`xwikilistitems` ROW_FORMAT=Dynamic," + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Index [long_index] (key length of [4008] bytes after the conversion) exceeds the maximum key"
            + " length of [3072] bytes and will be dropped. It can be re-created with a shorter prefix using"
            + " [CREATE INDEX `long_index` ON `xwiki`.`xwikilistitems` (`XWL_ID`, `XWL_VALUE`)].",
            this.logCapture.getMessage(0));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenWiderCharacterSet()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list()).thenReturn(List.<Object[]>of(new Object[] { "utf8mb3", "utf8mb3_bin" }));
        when(this.maxLengthQuery.uniqueResult()).thenReturn(3L);
        when(this.tablesQuery.list()).thenReturn(
            // The tables which don't use utf8mb3_bin
            List.of(new Object[] { "xwikistrings", "utf8mb4_bin", INNODB },
                new Object[] { "xwikiproperties", "utf8mb3_general_ci", INNODB },
                new Object[] { "xwikidoc", "latin1_bin", INNODB },
                new Object[] { "activitystream_events", "utf8mb4_general_ci", INNODB },
                new Object[] { "activitystream_events", "utf8mb4_bin", INNODB }),
            // The tables which don't use utf8mb4_bin
            List.of(new Object[] { "xwikiproperties", "utf8mb3_general_ci", INNODB },
                new Object[] { "xwikidoc", "latin1_bin", INNODB },
                new Object[] { "activitystream_events", "utf8mb4_general_ci", INNODB }));

        NativeQuery<String> wideQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("s.MAXLEN > :maxLength"))).thenReturn(wideQuery);
        when(wideQuery.list()).thenReturn(List.of("activitystream_events", "custom_table", "xwikistrings"));

        // Some standard tables use utf8mb4, so the database and all its tables are converted to utf8mb4 (xwikistrings
        // already uses utf8mb4_bin)
        assertEquals(List.of("ALTER DATABASE `xwiki` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin",
            "ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`",
            "ALTER TABLE `xwiki`.`xwikistrings` DROP FOREIGN KEY `FKstrings`",
            "ALTER TABLE `xwiki`.`activitystream_events` DROP INDEX `ase_page_date`",
            "ALTER TABLE `xwiki`.`activitystream_events` ROW_FORMAT=Dynamic," + CONVERT,
            "ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT,
            "ALTER TABLE `xwiki`.`xwikiproperties` ROW_FORMAT=Compressed," + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        verify(this.tablesQuery).setParameter("collation", "utf8mb3_bin");
        verify(this.tablesQuery).setParameter("collation", "utf8mb4_bin");
        assertEquals("Index [ase_page_date] (key length of [3080] bytes after the conversion) exceeds the maximum key"
            + " length of [3072] bytes and will be dropped. It can be re-created with a shorter prefix using"
            + " [CREATE INDEX `ase_page_date` ON `xwiki`.`activitystream_events` (`ase_page`, `ase_date`)].",
            this.logCapture.getMessage(0));
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenUTF8MB3()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "utf8mb3", "utf8mb3_general_ci" }));
        when(this.maxLengthQuery.uniqueResult()).thenReturn(3L);
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikidoc", "utf8mb3_general_ci", INNODB }));

        // utf8mb3 is kept since no standard table uses a wider character set
        assertEquals(List.of("ALTER DATABASE `xwiki` CHARACTER SET utf8mb3 COLLATE utf8mb3_bin",
            "ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`",
            "ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic, CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_bin"),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenNonUnicodeDatabase()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "latin1", "latin1_swedish_ci" }));
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikidoc", "latin1_swedish_ci", INNODB }));

        // latin1 cannot hold most of the translations, so the database and its tables are converted to utf8mb4
        assertEquals(List.of("ALTER DATABASE `xwiki` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin",
            "ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`",
            "ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        verify(this.tablesQuery).setParameter("collation", "utf8mb4_bin");
    }

    private Statement mockJDBCStatement() throws SQLException
    {
        Connection connection = mock(Connection.class);
        Statement jdbcStatement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(jdbcStatement);
        doAnswer(invocation -> {
            invocation.<Work>getArgument(0).execute(connection);
            return null;
        }).when(this.session).doWork(any());

        return jdbcStatement;
    }

    @Test
    void convertToDatabaseCollation() throws SQLException
    {
        mockCollationQueries();
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikidoc", "utf8mb4_general_ci", INNODB }));

        NativeQuery statement = mock(NativeQuery.class);
        when(this.session.createNativeQuery(startsWith("ALTER TABLE "))).thenReturn(statement);
        Transaction transaction = mock(Transaction.class);
        when(this.session.getTransaction()).thenReturn(transaction);
        Statement jdbcStatement = mockJDBCStatement();

        this.adapter.convertToDatabaseCollation(this.metadata, this.session);

        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`");
        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT);
        verify(statement, times(2)).executeUpdate();
        verify(transaction, times(2)).commit();

        // The dropped foreign key is re-created without the foreign key checks
        InOrder inOrder = inOrder(jdbcStatement);
        inOrder.verify(jdbcStatement).execute("SET FOREIGN_KEY_CHECKS=0");
        inOrder.verify(jdbcStatement).execute("ALTER TABLE `xwiki`.`custom_table` ADD CONSTRAINT `FKcustom` FOREIGN KEY"
            + " (`VALUE`) REFERENCES `xwiki`.`xwikidoc` (`XWD_FULLNAME`) ON UPDATE CASCADE ON DELETE NO ACTION");
        inOrder.verify(jdbcStatement).execute("SET FOREIGN_KEY_CHECKS=1");
        verify(jdbcStatement).close();
    }

    @Test
    void convertToDatabaseCollationWhenForeignKeyCannotBeRecreated() throws SQLException
    {
        mockCollationQueries();
        when(this.tablesQuery.list()).thenReturn(List.of(new Object[] { "xwikidoc", "utf8mb4_general_ci", INNODB },
            new Object[] { "xwikiproperties", "utf8mb4_general_ci", INNODB }));

        NativeQuery<?> alterQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(startsWith("ALTER TABLE "))).thenReturn(alterQuery);
        Transaction transaction = mock(Transaction.class);
        when(this.session.getTransaction()).thenReturn(transaction);
        Statement jdbcStatement = mockJDBCStatement();
        String customForeignKey = "ALTER TABLE `xwiki`.`custom_table` ADD CONSTRAINT `FKcustom` FOREIGN KEY (`VALUE`)"
            + " REFERENCES `xwiki`.`xwikidoc` (`XWD_FULLNAME`) ON UPDATE CASCADE ON DELETE NO ACTION";
        when(jdbcStatement.execute(customForeignKey)).thenThrow(new SQLException("incompatible"));

        this.adapter.convertToDatabaseCollation(this.metadata, this.session);

        // The failure does not prevent re-creating the other foreign keys
        InOrder inOrder = inOrder(jdbcStatement);
        inOrder.verify(jdbcStatement).execute("SET FOREIGN_KEY_CHECKS=0");
        inOrder.verify(jdbcStatement).execute(customForeignKey);
        inOrder.verify(jdbcStatement).execute("ALTER TABLE `xwiki`.`xwikistrings` ADD CONSTRAINT `FKstrings`"
            + " FOREIGN KEY (`XWS_ID`, `XWS_NAME`) REFERENCES `xwiki`.`xwikiproperties` (`XWP_ID`, `XWP_NAME`)"
            + " ON UPDATE RESTRICT ON DELETE RESTRICT");
        inOrder.verify(jdbcStatement).execute("SET FOREIGN_KEY_CHECKS=1");

        assertEquals("Failed to re-create a foreign key with [" + customForeignKey + "]: SQLException: incompatible."
            + " The Hibernate schema update re-creates the missing foreign keys of the Hibernate mapping, the other"
            + " ones have to be re-created manually.", this.logCapture.getMessage(0));
    }

    @Test
    void convertToDatabaseCollationWhenConversionFails() throws SQLException
    {
        mockCollationQueries();
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikidoc", "utf8mb4_general_ci", INNODB }));

        NativeQuery<?> dropStatement = mock(NativeQuery.class);
        when(this.session.createNativeQuery("ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`"))
            .thenReturn(dropStatement);
        NativeQuery<?> convertStatement = mock(NativeQuery.class);
        when(this.session.createNativeQuery("ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT))
            .thenReturn(convertStatement);
        HibernateException failure = new HibernateException("failure");
        when(convertStatement.executeUpdate()).thenThrow(failure);
        Transaction transaction = mock(Transaction.class);
        when(this.session.getTransaction()).thenReturn(transaction);
        Statement jdbcStatement = mockJDBCStatement();

        HibernateException exception = assertThrows(HibernateException.class,
            () -> this.adapter.convertToDatabaseCollation(this.metadata, this.session));

        assertSame(failure, exception);
        verify(dropStatement).executeUpdate();
        // The dropped foreign key is re-created anyway, otherwise it would be lost
        verify(jdbcStatement).execute("ALTER TABLE `xwiki`.`custom_table` ADD CONSTRAINT `FKcustom` FOREIGN KEY"
            + " (`VALUE`) REFERENCES `xwiki`.`xwikidoc` (`XWD_FULLNAME`) ON UPDATE CASCADE ON DELETE NO ACTION");
        verify(jdbcStatement).execute("SET FOREIGN_KEY_CHECKS=1");
    }

    @Test
    void convertToDatabaseCollationWhenConversionAndForeignKeysRecreationFail()
    {
        mockCollationQueries();
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikidoc", "utf8mb4_general_ci", INNODB }));

        NativeQuery<?> statement = mock(NativeQuery.class);
        when(this.session.createNativeQuery(startsWith("ALTER TABLE "))).thenReturn(statement);
        HibernateException failure = new HibernateException("failure");
        when(statement.executeUpdate()).thenThrow(failure);
        Transaction transaction = mock(Transaction.class);
        when(this.session.getTransaction()).thenReturn(transaction);
        HibernateException recreateFailure = new HibernateException("recreate failure");
        doThrow(recreateFailure).when(this.session).doWork(any());

        HibernateException exception = assertThrows(HibernateException.class,
            () -> this.adapter.convertToDatabaseCollation(this.metadata, this.session));

        // The conversion failure is the one reported
        assertSame(failure, exception);
        assertArrayEquals(new Throwable[] { recreateFailure }, exception.getSuppressed());
    }

    @Test
    void getRowFormats()
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("subwiki");
        Session rowFormatsSession = mock(Session.class);
        NativeQuery<Object[]> query = mock(NativeQuery.class);
        when(rowFormatsSession.createNativeQuery(anyString())).thenReturn(query);
        when(query.list()).thenReturn(
            List.of(new Object[] { "xwikidoc", "Dynamic" }, new Object[] { "xwikircs", "Compressed" }));

        assertEquals(Map.of("xwikidoc", "Dynamic", "xwikircs", "Compressed"),
            this.adapter.getRowFormats(rowFormatsSession));

        verify(query).setParameter("schema", "subwiki");
        verify(rowFormatsSession).createNativeQuery("SELECT TABLE_NAME, ROW_FORMAT FROM INFORMATION_SCHEMA.TABLES"
            + " WHERE TABLE_SCHEMA = :schema AND TABLE_TYPE = 'BASE TABLE' AND ENGINE = 'InnoDB'");
    }

    @Test
    void getRowFormatsWhenNoCurrentWiki()
    {
        when(this.wikis.getCurrentWikiId()).thenReturn(null);

        IllegalStateException exception =
            assertThrows(IllegalStateException.class, () -> this.adapter.getRowFormats(mock(Session.class)));

        assertEquals("Cannot get the tables row formats without a current wiki database", exception.getMessage());
    }

    private SessionFactory mockRepairDatabase()
    {
        mockCollationQueries();

        SessionFactory sessionFactory = mock(SessionFactory.class);
        when(this.hibernateStore.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.openSession()).thenReturn(this.session);

        this.alterStatement = mock(NativeQuery.class);
        when(this.session.createNativeQuery(startsWith("ALTER TABLE "))).thenReturn(this.alterStatement);
        Transaction transaction = mock(Transaction.class);
        when(this.session.getTransaction()).thenReturn(transaction);

        return sessionFactory;
    }

    @Test
    void repairDatabase() throws Exception
    {
        mockRepairDatabase();
        when(this.tablesQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "xwikidoc", "utf8mb4_general_ci", INNODB }));

        this.adapter.repairDatabase(this.metadata);

        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.`custom_table` DROP FOREIGN KEY `FKcustom`");
        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.`xwikidoc` ROW_FORMAT=Dynamic," + CONVERT);
        verify(this.alterStatement, times(2)).executeUpdate();
        verify(this.session).close();
    }

    @Test
    void repairDatabaseWhenNothingToConvert() throws Exception
    {
        mockRepairDatabase();
        when(this.tablesQuery.list()).thenReturn(List.of());

        this.adapter.repairDatabase(this.metadata);

        // Nothing is touched, not even the legacy row formats
        verify(this.alterStatement, never()).executeUpdate();
        verify(this.session).close();
    }

    @Test
    void repairDatabaseWhenFixTableEncodingDisabled() throws Exception
    {
        SessionFactory sessionFactory = mockRepairDatabase();
        when(this.xwikiProperties.getProperty(FIX_TABLE_ENCODING_PROPERTY, Boolean.class)).thenReturn(false);

        this.adapter.repairDatabase(this.metadata);

        verify(sessionFactory, never()).openSession();
    }

    @Test
    void repairDatabaseWhenFailing()
    {
        mockRepairDatabase();
        HibernateException failure = new HibernateException("failure");
        when(this.tablesQuery.list()).thenThrow(failure);

        HibernateStoreException exception =
            assertThrows(HibernateStoreException.class, () -> this.adapter.repairDatabase(this.metadata));

        assertEquals("Failed to convert the tables to the database character set and a binary collation. The"
            + " conversion at startup can be disabled by setting [store.hibernate.mysql.fixTableEncoding] to false in"
            + " xwiki.properties.", exception.getMessage());
        assertSame(failure, exception.getCause());
        verify(this.session).close();
    }
}
