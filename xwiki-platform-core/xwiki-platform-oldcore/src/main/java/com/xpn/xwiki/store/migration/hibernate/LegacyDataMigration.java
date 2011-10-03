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

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI4396: Duplicate document ID. This dataMigration change document ID to use the new improved hash
 * algorithm.
 *
 * @version $Id$
 * @since 3.4M1
 */
@Component
@Named("Legacy")
public class LegacyDataMigration extends AbstractHibernateDataMigration
{
    @Override
    public String getDescription()
    {
        return "Convert very old legacy databases";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(0);
    }

    @Override
    public void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // migrate data
        getStore().executeWrite(getXWikiContext(), true, new XWikiHibernateBaseStore.HibernateCallback<Object>()
        {
            /** Update SQL command. */
            private static final String UPDATE = "update ";

            /** Delete SQL command. */
            private static final String DELETE_FROM = "delete from ";

            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                String docClass = XWikiDocument.class.getName();
                try {
                    session
                        .createQuery(
                            UPDATE + docClass + " doc set doc.translation = 0 where doc.translation is null")
                        .executeUpdate();

                    session
                        .createQuery(UPDATE + docClass + " doc set doc.language = '' where doc.language is null")
                        .executeUpdate();

                    session
                        .createQuery(UPDATE + docClass
                            + " doc set doc.defaultLanguage = '' where doc.defaultLanguage is null")
                        .executeUpdate();

                    session
                        .createQuery(UPDATE + docClass
                            + " doc set doc.fullName = concat(doc.space,'.',doc.name) where doc.fullName is null")
                        .executeUpdate();

                    session
                        .createQuery(UPDATE + docClass + " doc set doc.elements = 3 where doc.elements is null")
                        .executeUpdate();

                    try {
                        session
                            .createQuery(DELETE_FROM + BaseProperty.class.getName() + " prop"
                                + " where prop.name like 'editbox_%'"
                                + " and prop.classType = 'com.xpn.xwiki.objects.LongProperty'")
                            .executeUpdate();

                        session
                            .createQuery(
                                DELETE_FROM + LongProperty.class.getName() + " prop where prop.name like 'editbox_%'")
                            .executeUpdate();
                    } catch (Exception ignored) {
                        // Cleanup may fail, this is not important enough to break the whole stuff.
                    }
                } catch (Exception e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_STORE_MIGRATION, getName() + " migration failed", e);
                }
                return null;
            }
        });
    }
}
