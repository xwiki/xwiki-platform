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
package org.xwiki.store.hibernate;

import java.sql.Driver;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Implement XWiki Hibernate features specific to a RDBMS. The goal is mainly to extend the Hibernate {@link Dialect}
 * and JDBC {@link Driver} concepts with features they don't support (table compression, wiki -> database name, etc.).
 * <p>
 * It's recommended to extend {@link AbstractHibernateAdapter}.
 * <p>
 * A {@link HibernateAdapter} is supposed to be provided by a {@link HibernateAdapterFactory}, but if you need to
 * associate the {@link HibernateAdapter} to a specific database engine (and optionally to a specific minimum version)
 * you can use the following helper format:
 * <ul>
 * <li>"jdbc scheme/minimum version" (for example the "mysql/8" adapter will be selected for a MySQL 9.0.1 server, if no
 * adapter is registered for version 9)</li>
 * <li>"jdbc scheme" (for example the "mysql" adapter will be selected for a MySQL 5.7 server, if no adapter is
 * registered for version 5.7 or lower)</li>
 * </ul>
 * 
 * @version $Id$
 * @since 17.1.0RC1
 */
@Unstable
@Role
public interface HibernateAdapter
{
    /**
     * The name of the meta attribute indicating if a table should be compressed.
     */
    String META_ATTRIBUTE_COMPRESSED = "xwiki-compressed";

    // Configuration

    /**
     * Get the native database/schema name for the current wiki.
     *
     * @return the native database/schema name
     */
    String getDatabaseFromWikiName();

    /**
     * Get the native database/schema name for the passed wiki identifier.
     *
     * @param wikiId the identifier of the wiki
     * @return the native database/schema name
     */
    String getDatabaseFromWikiName(String wikiId);

    /**
     * @return true if the user has configured Hibernate to use XWiki in schema mode (vs database mode)
     */
    boolean isConfiguredInSchemaMode();

    /**
     * @param persistentClass the Hibernate entity
     * @return the table name
     */
    String getTableName(PersistentClass persistentClass);

    /**
     * @param table the Hibernate table representation
     * @return the table name
     */
    String getTableName(Table table);

    /**
     * @param tableName the name of the table
     * @return the name of the table in the right case/format
     */
    String getTableName(String tableName);

    /**
     * @param entity the Hibernate entity for which to extract the configuration
     * @return true if the table should be compressed
     */
    boolean isCompressed(PersistentClass entity);

    /**
     * Escape database name to be used in a query.
     *
     * @param databaseName the schema name to escape
     * @return the escaped version
     */
    String escapeDatabaseName(String databaseName);

    /**
     * @return true if compression is enabled for this database. Its possible to force it using hibernate.cfg.xml
     *         {@code xwiki.compression} property, but the default may vary depending on the database.
     */
    boolean isCompressionAllowed();

    // Global

    /**
     * @return the Hibernate {@link SessionFactory}
     */
    SessionFactory getSessionFactory();

    /**
     * @return the Hibernate {@link Dialect}
     */
    Dialect getDialect();

    // Database operations

    /**
     * Automatically update the current database schema to contains what's defined in provided metadata.
     * 
     * @param metadata the metadata we want the current database to follow
     * @throws HibernateStoreException when failing to update the database
     */
    void updateDatabase(Metadata metadata) throws HibernateStoreException;

    /**
     * @return true if the current database product is catalog based, false for a schema based databases
     */
    boolean isCatalog();
}
