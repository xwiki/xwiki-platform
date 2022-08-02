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
import org.hibernate.query.Query;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-12277: Remove the 'type' xproperty from TemplateProviderClass
 * <p>
 * Migrate TemplateProviderClass' removed 'type' property values to the new 'terminal' property.
 *
 * @version $Id$
 * @since 7.3RC1
 */
@Component
@Named("R73000XWIKI12277")
@Singleton
public class R73000XWIKI12277DataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "Migrate TemplateProviderClass' removed 'type' property values to the new 'terminal' property.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(73000);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                return doWork(session);
            }

        });
    }

    private Object doWork(Session session) throws HibernateException
    {
        Query<Object[]> query = session.createQuery(createQueryString(), Object[].class);

        for (Object[] result : query.list()) {
            BaseObject object = (BaseObject) result[0];
            StringProperty typeProperty = (StringProperty) result[1];

            // Migrate each property.
            migrateProperty(typeProperty, object, session);
        }

        return Boolean.TRUE;
    }

    private String createQueryString()
    {
        StringBuilder query = new StringBuilder();
        query.append("SELECT templateProviderObj, typeProp ");
        query.append("FROM BaseObject templateProviderObj, StringProperty typeProp ");
        query.append("WHERE templateProviderObj.className='XWiki.TemplateProviderClass'");
        query.append(" AND templateProviderObj.name<>'XWiki.TemplateProviderTemplate'");
        query.append(" AND typeProp.id.id=templateProviderObj.id");
        query.append(" AND typeProp.name='type'");

        return query.toString();
    }

    private void migrateProperty(StringProperty typeProperty, BaseObject object, Session session)
        throws HibernateException
    {
        // Create the new property value and assign it to the owning object.
        IntegerProperty terminalProperty = new IntegerProperty();
        int value = 1;
        if ("space".equals(typeProperty.getValue())) {
            value = 0;
        }
        terminalProperty.setValue(value);
        terminalProperty.setName("terminal");
        terminalProperty.setObject(object);

        // Save the new property.
        session.saveOrUpdate(terminalProperty);
        // Delete the old property.
        session.delete(typeProperty);
    }
}
