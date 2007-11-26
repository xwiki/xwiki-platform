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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI1878: Fix xwikircs table isdiff data not matching RCS state of some revisions (when the state
 * says "full" the isdiff column in the database should be false).
 *
 * Note: This migrator should only be executed if the R4359XWIKI1459 one has already been executed (i.e. if the
 * database is in version < 4360). This is because this current migrator is because of a bug in R4359XWIKI1459 which
 * has now been fixed.  
 * 
 * @version $Id: $
 */
public class R6079XWIKI1878Migrator extends R4359XWIKI1459Migrator
{
    /** logger. */
    private static final Log LOG = LogFactory.getLog(R6079XWIKI1878Migrator.class);

    private int previousVersion;

    public R6079XWIKI1878Migrator(int currentVersionBeforeMigratorsExecute)
    {
        this.previousVersion = currentVersionBeforeMigratorsExecute;
    }

    /**
     * {@inheritDoc}
     * @see com.xpn.xwiki.store.migration.hibernate.AbstractXWikiHibernateMigrator#getName()
     */
    public String getName()
    {
        return "R6079XWIKI1878";
    }

    /**
     * {@inheritDoc}
     * @see AbstractXWikiHibernateMigrator#getDescription()
     */
    public String getDescription()
    {
        return "See http://jira.xwiki.org/jira/browse/XWIKI-1878";
    }

    /** {@inheritDoc} */
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(6079);
    }

    /** {@inheritDoc} */
    public void migrate(XWikiHibernateMigrationManager manager, final XWikiContext context) throws XWikiException
    {
        if (this.previousVersion >= 4360) {
            super.migrate(manager, context, LOG);
        }
    }
}
