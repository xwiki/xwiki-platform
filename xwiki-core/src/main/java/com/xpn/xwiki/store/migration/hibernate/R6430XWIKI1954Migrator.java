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

import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI1954: When migrating the document archive format from 1.1 to 1.2, delete the
 * contents of the old XWD_ARCHIVE field.
 * 
 * Note: This migrator should only be executed if the R4359XWIKI1459 one has already been executed
 * during a previous migration (i.e. if the database is in version >= 4359).
 *
 * @version $Id: $
 */
public class R6430XWIKI1954Migrator extends AbstractXWikiHibernateMigrator
{
    private int startupVersion;

    public R6430XWIKI1954Migrator(int currentVersionBeforeMigratorsExecute)
    {
        this.startupVersion = currentVersionBeforeMigratorsExecute;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.migration.hibernate.AbstractXWikiHibernateMigrator#getName()
     */
    public String getName()
    {
        return "R6430XWIKI1954";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractXWikiHibernateMigrator#getDescription()
     */
    public String getDescription()
    {
        return "See http://jira.xwiki.org/jira/browse/XWIKI-1954";
    }

    /** {@inheritDoc} */
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(6430);
    }

    /** {@inheritDoc} */
    public void migrate(XWikiHibernateMigrationManager manager, final XWikiContext context)
        throws XWikiException
    {
        if (this.startupVersion >= 4359) {
            manager.getStore(context).executeWrite(context, true, new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    try {
                        Statement stmt = session.connection().createStatement();
                        stmt.executeUpdate("update xwikidoc set XWD_ARCHIVE=null");
                        stmt.close();
                    } catch (SQLException e) {
                        // Maybe the columnd doesn't exist.
                    }
                    return Boolean.TRUE;
                }
            });
        }
    }
}
