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
package com.xpn.xwiki.store.migration.hibernate;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.XWikiMigrationManagerInterface;
import com.xpn.xwiki.store.migration.XWikiMigratorInterface;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Template for migrators of hibernate store
 * @see XWikiMigratorInterface
 * @version $Id$
 */
public abstract class AbstractXWikiHibernateMigrator implements XWikiMigratorInterface
{
    /** {@inheritDoc} */
    public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
        throws XWikiException
    {
        migrate((XWikiHibernateMigrationManager)manager, context);
    }

    /**
     * {@inheritDoc}
     * @see com.xpn.xwiki.store.migration.XWikiMigratorInterface#shouldExecute(XWikiDBVersion)
     */
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return true;
    }

    /** @see XWikiMigratorInterface#migrate(XWikiMigrationManagerInterface,XWikiContext)  */
    public abstract void migrate(XWikiHibernateMigrationManager manager, XWikiContext context)
        throws XWikiException;
}
