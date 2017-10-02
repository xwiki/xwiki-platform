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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-14697. Make sure all attachment have a store id.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component
@Named("R99000XWIKI14697")
@Singleton
public class R99000XWIKI14697DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    @Named(XWikiCfgConfigurationSource.ROLEHINT)
    private ConfigurationSource configuration;

    @Override
    public String getDescription()
    {
        return "Make sure all existing attachments have a store id.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(99000);
    }

    @Override
    public void hibernateMigrate() throws XWikiException, DataMigrationException
    {
        final String defaultStoreHint =
            this.configuration.getProperty("xwiki.store.attachment.hint", XWikiHibernateBaseStore.HINT);

        getStore().executeWrite(getXWikiContext(), new HibernateCallback<Void>()
        {
            @Override
            public Void doInHibernate(Session session) throws HibernateException
            {
                Query query = session.createQuery("UPDATE XWikiAttachment SET xmlStore = ?");
                query.setString(0, defaultStoreHint);
                query.executeUpdate();

                return null;
            }
        });
    }
}
