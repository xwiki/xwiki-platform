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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI2977: Add a Globally Unique Identifier (GUID) to objects. This migrator adds GUIDs to existing
 * objects.
 * 
 * @version $Id$
 */
public class R15428XWIKI2977Migrator extends AbstractXWikiHibernateMigrator
{
    /**
     * {@inheritDoc}
     * 
     * @see AbstractXWikiHibernateMigrator#getName()
     */
    public String getName()
    {
        return "R15428XWIKI2977";
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.store.migration.hibernate.AbstractXWikiHibernateMigrator#getDescription()
     */
    public String getDescription()
    {
        return "Add a GUID to existing objects when upgrading from pre-1.8M1.";
    }

    /** {@inheritDoc} */
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(15428);
    }

    /** {@inheritDoc} */
    @Override
    public void migrate(XWikiHibernateMigrationManager manager, final XWikiContext context) throws XWikiException
    {
        // migrate data
        manager.getStore(context).executeWrite(context, true, new HibernateCallback<Object>()
        {
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                Query q = session.createQuery("select o from BaseObject o where o.guid is null");
                List lst = q.list();
                if (lst.size() == 0) {
                    return null;
                }
                List<BaseObject> lst2 = new ArrayList<BaseObject>(lst.size());
                for (Iterator it = lst.iterator(); it.hasNext();) {
                    BaseObject o = (BaseObject) it.next();
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
