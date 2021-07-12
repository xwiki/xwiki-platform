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
package org.xwiki.administration.internal.migration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;

/**
 * Migration to ensure that all ConfigurableClass xobjects are migrated to use the new scope property.
 *
 * @version $Id$
 * @since 13.6RC1
 */
@Component
@Named("R130600000XWIKI18723")
@Singleton
public class R130600000XWIKI18723DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    private ConfigurableClassScopeMigrator configurableClassScopeMigrator;

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        try {
            this.configurableClassScopeMigrator.migrateAllConfigurableClass();
        } catch (QueryException e) {
            throw new DataMigrationException("Error while performing migration of old ConfigurableClass property.", e);
        }
    }

    @Override
    public String getDescription()
    {
        return "Migrate ConfigurableClass xobjects to remove old configureGlobally property and use scope property.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(130600000);
    }
}
