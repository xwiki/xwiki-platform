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

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.mapping.Table;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.store.hibernate.HibernateStoreException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
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

    private static final String FIX_TABLE_COLLATION_PROPERTY = "store.hibernate.mysql.fixTableCollation";

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

    private NativeQuery<String> tablesQuery;

    private NativeQuery<Number> maxLengthQuery;

    private NativeQuery<?> alterStatement;

    @BeforeEach
    void beforeEach()
    {
        when(this.hibernateStore.getDialect()).thenReturn(new MySQLDialect());
        when(this.hibernateConfiguration.getDBPrefix()).thenReturn("");
        when(this.wikis.getMainWikiId()).thenReturn("xwiki");
        when(this.xwikiProperties.getProperty(FIX_TABLE_COLLATION_PROPERTY, true)).thenReturn(true);
    }

    @Test
    void getAlterRowFormatStringIsQualifiedWithTheCurrentWikiDatabase() throws Exception
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("subwiki");

        // The statement must name the database explicitly, otherwise it is executed against whichever database the
        // pooled JDBC connection happens to be on.
        assertEquals("ALTER TABLE `subwiki`.xwikircs ROW_FORMAT=Dynamic",
            this.adapter.getAlterRowFormatString("xwikircs", false, Map.of("xwikircs", "Compact"), null));
        assertEquals("ALTER TABLE `subwiki`.xwikircs ROW_FORMAT=Compressed",
            this.adapter.getAlterRowFormatString("xwikircs", true, Map.of(), null));
    }

    @Test
    void getAlterRowFormatStringWhenNoCurrentWiki() throws Exception
    {
        when(this.wikis.getCurrentWikiId()).thenReturn(null);

        assertEquals("ALTER TABLE xwikircs ROW_FORMAT=Dynamic",
            this.adapter.getAlterRowFormatString("xwikircs", false, Map.of(), null));
    }

    private void mockCollationQueries()
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("xwiki");

        this.metadata = mock(Metadata.class);
        when(this.metadata.collectTableMappings()).thenReturn(List.of(new Table("xwikidoc"),
            new Table("xwikiproperties"), new Table("xwikistrings"), new Table("activitystream_events")));

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
        when(this.tablesQuery.list()).thenReturn(
            List.of("xwikistrings", "activitystream_events", "custom_table", "xwikiproperties"));

        NativeQuery<Object[]> foreignKeysQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INFORMATION_SCHEMA.KEY_COLUMN_USAGE")))
            .thenReturn(foreignKeysQuery);
        when(foreignKeysQuery.list()).thenReturn(
            List.of(new Object[] { "xwikistrings", "FKstrings", "xwikiproperties" },
                new Object[] { "custom_table", "FKcustom", "xwikidoc" }));

        // table, index, column, sub part, type, collation, max length, octet length, precision, datetime precision,
        // non unique
        NativeQuery<Object[]> indexesQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INFORMATION_SCHEMA.STATISTICS"))).thenReturn(indexesQuery);
        when(indexesQuery.list()).thenReturn(List.of(
            // 768 * 4 + 5 = 3077: too long
            new Object[] { "activitystream_events", "ase_page_date", "ase_page", null, "varchar",
                "utf8mb3_general_ci", 768L, 2304L, null, null, 1L },
            new Object[] { "activitystream_events", "ase_page_date", "ase_date", null, "datetime", null, null, null,
                null, 0L, 1L },
            // 768 * 4 = 3072: fits
            new Object[] { "activitystream_events", "EVENT_PAGE", "ase_page", null, "varchar", "utf8mb3_general_ci",
                768L, 2304L, null, null, 1L },
            // 200 * 4 = 800: fits
            new Object[] { "activitystream_events", "ase_param1", "ase_param1", 200L, "longtext",
                "utf8mb3_general_ci", 4294967295L, 4294967295L, null, null, 1L },
            // 3000 + 2 + 8 + 8 + 8 = 3026: fits
            new Object[] { "xwikistrings", "binary_index", "XWS_BINARY", null, "varbinary", null, 3000L, 3000L, null,
                null, 1L },
            new Object[] { "xwikistrings", "binary_index", "XWS_BIT", null, "bit", null, null, null, 9L, null, 1L },
            new Object[] { "xwikistrings", "binary_index", "XWS_DATE", null, "datetime", null, null, null, null, 6L,
                1L },
            new Object[] { "xwikistrings", "binary_index", "XWS_ID", null, "bigint", null, null, null, 19L, null,
                1L },
            new Object[] { "xwikistrings", "binary_index", "XWS_DECIMAL", null, "decimal", null, null, null, 10L,
                null, 1L },
            // 768 * 4 + 8 = 3080: too long
            new Object[] { "xwikistrings", "custom_unique", "XWS_VALUE", null, "varchar", "utf8mb3_general_ci", 768L,
                2304L, null, null, 0L },
            new Object[] { "xwikistrings", "custom_unique", "XWS_ID", null, "bigint", null, null, null, 19L, null,
                0L },
            // Not a table to convert
            new Object[] { "custom_table", "custom_index", "VALUE", null, "varchar", "utf8mb3_general_ci", 2000L,
                6000L, null, null, 1L }));
    }

    @Test
    void getConvertToDatabaseCollationStatements()
    {
        mockCollationQueries();

        assertEquals(List.of("ALTER TABLE `xwiki`.xwikistrings DROP FOREIGN KEY `FKstrings`",
            "ALTER TABLE `xwiki`.activitystream_events DROP INDEX `ase_page_date`",
            "ALTER TABLE `xwiki`.xwikistrings DROP INDEX `custom_unique`",
            "ALTER TABLE `xwiki`.activitystream_events" + CONVERT, "ALTER TABLE `xwiki`.xwikiproperties" + CONVERT,
            "ALTER TABLE `xwiki`.xwikistrings" + CONVERT),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Index [ase_page_date] (key length of [3077] bytes after the conversion) exceeds the maximum key"
            + " length of [3072] bytes and will be dropped. It can be re-created with a shorter prefix using"
            + " [CREATE INDEX `ase_page_date` ON `xwiki`.`activitystream_events` (`ase_page`, `ase_date`)].",
            this.logCapture.getMessage(0));
        assertEquals("Index [custom_unique] (key length of [3080] bytes after the conversion) exceeds the maximum key"
            + " length of [3072] bytes and will be dropped. It can be re-created with a shorter prefix using"
            + " [CREATE UNIQUE INDEX `custom_unique` ON `xwiki`.`xwikistrings` (`XWS_VALUE`, `XWS_ID`)].",
            this.logCapture.getMessage(1));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenMariaDB()
    {
        mockCollationQueries();

        // MariaDB supports too long unique indexes
        assertEquals(List.of("ALTER TABLE `xwiki`.xwikistrings DROP FOREIGN KEY `FKstrings`",
            "ALTER TABLE `xwiki`.activitystream_events DROP INDEX `ase_page_date`",
            "ALTER TABLE `xwiki`.activitystream_events" + CONVERT, "ALTER TABLE `xwiki`.xwikiproperties" + CONVERT,
            "ALTER TABLE `xwiki`.xwikistrings" + CONVERT),
            this.mariaDBAdapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Index [ase_page_date] (key length of [3077] bytes after the conversion) exceeds the maximum key"
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
        when(this.tablesQuery.list()).thenReturn(List.of("custom_table"));

        assertEquals(List.of(), this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenDatabaseNotFound()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list()).thenReturn(List.of());

        assertEquals(List.of(), this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenCaseInsensitiveCollation()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list())
            .thenReturn(List.<Object[]>of(new Object[] { "utf8mb4", "utf8mb4_0900_ai_ci" }));

        assertEquals(List.of(), this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("The collation [utf8mb4_0900_ai_ci] configured on database [xwiki] is not case sensitive, its"
            + " tables won't be converted to it. XWiki requires a case sensitive collation (e.g. utf8mb4_bin).",
            this.logCapture.getMessage(0));
    }

    @Test
    void getConvertToDatabaseCollationStatementsWhenLossyCharacterSet()
    {
        mockCollationQueries();
        when(this.defaultsQuery.list()).thenReturn(List.<Object[]>of(new Object[] { "utf8mb3", "utf8mb3_bin" }));
        when(this.tablesQuery.list()).thenReturn(List.of("xwikistrings", "xwikiproperties", "xwikidoc"));

        NativeQuery<String> lossyQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("CHARACTER_SET_NAME <> :characterSet"))).thenReturn(lossyQuery);
        when(lossyQuery.list()).thenReturn(List.of("custom_table", "xwikistrings"));

        // xwikiproperties is linked to xwikistrings by a foreign key, so it's not converted either
        assertEquals(List.of("ALTER TABLE `xwiki`.custom_table DROP FOREIGN KEY `FKcustom`",
            "ALTER TABLE `xwiki`.xwikidoc CONVERT TO CHARACTER SET utf8mb3 COLLATE utf8mb3_bin"),
            this.adapter.getConvertToDatabaseCollationStatements(this.metadata, this.session));

        assertEquals("Tables [xwikiproperties, xwikistrings] of database [xwiki] won't be converted to character set"
            + " [utf8mb3] since they (or a table linked to them by a foreign key) contain columns using another"
            + " character set, some characters of which might be lost. Converting the database to utf8mb4 is"
            + " recommended.", this.logCapture.getMessage(0));
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void convertToDatabaseCollation()
    {
        mockCollationQueries();
        when(this.tablesQuery.list()).thenReturn(List.of("xwikidoc"));

        NativeQuery statement = mock(NativeQuery.class);
        when(this.session.createNativeQuery(startsWith("ALTER TABLE "))).thenReturn(statement);
        Transaction transaction = mock(Transaction.class);
        when(this.session.getTransaction()).thenReturn(transaction);

        this.adapter.convertToDatabaseCollation(this.metadata, this.session);

        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.custom_table DROP FOREIGN KEY `FKcustom`");
        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.xwikidoc" + CONVERT);
        verify(statement, times(2)).executeUpdate();
        verify(transaction, times(2)).commit();
    }

    @Test
    void getRowFormatsWhenNoCurrentWiki()
    {
        when(this.wikis.getCurrentWikiId()).thenReturn(null);

        IllegalStateException exception =
            assertThrows(IllegalStateException.class, () -> this.adapter.getRowFormats(mock(Session.class)));

        assertEquals("Cannot get the tables row formats without a current wiki database", exception.getMessage());
    }

    private void mockRepairDatabase()
    {
        mockCollationQueries();

        SessionFactory sessionFactory = mock(SessionFactory.class);
        when(this.hibernateStore.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.openSession()).thenReturn(this.session);

        NativeQuery<Object[]> rowFormatsQuery = mock(NativeQuery.class);
        when(this.session.createNativeQuery(contains("INNODB_SYS_TABLES"))).thenReturn(rowFormatsQuery);
        when(rowFormatsQuery.list()).thenReturn(List.of(new Object[] { "xwiki/xwikidoc", "Compact" },
            new Object[] { "xwiki/xwikilistitems", "Redundant" }, new Object[] { "xwiki/xwikistrings", "Dynamic" },
            new Object[] { "xwiki/xwikircs", "Compressed" }));

        this.alterStatement = mock(NativeQuery.class);
        when(this.session.createNativeQuery(startsWith("ALTER TABLE "))).thenReturn(this.alterStatement);
        when(this.session.getTransaction()).thenReturn(mock(Transaction.class));
    }

    @Test
    void repairDatabase() throws Exception
    {
        mockRepairDatabase();
        when(this.tablesQuery.list()).thenReturn(List.of());

        // xwikilistitems is a collection table and xwikiproperties does not exist yet
        when(this.metadata.collectTableMappings()).thenReturn(List.of(new Table("xwikidoc"),
            new Table("xwikilistitems"), new Table("xwikiproperties"), new Table("xwikistrings"),
            new Table("xwikircs")));

        this.adapter.repairDatabase(this.metadata);

        // Only the legacy row formats are changed
        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.xwikidoc ROW_FORMAT=Dynamic");
        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.xwikilistitems ROW_FORMAT=Dynamic");
        verify(this.alterStatement, times(2)).executeUpdate();
        verify(this.tablesQuery).list();
        verify(this.session).close();
    }

    @Test
    void repairDatabaseWhenFixTableCollationDisabled() throws Exception
    {
        mockRepairDatabase();
        when(this.xwikiProperties.getProperty(FIX_TABLE_COLLATION_PROPERTY, true)).thenReturn(false);

        this.adapter.repairDatabase(this.metadata);

        // The legacy row formats are still fixed
        verify(this.session).createNativeQuery("ALTER TABLE `xwiki`.xwikidoc ROW_FORMAT=Dynamic");
        verify(this.tablesQuery, never()).list();
        verify(this.session).close();
    }

    @Test
    void repairDatabaseWhenFailing()
    {
        mockRepairDatabase();
        HibernateException failure = new HibernateException("failure");
        when(this.tablesQuery.list()).thenThrow(failure);

        HibernateStoreException exception =
            assertThrows(HibernateStoreException.class, () -> this.adapter.repairDatabase(this.metadata));

        assertEquals("Failed to repair the database", exception.getMessage());
        assertSame(failure, exception.getCause());
        verify(this.session).close();
    }
}
