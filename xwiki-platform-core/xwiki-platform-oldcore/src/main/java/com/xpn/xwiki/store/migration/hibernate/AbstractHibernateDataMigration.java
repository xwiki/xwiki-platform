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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.DataMigrationManager;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Template for data migration of hibernate store.
 *
 * @see com.xpn.xwiki.store.migration.DataMigration
 * @version $Id$
 * @since 3.4M1
 */
public abstract class AbstractHibernateDataMigration implements HibernateDataMigration
{
    /**
     * Size of a major version in the {@code MMmmppNNN} DB version format (e.g. {@code 171014000} for 17.10.14).
     */
    private static final int MAJOR_VERSION_SIZE = 10000000;

    /**
     * Size of a minor version in the {@code MMmmppNNN} DB version format.
     */
    private static final int MINOR_VERSION_SIZE = 100000;

    /**
     * The last minor version of a major cycle (e.g. 17.10), after which the next branch is the next major version.
     */
    private static final int LAST_MINOR_VERSION = 10;

    /**
     * Component manager used to access stores.
     */
    @Inject
    protected ComponentManager componentManager;

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    protected Provider<DataMigrationManager> manager;

    @Inject
    private ComponentDescriptor<HibernateDataMigration> componentDescriptor;

    /**
     * Execution context used to access XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * @return XWikiContext to access the store
     */
    protected XWikiContext getXWikiContext()
    {
        ExecutionContext context = this.execution.getContext();
        return (XWikiContext) context.getProperty("xwikicontext");
    }

    /**
     * @return store system for execute store-specific actions.
     * @throws DataMigrationException if the store could not be reached
     */
    protected XWikiHibernateBaseStore getStore() throws DataMigrationException
    {
        try {
            return (XWikiHibernateBaseStore) this.componentManager.getInstance(XWikiStoreInterface.class,
                XWikiHibernateBaseStore.HINT);
        } catch (ComponentLookupException e) {
            throw new DataMigrationException(
                String.format("Unable to reach the store for database %s", getXWikiContext().getWikiId()), e);
        }
    }

    @Override
    public String getName()
    {
        return componentDescriptor.getRoleHint();
    }

    /**
     * {@inheritDoc}
     * <p>
     * By default, the migration is executed unless the database already contains one of its backported copies (see
     * {@link #getBackportVersions()}). Subclasses overriding this method should call it to keep that check.
     */
    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return getBackportVersions().stream().noneMatch(backportVersion -> isInBackportBranch(startupVersion,
            backportVersion));
    }

    /**
     * Declare the versions of the copies of this migration that were backported to older branches, so that
     * {@link #shouldExecute(XWikiDBVersion)} doesn't execute it again on a database that already contains one of them.
     * A database is considered to contain a backported copy when its version is between the backported version
     * (included) and the first version of the next branch (excluded): for example, a backport version of
     * {@code 171014000} (17.10.14) skips databases from 17.10.14 to 18.0.0 (excluded), and a backport version of
     * {@code 160401000} (16.4.1) skips databases from 16.4.1 to 16.5.0 (excluded).
     *
     * @return the DB versions (in the {@code MMmmppNNN} format) of the backported copies of this migration, empty by
     *         default
     * @since 18.9.0RC1
     */
    protected List<XWikiDBVersion> getBackportVersions()
    {
        return List.of();
    }

    private boolean isInBackportBranch(XWikiDBVersion startupVersion, XWikiDBVersion backportVersion)
    {
        int version = backportVersion.getVersion();
        int major = version / MAJOR_VERSION_SIZE;
        int minor = (version / MINOR_VERSION_SIZE) % (MAJOR_VERSION_SIZE / MINOR_VERSION_SIZE);
        int nextBranchVersion = minor >= LAST_MINOR_VERSION ? (major + 1) * MAJOR_VERSION_SIZE
            : major * MAJOR_VERSION_SIZE + (minor + 1) * MINOR_VERSION_SIZE;

        return startupVersion.getVersion() >= version && startupVersion.getVersion() < nextBranchVersion;
    }

    /**
     * Execute the migration itself.
     *
     * @throws DataMigrationException on migration error.
     * @throws XWikiException on error from the store.
     */
    protected abstract void hibernateMigrate() throws DataMigrationException, XWikiException;

    @Override
    public void migrate() throws DataMigrationException
    {
        try {
            hibernateMigrate();
        } catch (Exception e) {
            throw new DataMigrationException(String.format("Data migration %s failed", getName()), e);
        }
    }

    @Override
    public String getPreHibernateLiquibaseChangeLog() throws DataMigrationException
    {
        return null;
    }

    @Override
    public String getLiquibaseChangeLog() throws DataMigrationException
    {
        return null;
    }

    /**
     * @return the current DB version (after executing the previous migrations)
     * @throws DataMigrationException when failing to get the current DB version
     * @since 11.0
     */
    protected XWikiDBVersion getCurrentDBVersion() throws DataMigrationException
    {
        return this.manager.get().getDBVersion();
    }
}
