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
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI1933: Editing users fails.
 * 
 * Note: This migrator should only be executed if the R4340XWIKI833 one has already been executed
 * during a previous migration (i.e. if the database is in version >= 4340). This is because this
 * current migrator is because the old migrator was only executed in the main wiki, and there was
 * some code that inserted wrong data after the migration.
 * 
 * @version $Id$
 */
public class R6405XWIKI1933Migrator extends R4340XWIKI883Migrator
{
    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.migration.hibernate.AbstractXWikiHibernateMigrator#getName()
     */
    public String getName()
    {
        return "R6405XWIKI1933";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractXWikiHibernateMigrator#getDescription()
     */
    public String getDescription()
    {
        return "See http://jira.xwiki.org/jira/browse/XWIKI-1933";
    }

    /** {@inheritDoc} */
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(6405);
    }

    /**
     * {@inheritDoc}
     * @see AbstractXWikiHibernateMigrator#shouldExecute(com.xpn.xwiki.store.migration.XWikiDBVersion)
     */
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return (startupVersion.getVersion() >= 4340);
    }

    /** {@inheritDoc} */
    public void migrate(XWikiHibernateMigrationManager manager, final XWikiContext context)
        throws XWikiException
    {
        super.migrate(manager, context);
    }
}
