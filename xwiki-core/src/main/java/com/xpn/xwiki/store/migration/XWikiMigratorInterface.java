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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Interface for migrators which migrate data.
 * New migrators should named like "R"+vernum+issuenumber+"Migrator" for prevent collisions. 
 * @version $Id$
 */
public interface XWikiMigratorInterface
{
    /**
     * @return the migrator name. For example "R4340XWIKI883". 
     */
    String getName();

    /**
     * @return a description of what the migrator does
     */
    String getDescription();

    /**
     * @return data version which need migration. 
     * before you commit stuff which needs migration,
     *  you need write migrator with version = current svn revision number. 
     */
    XWikiDBVersion getVersion();

    /**
     * Run migration.
     * @param manager the manager which run migration. used for access to store system.
     * @param context used everywhere
     * @throws XWikiException if any error
     */
    void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
        throws XWikiException;

    /**
     * @param startupVersion the database version when the migration process starts (before any
     *        migrator is applied). This is useful for migrator which need to run only when the
     *        database is in a certain version.
     * @return true if the migration should be executed or false otherwise
     */
    boolean shouldExecute(XWikiDBVersion startupVersion);
}
