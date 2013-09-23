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

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-883: global access preferences cannot be updated.
 * 
 * @version $Id$
 */
@Component
@Named("R4340XWIKI883")
@Singleton
public class R4340XWIKI883DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "See http://jira.xwiki.org/jira/browse/XWIKI-883";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(4340);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException
            {
                Query q = session.createQuery(
                    "select s from BaseObject o, StringProperty s where o.className like 'XWiki.XWiki%Rights'"
                    + " and o.id=s.id and (s.name='users' or s.name='groups')");
                @SuppressWarnings("unchecked")
                List<StringProperty> lst = q.list();
                if (lst.size() == 0) {
                    return null;
                }
                List<LargeStringProperty> lst2 = new ArrayList<LargeStringProperty>(lst.size());
                for (Iterator<StringProperty> it = lst.iterator(); it.hasNext();) {
                    StringProperty sp = it.next();
                    LargeStringProperty lsp = new LargeStringProperty();
                    lsp.setId(sp.getId());
                    lsp.setName(sp.getName());
                    lsp.setValue(sp.getValue());
                    lst2.add(lsp);
                }
                for (StringProperty property : lst) {
                    session.delete(property);
                }
                for (LargeStringProperty property : lst2) {
                    session.save(property);
                }
                return null;
            }
        });
    }
}
