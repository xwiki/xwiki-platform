/*
 * Copyright 2007, XpertNet SARL, and individual contributors.
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

import org.hibernate.HibernateException;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Sample migrator which nothing does.
 * It just set version to 4326+1.
 * @version $Id: $
 */
public class R4326SampleMigrator extends AbstractXWikiHibernateMigrator
{
    /** {@inheritDoc} */
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(4326);
    }
    /** {@inheritDoc} */
    public void migrate(XWikiHibernateMigrationManager manager, XWikiContext context)
        throws XWikiException
    {
        manager.getStore(context).executeWrite(context, true, new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException
            {
                // here you can write migration
                return null;
            }
        });
    }
}
