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

/**
 * Common interface to report data migration status.
 *
 * @version $Id$
 * @since 4.4.1
 */
public interface DataMigrationStatus
{
    /**
     * @return current DB version or null for a new database
     * @xwiki.xwikicfg xwiki.store.migration.version - override data version
     * @throws DataMigrationException if any error
     */
    XWikiDBVersion getDBVersion() throws DataMigrationException;

    /**
     * @return true if any migration has been attempted on current database
     */
    boolean hasDataMigrationBeenAttempted();

    /**
     * @return true if all attempted migrations has been successfully applied on current database
     */
    boolean hasBeenSuccessfullyMigrated();

    /**
     * @return the exception returned on failure by the last attempted migration on this database
     */
    Exception getLastMigrationException();
}
