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
 * Migration for XWIKI-13474. Duplicate existing attachment size column into a new column supporting long values.
 *
 * @version $Id$
 * @since 9.0RC1
 */
@Component
@Named("R90000XWIKI13474")
@Singleton
public class R90000XWIKI13474DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "Convert attachment size type to BIGINT to allow attachments bigger than 2GB.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(90000);
    }

    @Override
    public void hibernateMigrate() throws XWikiException, DataMigrationException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Void>()
        {
            @Override
            public Void doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.createQuery("UPDATE XWikiAttachment SET longSize = filesize").executeUpdate();

                return null;
            }
        });
    }
}
