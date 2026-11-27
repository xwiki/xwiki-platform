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

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI2079: When migrating the document archive format from 1.0 or before to 1.2, delete the old
 * XWD_ARCHIVE field, as it will prevent saving documents, since that column used to have a NOT NULL constraint. Also,
 * Hibernate does not delete columns/tables that don't appear in the mapping file, so the column must be manually
 * dropped.
 *
 * @version $Id$
 * @since 1.3M2
 * @since 1.2.2
 */
@Component
@Named("R7345XWIKI2079")
@Singleton
public class R7350XWIKI2079DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "See https://jira.xwiki.org/browse/XWIKI-2079";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(7350);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                try {
                    session.createSQLQuery("ALTER TABLE xwikidoc DROP COLUMN XWD_ARCHIVE").executeUpdate();
                } catch (HibernateException ex) {
                    // Maybe the column doesn't exist. Anyway, in case we're using a DBMS which
                    // doesn't support DROP COLUMN (such as Derby < 10.3.1.4), we can try to alter
                    // the column to allow NULL values.
                    // TODO Can we check the exception and see what is happening?
                    try {
                        session.createSQLQuery("ALTER TABLE xwikidoc ALTER COLUMN XWD_ARCHIVE " + "SET DEFAULT ' '")
                                .executeUpdate();
                    } catch (HibernateException ex2) {
                        // Maybe the column doesn't exist, after all.
                        /*
                         * TODO Can we check the exception and see what is happening? If the statements failed because
                         * they are not supported by the DBMS, then this is a fatal error, perhaps we should stop
                         * serving request and notify the admin
                         */
                    }
                }

                return Boolean.TRUE;
            }
        });
    }
}
