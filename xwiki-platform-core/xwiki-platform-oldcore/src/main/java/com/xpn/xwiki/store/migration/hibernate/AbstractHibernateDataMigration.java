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
import javax.inject.Provider;

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
     * Component manager used to access stores.
     */
    @Inject
    protected ComponentManager componentManager;

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    protected Provider<DataMigrationManager> manager;

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
        String hint = null;
        Named named = this.getClass().getAnnotation(Named.class);
        if (named != null) {
            hint = named.value();
        }
        return hint;
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        return true;
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
