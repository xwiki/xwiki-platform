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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI2977: Add a Globally Unique Identifier (GUID) to objects. This data migration adds GUIDs to
 * existing objects.
 *
 * @version $Id$
 */
@Component
@Named("R15428XWIKI2977")
@Singleton
public class R15428XWIKI2977DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "Add a GUID to existing objects when upgrading from pre-1.8M1.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(15428);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // migrate data
        getStore().executeWrite(getXWikiContext(), true, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                Query q = session.createQuery("select o from BaseObject o where o.guid is null");
                List<BaseObject> lst = q.list();
                if (lst.size() == 0) {
                    return null;
                }
                List<BaseObject> lst2 = new ArrayList<BaseObject>(lst.size());
                for (BaseObject o : lst) {
                    o.setGuid(UUID.randomUUID().toString());
                    lst2.add(o);
                }
                for (BaseObject o : lst2) {
                    session.update(o);
                }
                return null;
            }
        });
    }
}
