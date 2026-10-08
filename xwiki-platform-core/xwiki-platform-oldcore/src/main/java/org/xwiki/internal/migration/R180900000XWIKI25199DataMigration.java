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
package org.xwiki.internal.migration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.AbstractHibernateDataMigration;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

/**
 * Migration in charge of migrating again the passwords that have been put back in their legacy storage after
 * {@link R180100000XWIKI23827DataMigration} and {@link R180800000XWIKI24357DataMigration} were executed.
 * <p>
 * Those migrations used to keep the documents cache untouched while changing the passwords directly in the database.
 * So a document loaded before them (e.g. a user profile loaded by a previous migration) and saved after them (e.g.
 * when a mandatory class gets updated during the initialization) got back its legacy password property, stored as a
 * {@code StringProperty} with a value that is not re-encoded. Both migrations only handle the values that are not
 * migrated yet, so executing them again only fixes those passwords.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Singleton
@Named("180900000XWIKI25199")
public class R180900000XWIKI25199DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    @Named("180100000XWIKI23827")
    private HibernateDataMigration passwordStorageMigration;

    @Inject
    @Named("180800000XWIKI24357")
    private HibernateDataMigration passwordHashMigration;

    @Override
    public String getDescription()
    {
        return "Migrate again the passwords put back in their legacy storage after their migration.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(180900000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException
    {
        // The storage needs to be migrated first since the re-encoding only handles the passwords stored in the
        // dedicated table.
        this.passwordStorageMigration.migrate();
        this.passwordHashMigration.migrate();
    }
}
