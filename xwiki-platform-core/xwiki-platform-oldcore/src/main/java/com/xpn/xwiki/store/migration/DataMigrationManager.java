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
package com.xpn.xwiki.store.migration;

import org.xwiki.component.annotation.Role;

/**
 * Interface for all migration managers.
 *
 * @version $Id$
 * @since 3.4M1
 */
@Role
public interface DataMigrationManager
{
    /**
     * @return current DB version or null for a new database
     * @xwiki.xwikicfg xwiki.store.migration.version - override data version
     * @throws DataMigrationException if any error
     */
    XWikiDBVersion getDBVersion() throws DataMigrationException;

    /**
     * @return current DB migration status or null for a new database
     * @throws DataMigrationException if any error
     * @since 4.4.1
     */
    DataMigrationStatus getDataMigrationStatus() throws DataMigrationException;

    /**
     * Check current database version and proceed to migrations. Migration is processed only once, and depends on the
     * following configuration:
     *
     * @xwiki.xwikicfg xwiki.store.migration - 1 to enable migration, default to 0
     * @xwiki.xwikicfg xwiki.store.migration.databases - list of database to migrate, default to all
     * @xwiki.xwikicfg xwiki.store.migration.forced - force run selected migrations
     * @xwiki.xwikicfg xwiki.store.migration.ignored - ignore selected migrations
     * @xwiki.xwikicfg xwiki.store.migration.exitAfterEnd - 1 to exit at the end of migrations, default to 0
     * @throws MigrationRequiredException when version is incompatible with current version
     * @throws DataMigrationException when an error occurs during check.
     */
    void checkDatabase() throws MigrationRequiredException, DataMigrationException;

    /**
     * @return latest DB version
     * @since 3.4M1
     */
    XWikiDBVersion getLatestVersion();

    /**
     * Setup the schema of a new DB and set it to the latest version (not running migrations).
     * This should be used on a newly created DB only
     *
     * @throws DataMigrationException if any error
     * @since 3.4M1
     */
    void initNewDB() throws DataMigrationException;
}
