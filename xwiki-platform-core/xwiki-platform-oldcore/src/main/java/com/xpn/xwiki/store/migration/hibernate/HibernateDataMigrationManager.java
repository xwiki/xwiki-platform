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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.AbstractDataMigrationManager;
import com.xpn.xwiki.store.migration.DataMigration;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.sdk.resource.MockResourceAccessor;

/**
 * Migration manager for hibernate store.
 *
 * @version $Id$
 * @since 3.4M1
 */
@Component
@Named(XWikiHibernateBaseStore.HINT)
@Singleton
public class HibernateDataMigrationManager extends AbstractDataMigrationManager
{
    /**
     * Name of the liquibase resource used to include additional change logs XMLs. If it exists, this resource should
     * contains at least one valid liquibase XML definition.
     */
    private static final String LIQUIBASE_RESOURCE = "liquibase-xwiki/";

    /**
     * Name for which the change log is served.
     */
    public static final String CHANGELOG_NAME = "liquibase.xml";

    /**
     * @return store system for execute store-specific actions.
     * @throws DataMigrationException if the store could not be reached
     */
    public XWikiHibernateBaseStore getBaseStore() throws DataMigrationException
    {
        try {
            return this.componentManager.getInstance(XWikiStoreInterface.class, XWikiHibernateBaseStore.HINT);
        } catch (ComponentLookupException e) {
            throw new DataMigrationException("Unable to reach the base store", e);
        }
    }

    private HibernateStore getStore() throws DataMigrationException
    {
        try {
            return this.componentManager.getInstance(HibernateStore.class);
        } catch (ComponentLookupException e) {
            throw new DataMigrationException("Unable to reach the store", e);
        }
    }

    @Override
    public XWikiDBVersion getDBVersionFromDatabase() throws DataMigrationException
    {
        XWikiDBVersion ver = getDBVersionFromConfig();
        if (ver != null) {
            return ver;
        }

        final XWikiContext context = getXWikiContext();
        final HibernateStore store = getStore();

        // Check if the version table exist
        if (!store.tableExists(XWikiDBVersion.class)) {
            // Check if the document table exist
            if (!store.tableExists(XWikiDocument.class)) {
                // The database does not seems to be initialized at all
                return null;
            }

            // The database seems older than the introduction of the version table
            return new XWikiDBVersion(0);
        }

        XWikiHibernateBaseStore baseStore = getBaseStore();

        // Try retrieving a version from the database
        try {
            ver = baseStore.executeRead(context, new HibernateCallback<XWikiDBVersion>()
            {
                @Override
                public XWikiDBVersion doInHibernate(Session session) throws HibernateException
                {
                    CriteriaBuilder builder = session.getCriteriaBuilder();
                    CriteriaQuery<XWikiDBVersion> query = builder.createQuery(XWikiDBVersion.class);
                    Root<XWikiDBVersion> root = query.from(XWikiDBVersion.class);
                    query.select(root);
                    List<XWikiDBVersion> versions = session.createQuery(query).getResultList();

                    return versions.isEmpty() ? new XWikiDBVersion(0) : versions.get(0);
                }
            });
        } catch (XWikiException e) {
            throw new DataMigrationException("Failed to get the database version", e);
        }

        return ver;
    }

    @Override
    protected void initializeEmptyDB() throws DataMigrationException
    {
        final XWikiContext context = getXWikiContext();
        final XWikiHibernateBaseStore store = getBaseStore();

        final Session originalSession = store.getSession(context);
        final Transaction originalTransaction = store.getTransaction(context);
        store.setSession(null, context);
        store.setTransaction(null, context);

        try {
            updateSchema(null);
            setDBVersion(getLatestVersion());
        } finally {
            store.setSession(originalSession, context);
            store.setTransaction(originalTransaction, context);
        }
    }

    @Override
    protected void setDBVersionToDatabase(final XWikiDBVersion version) throws DataMigrationException
    {
        final XWikiContext context = getXWikiContext();

        try {
            getBaseStore().executeWrite(context, session -> {
                session.createQuery("delete from " + XWikiDBVersion.class.getName()).executeUpdate();
                session.save(version);

                return null;
            });
        } catch (Exception e) {
            throw new DataMigrationException(String.format("Unable to store new data version %d into database %s",
                version.getVersion(), context.getWikiId()), e);
        }
    }

    @Override
    protected void updateSchema(Collection<XWikiMigration> migrations) throws DataMigrationException
    {
        try {
            liquibaseUpdate(migrations, true);
            hibernateShemaUpdate();
            liquibaseUpdate(migrations, false);
        } catch (Exception e) {
            throw new DataMigrationException(
                String.format("Unable to update schema of wiki [%s]", getXWikiContext().getWikiId()), e);
        }
    }

    /**
     * Run hibernate schema updates
     *
     * @throws DataMigrationException if the store is not accessible
     */
    private void hibernateShemaUpdate() throws DataMigrationException
    {
        if (this.logger.isInfoEnabled()) {
            this.logger.info("Checking Hibernate mapping and updating schema if needed for wiki [{}]",
                getXWikiContext().getWikiId());
        }
        getBaseStore().updateSchema(getXWikiContext(), true);
    }

    /**
     * Run liquibase for a given set of change logs
     *
     * @param migrations the set of migration to visit
     * @param preHibernate if true, use pre-hibernate schema update changelogs.
     * @throws XWikiException
     * @throws DataMigrationException
     */
    private void liquibaseUpdate(Collection<XWikiMigration> migrations, boolean preHibernate)
        throws XWikiException, DataMigrationException
    {
        final String database = getXWikiContext().getWikiId();

        // Execute migrations
        if (migrations != null) {
            for (XWikiMigration migration : migrations) {
                if (migration.dataMigration instanceof HibernateDataMigration) {
                    liquibaseUpdate((HibernateDataMigration) migration.dataMigration, preHibernate, database);
                }
            }
        }

        if (!preHibernate) {
            // Execute liquibase changes from resources if any
            try {
                if (getClass().getClassLoader().getResources(LIQUIBASE_RESOURCE).hasMoreElements()) {
                    this.logger.info("Execute liquibase changes from [{}] resource on database [{}]",
                        LIQUIBASE_RESOURCE, database);
                }
            } catch (IOException ignored) {
                // ignored
            }
        }
    }

    private void liquibaseUpdate(HibernateDataMigration migration, boolean preHibernate, String database)
        throws XWikiException, DataMigrationException
    {
        String liquibaseChangeLogs;
        if (preHibernate) {
            liquibaseChangeLogs = migration.getPreHibernateLiquibaseChangeLog();
        } else {
            liquibaseChangeLogs = migration.getLiquibaseChangeLog();
        }

        if (liquibaseChangeLogs == null || liquibaseChangeLogs.length() == 0) {
            return;
        }

        if (this.logger.isInfoEnabled()) {
            if (preHibernate) {
                this.logger.info("Running early schema updates (using liquibase) for migration [{}] on database [{}]",
                    migration.getName(), database);
            } else {
                this.logger.info(
                    "Running additional schema updates (using liquibase) for migration [{}] on database [{}]",
                    migration.getName(), database);
            }
        }

        liquibaseUpdate(liquibaseChangeLogs, database);
    }

    private void liquibaseUpdate(String liquibaseChangeLogs, String database)
        throws XWikiException, DataMigrationException
    {
        final StringBuilder changeLogs = new StringBuilder(10000);
        changeLogs.append(getLiquibaseChangeLogHeader());
        changeLogs.append(liquibaseChangeLogs);
        changeLogs.append(getLiquibaseChangeLogFooter());

        final XWikiHibernateBaseStore store = getBaseStore();

        store.executeRead(getXWikiContext(), new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws XWikiException
            {
                session.doWork(connection -> {
                    Liquibase lb;
                    try {
                        Database lbDatabase = DatabaseFactory.getInstance()
                            .findCorrectDatabaseImplementation(new JdbcConnection(connection));

                        // Precise the schema name to liquibase, since it does not usually determine it
                        // properly (See XWIKI-8813).
                        lbDatabase.setDefaultSchemaName(store.getSchemaFromWikiName(getXWikiContext()));

                        lb = new Liquibase(CHANGELOG_NAME,
                            new MockResourceAccessor(Map.of(CHANGELOG_NAME, changeLogs.toString())), lbDatabase);
                    } catch (LiquibaseException e) {
                        throw new HibernateException(new XWikiException(
                            XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MIGRATION, String
                                .format("Unable to launch liquibase for database %s, schema update failed.", database),
                            e));
                    }

                    try {
                        lb.update((Contexts) null);
                    } catch (LiquibaseException e) {
                        throw new HibernateException(new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                            XWikiException.ERROR_XWIKI_STORE_MIGRATION,
                            String.format("Unable to update schema of database %s.", database), e));
                    }
                });

                return null;
            }
        });
    }

    /**
     * @return the liquibase XML change log top level element opening with the XML declaration
     * @since 4.0M1
     */
    private String getLiquibaseChangeLogHeader()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<databaseChangeLog\n"
            + "    xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n"
            + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "    xmlns:ext=\"http://www.liquibase.org/xml/ns/dbchangelog-ext\"\n"
            + "    xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog "
            + "http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd\">";
    }

    /**
     * @return the liquibase change log top level element close tag
     * @since 4.0M1
     */
    private String getLiquibaseChangeLogFooter()
    {
        return "</databaseChangeLog>";
    }

    @Override
    protected void startMigrations() throws DataMigrationException
    {
        XWikiContext context = getXWikiContext();
        XWikiHibernateBaseStore store = getBaseStore();

        Session originalSession = store.getSession(context);
        Transaction originalTransaction = store.getTransaction(context);
        store.setSession(null, context);
        store.setTransaction(null, context);

        try {
            super.startMigrations();
        } finally {
            store.setSession(originalSession, context);
            store.setTransaction(originalTransaction, context);
        }
    }

    @Override
    protected List<? extends DataMigration> getAllMigrations() throws DataMigrationException
    {
        try {
            return this.componentManager.getInstanceList(HibernateDataMigration.class);
        } catch (ComponentLookupException e) {
            throw new DataMigrationException("Unable to retrieve the list of hibernate data migrations", e);
        }
    }
}
