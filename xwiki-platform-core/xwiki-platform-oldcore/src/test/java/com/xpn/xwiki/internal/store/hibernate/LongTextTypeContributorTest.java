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

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link LongTextTypeContributor} with the dialects of the databases where the length of the columns mapped
 * with the {@code text} type would otherwise change their type.
 *
 * @version $Id$
 */
class LongTextTypeContributorTest
{
    @ParameterizedTest
    @CsvSource({ "org.hibernate.dialect.MySQLDialect, longtext", "org.hibernate.dialect.MariaDBDialect, longtext",
        "org.hibernate.dialect.PostgreSQLDialect, text" })
    void textColumnsUseTheLongType(String dialect, String expectedType)
    {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", dialect)
            // Don't try to connect to a database
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false).build();
        try {
            Metadata metadata = new MetadataSources(registry).addResource("xwiki.hbm.xml").buildMetadata();

            // Mapped with type="text" and different lengths
            assertEquals(expectedType, getSqlType(metadata, "com.xpn.xwiki.doc.XWikiDocument", "content"));
            assertEquals(expectedType, getSqlType(metadata, "com.xpn.xwiki.objects.LargeStringProperty", "value"));
            // Not affected
            assertEquals("varchar(768)", getSqlType(metadata, "com.xpn.xwiki.doc.XWikiDocument", "title"));
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
