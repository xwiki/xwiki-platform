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
package com.xpn.xwiki.internal.store.hibernate;

import java.util.function.Consumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link LongTextTypeContributor} with the dialects of the databases where the length of the columns mapped
 * with the {@code text} type would otherwise change their type.
 *
 * @version $Id$
 */
class LongTextTypeContributorTest
{
    private static final String MYSQL = "org.hibernate.dialect.MySQLDialect";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @ParameterizedTest
    @CsvSource({ MYSQL + ", longtext", "org.hibernate.dialect.MariaDBDialect, longtext",
        "org.hibernate.dialect.PostgreSQLDialect, text" })
    void textColumnsUseTheLongType(String dialect, String expectedType)
    {
        withMetadata(dialect, "xwiki.hbm.xml", metadata -> {
            assertEquals(expectedType, getSqlType(metadata, "com.xpn.xwiki.doc.XWikiDocument", "content"));
            assertEquals(expectedType, getSqlType(metadata, "com.xpn.xwiki.objects.LargeStringProperty", "value"));
            // Not affected
            assertEquals("varchar(768)", getSqlType(metadata, "com.xpn.xwiki.doc.XWikiDocument", "title"));

            // The XWiki mappings don't use deprecated lengths
            int start = this.logCapture.size();
            new LongTextTypeContributor().integrate(metadata, null, null);
            assertEquals(start, this.logCapture.size());
        });

        this.logCapture.ignoreAllMessages();
    }

    @ParameterizedTest
    @CsvSource({ MYSQL + ", longtext", "org.hibernate.dialect.PostgreSQLDialect, text" })
    void deprecatedLengthsUseTheLongType(String dialect, String expectedType)
    {
        withMetadata(dialect, "legacytext.hbm.xml", metadata -> {
            assertEquals(expectedType, getSqlType(metadata, "LegacyText", "limited"));
            assertEquals(expectedType, getSqlType(metadata, "LegacyText", "noLength"));
            assertEquals(expectedType, getSqlType(metadata, "LegacyText", "unlimited"));
        });

        this.logCapture.ignoreAllMessages();
    }

    @Test
    void integrateLogsTheDeprecatedLengths()
    {
        withMetadata(MYSQL, "legacytext.hbm.xml", metadata -> {
            // The deprecation warning of the hbm.xml mappings
            int start = this.logCapture.size();

            new LongTextTypeContributor().integrate(metadata, null, null);

            assertEquals(start + 2, this.logCapture.size());
            assertEquals("Deprecated length [2000] for the column [LT_LIMITED] of the table [legacytext] mapped with"
                + " the type [text]: the column gets the large character type of the database whatever its length."
                + " Use the length [2147483647] for a property which is not meant to be limited.",
                this.logCapture.getMessage(start));
            assertEquals("Deprecated length [null] for the column [LT_NO_LENGTH] of the table [legacytext] mapped"
                + " with the type [text]: the column gets the large character type of the database whatever its"
                + " length. Use the length [2147483647] for a property which is not meant to be limited.",
                this.logCapture.getMessage(start + 1));
        });

        this.logCapture.ignoreAllMessages();
    }

    private void withMetadata(String dialect, String resource, Consumer<Metadata> consumer)
    {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", dialect)
            // Don't try to connect to a database
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false).build();
        try {
            consumer.accept(new MetadataSources(registry).addResource(resource).buildMetadata());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private String getSqlType(Metadata metadata, String entity, String property)
    {
        Column column = metadata.getEntityBinding(entity).getProperty(property).getColumns().get(0);

        return column.getSqlType(metadata);
    }
}
