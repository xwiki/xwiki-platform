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
package com.xpn.xwiki.store.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Template for {@link DataMigrationManager}.
 *
 * @version $Id$
 * @since 3.4M1
 */
public abstract class AbstractDataMigrationManager implements DataMigrationManager, Initializable
{
    /**
     * Component manager used to access stores and data migrations.
     */
    @Inject
    protected ComponentManager componentManager;

    /**
     * Component manager used to access stores and data migrations.
     */
    @Inject
    protected ObservationManager observationManager;

    /**
     * Ordered list of migrators that may be applied.
     */
    protected Collection<XWikiMigration> migrations;

    /**
     * Internal class used to find out the data migration that are being forced in the XWiki configuration file.
     */
    protected class XWikiMigration
    {
        /**
         * True for a forced migration.
         */
        public boolean isForced;

        /**
         * The data migration.
         */
        public DataMigration dataMigration;

        /**
         * Build a new XWikiMigration.
         * @param dataMigration the data migration
         * @param isForced true when this migration is forced
         */
        public XWikiMigration(DataMigration dataMigration, boolean isForced)
        {
            this.dataMigration = dataMigration;
            this.isForced = isForced;
        }
    }

    /**
     * Logger.
     */
    @Inject
    protected Logger logger;

    /**
     * Execution context used to access XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * Internal class used to prevent double checking of the database during migration operation.
     */
    private static class ThreadLock extends ThreadLocal<Integer>
    {
        @Override
        protected Integer initialValue()
        {
            return 0;
        }

        /**
         * Release the lock.
         */
        public void unlock()
        {
            int i = get();
            if (i > 0) {
                set(--i);
            }
        }

        /**
         * Acquire the lock.
         */
        public void lock()
        {
            set(get() + 1);
        }

        /**
         * Try to acquire the lock.
         * @return true if the lock has been acquired
         */
        public boolean tryLock()
        {
            int i = get();
            if (i > 0) {
                return false;
            }
            set(++i);
            return true;
        }
    }


    /**
     * Semaphore to prevent re-entrance.
     */
    private final ThreadLock lock = new ThreadLock();

    /**
     * Internal class used to store the migration status of the database.
     */
    private static class MigrationStatus implements DataMigrationStatus
    {
        /**
         * Current version of the database.
         */
        private XWikiDBVersion version;

        /**
         * True if the database has been migrated during the current run.
         */
        private Exception migrationException;

        /**
         * The exception produced by a migration failure, if any.
         */
        private boolean migrationAttempted;

        /**
         * Build a simple status with no migration attempted, just storing the current database version.
         * @param version the current database version
         */
        public MigrationStatus(XWikiDBVersion version)
        {
            this.version = version;
            migrationAttempted = false;
        }

        /**
         * Build a status following a migration storing the current database version and the exception resulting
         * from the migration. A null value for the exception means a successful migration.
         * @param version the current database version
         * @param migrationException the exception resulting from the migration.
         */
        public MigrationStatus(XWikiDBVersion version, Exception migrationException)
        {
            this.version = version;
            migrationAttempted = true;
            this.migrationException = migrationException;
        }

        @Override
        public XWikiDBVersion getDBVersion()
        {
            return version;
        }

        @Override
        public boolean hasDataMigrationBeenAttempted()
        {
            return migrationAttempted;
        }

        @Override
        public boolean hasBeenSuccessfullyMigrated()
        {
            return migrationAttempted && migrationException == null;
        }

        @Override
        public Exception getLastMigrationException()
        {
            return migrationException;
        }
    }

    /**
     * A cache of wiki database version.
     */
    private final Map<String, MigrationStatus> statusCache = new HashMap<String, MigrationStatus>();

    /**
     * The final database version when the migration process finishes.
     * This is use to compute the DBVersion of an empty store and quickly check the outdated status of existing DB
     */
    private XWikiDBVersion targetVersion;

    /**
     * Internal class used to clean the database version cache on wiki deletion.
     */
    private class WikiDeletedEventListener implements EventListener
    {
        @Override
        public String getName()
        {
            return "dbversioncache";
        }

        @Override
        public List<Event> getEvents()
        {
            return Arrays.<Event>asList(new WikiDeletedEvent());
        }

        @Override
        public void onEvent(Event event, Object source, Object data)
        {
            statusCache.remove(((WikiDeletedEvent) event).getWikiId());
        }
    }

    /**
     * Unified constructor for all subclasses.
     */
    public AbstractDataMigrationManager()
    {
    }

    /**
     * @return XWikiContext
     */
    protected XWikiContext getXWikiContext()
    {
        ExecutionContext context = execution.getContext();
        return (XWikiContext) context.getProperty("xwikicontext");
    }

    /**
     * @return XWikiConfig to read configuration from xwiki.cfg
     */
    protected XWikiConfig getXWikiConfig()
    {
        return getXWikiContext().getWiki().getConfig();
    }

    /**
     * @return true if running in virtual mode
     */
    protected boolean isVirtualMode()
    {
        return getXWikiContext().getWiki().isVirtualMode();
    }

    /**
     * @return list of virtual database names
     * @throws DataMigrationException on error
     */
    protected List<String> getVirtualWikisDatabaseNames() throws DataMigrationException
    {
        try {
            return getXWikiContext().getWiki().getVirtualWikisDatabaseNames(getXWikiContext());
        } catch (XWikiException e) {
            throw new DataMigrationException("Unable to retrieve the list of wiki names", e);
        }
    }

    /**
     * @return the main XWiki database name
     */
    protected String getMainXWiki()
    {
        return getXWikiContext().getMainXWiki();
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            SortedMap<XWikiDBVersion, XWikiMigration> availableMigrations
                = new TreeMap<XWikiDBVersion, XWikiMigration>();

            Map<XWikiDBVersion, XWikiMigration> forcedMigrations = getForcedMigrations();
            if (!forcedMigrations.isEmpty()) {
                availableMigrations.putAll(forcedMigrations);
            } else {
                Set<String> ignoredMigrations = new HashSet<String>(Arrays.asList(getXWikiConfig()
                    .getPropertyAsList("xwiki.store.migration.ignored")));
                for (DataMigration migrator : getAllMigrations()) {
                    if (ignoredMigrations.contains(migrator.getClass().getName())
                        || ignoredMigrations.contains(migrator.getVersion().toString()))
                    {
                        continue;
                    }
                    XWikiMigration migration = new XWikiMigration(migrator, false);
                    availableMigrations.put(migrator.getVersion(), migration);
                }
            }

            this.targetVersion = (availableMigrations.size() > 0) ? availableMigrations.lastKey()
                                                                  : new XWikiDBVersion(0);
            this.migrations =  availableMigrations.values();
        } catch (Exception e) {
            throw new InitializationException("Migration Manager initialization failed", e);
        }

        observationManager.addListener(new WikiDeletedEventListener());
    }

    /**
     * read data version from xwiki.cfg.
     *
     * @return data version if set, or null.
     */
    protected XWikiDBVersion getDBVersionFromConfig()
    {
        String ver = getXWikiConfig().getProperty("xwiki.store.migration.version");
        return ver == null ? null : new XWikiDBVersion(Integer.parseInt(ver));
    }

    /**
     * Read data version from database.
     * @return data version or null if this is a new database
     * @throws DataMigrationException in case of an unexpected error
     */
    protected XWikiDBVersion getDBVersionFromDatabase() throws DataMigrationException
    {
        return getDBVersionFromConfig();
    }

    @Override
    public final XWikiDBVersion getDBVersion() throws DataMigrationException
    {
        lock.lock();
        try {
            String wikiName = getXWikiContext().getDatabase();
            MigrationStatus dbStatus = this.statusCache.get(wikiName);
            if (dbStatus == null) {
                synchronized (this.statusCache) {
                    XWikiDBVersion version = getDBVersionFromDatabase();
                    if (version != null) {
                        this.statusCache.put(wikiName, new MigrationStatus(version));
                    }
                    return version;
                }
            }
            return dbStatus.getDBVersion();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DataMigrationStatus getDataMigrationStatus() throws DataMigrationException
    {
        lock.lock();
        try {
            String wikiName = getXWikiContext().getDatabase();
            MigrationStatus dbStatus = this.statusCache.get(wikiName);
            if (dbStatus == null) {
                synchronized (this.statusCache) {
                    XWikiDBVersion version = getDBVersionFromDatabase();
                    if (version != null) {
                        dbStatus = new MigrationStatus(version);
                        this.statusCache.put(wikiName, dbStatus);
                    }
                }
            }
            return dbStatus;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final XWikiDBVersion getLatestVersion()
    {
        return this.targetVersion;
    }

    @Override
    public synchronized void initNewDB() throws DataMigrationException
    {
        lock.lock();
        try {
            initializeEmptyDB();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @throws DataMigrationException if any error
     */
    protected abstract void initializeEmptyDB() throws DataMigrationException;

    /**
     * @param version to set
     * @throws DataMigrationException if any error
     */
    protected abstract void setDBVersionToDatabase(XWikiDBVersion version) throws DataMigrationException;

    /**
     * Update database version and status cache (not after a migration, use updateMigrationStatus).
     * @param version database version to be stored
     * @throws DataMigrationException if any error
     */
    protected final void setDBVersion(XWikiDBVersion version) throws DataMigrationException
    {
        updateMigrationStatus(version, false, null);
    }

    /**
     * Update database version and status cache after a successful migration.
     * @param version new database version
     * @throws DataMigrationException if any error
     */
    private void updateMigrationStatus(XWikiDBVersion version) throws DataMigrationException
    {
        updateMigrationStatus(version, true, null);
    }

    /**
     * Update status cache based on last migration failure.
     * @param version current database version (last valid version)
     * @param e exception thrown by the migration
     * @throws DataMigrationException if any error
     */
    private void updateMigrationStatus(XWikiDBVersion version, Exception e) throws DataMigrationException
    {
        updateMigrationStatus(version, true, e);
    }

    /**
     * Update database version and status cache based on the new migration status.
     * @param version current or new database version
     * @param migrationAttempted true if this update is the result of a migration process
     * @param e exception thrown by the last migration or null if the migration was successful
     * @throws DataMigrationException if any error
     */
    private synchronized void updateMigrationStatus(XWikiDBVersion version, boolean migrationAttempted, Exception e)
        throws DataMigrationException
    {
        String wikiName = getXWikiContext().getDatabase();
        if (!migrationAttempted || e == null) {
            setDBVersionToDatabase(version);
        }
        if (version != null) {
            this.statusCache.put(wikiName,
                (migrationAttempted) ? new MigrationStatus(version, e) : new MigrationStatus(version));
        }
    }

    /**
     * Update database schema to the latest structure.
     * @param migrations the migration that will be executed (since 4.0M1)
     * @throws DataMigrationException if any error
     */
    protected abstract void updateSchema(Collection<XWikiMigration> migrations) throws DataMigrationException;

    @Override
    public void checkDatabase() throws MigrationRequiredException, DataMigrationException
    {
        if (!lock.tryLock()) {
            return;
        }
        try {
            DataMigrationStatus status;

            // Retrieve DB version
            try {
                status = getDataMigrationStatus();
            } catch (DataMigrationException e) {
                String message = String.format(
                    "Database %s seems to be inaccessible, please check your configuration!",
                    getXWikiContext().getDatabase());
                logger.error(message, e);
                throw new DataMigrationException(message, e);
            }

            // Initialize new DB
            if (status == null) {
                try {
                    initNewDB();
                } catch (DataMigrationException e) {
                    String message = String.format(
                        "The empty database %s seems to be not writable, please check your configuration!",
                        getXWikiContext().getDatabase());
                    logger.error(message, e);
                    throw new DataMigrationException(message, e);
                }
            }

            // Proceed with migration (only once)
            if (this.migrations != null) {
                try {
                    XWikiConfig config = getXWikiConfig();
                    if ("1".equals(config.getProperty("xwiki.store.migration", "0"))
                        && !"0".equals(config.getProperty("xwiki.store.hibernate.updateschema"))) {
                        // Run migrations
                        logger.info("Storage schema updates and data migrations are enabled");

                        startMigrations();

                        // TODO: Improve or remove this which is inappropriate in a container environment
                        if ("1".equals(config.getProperty("xwiki.store.migration.exitAfterEnd", "0"))) {
                            logger.error("Exiting because xwiki.store.migration.exitAfterEnd is set");
                            System.exit(0);
                        }
                    }
                } finally {
                    // data migration are no more needed, migration only happen once
                    this.migrations = null;
                }
            }

            status = getDataMigrationStatus();

            // Prevent access to outdated DB
            if (getLatestVersion().compareTo(status.getDBVersion()) > 0) {
                if (status.hasDataMigrationBeenAttempted() && !status.hasBeenSuccessfullyMigrated()) {
                    String message = String.format(
                        "Migration of database [%s] has failed, it could not be safely used! Database is currently in"
                            + " version [%d] while the required version is [%d].",
                        getXWikiContext().getDatabase(),
                        status.getDBVersion().getVersion(),
                        getLatestVersion().getVersion());
                    throw new DataMigrationException(message, status.getLastMigrationException());
                } else {
                    String message = String.format(
                        "Since database [%s] needs to be migrated, it couldn't be safely used! Please check your"
                            + " configuration to enable required migration for upgrading database from version [%d]"
                            + " to version [%d].",
                        getXWikiContext().getDatabase(),
                        status.getDBVersion().getVersion(),
                        getLatestVersion().getVersion());
                    throw new MigrationRequiredException(message);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Start the migration process.
     *
     * @throws DataMigrationException in case of any error
     */
    protected synchronized void startMigrations() throws DataMigrationException
    {
        if (this.migrations == null) {
            return;
        }

        XWikiContext context = getXWikiContext();

        if (isVirtualMode()) {
            // Save context values so that we can restore them as they were before the migration.
            String currentDatabase = context.getDatabase();
            String currentOriginalDatabase = context.getOriginalDatabase();

            int errorCount = 0;
            try {
                for (String database : getDatabasesToMigrate()) {
                    // Set up the context so that it points to the virtual wiki corresponding to the
                    // database.
                    context.setDatabase(database);
                    context.setOriginalDatabase(database);
                    try {
                        startMigrationsForDatabase();
                    } catch (DataMigrationException e) {
                        errorCount++;
                    }
                }
                if (errorCount > 0) {
                    String message = String.format(
                        "%s database migration(s) failed, it is not safe to continue!", errorCount);
                    logger.error(message);
                    throw new DataMigrationException(message);
                }
            } finally {
                context.setDatabase(currentDatabase);
                context.setOriginalDatabase(currentOriginalDatabase);
            }
        } else {
            // Just update schema and migrate the main wiki
            try {
                startMigrationsForDatabase();
            } catch (DataMigrationException ex) {
                String message = "Main database migration failed, it is not safe to continue!";
                logger.error(message, ex);
                throw new DataMigrationException(message, ex);
            }
        }
    }

    /**
     * Returns the names of the databases that should be migrated.
     * This is controlled through the "xwiki.store.migration.databases" configuration property in xwiki.cfg.
     * A value of "all" or no value at all will add all databases. Note that the main database is automatically added
     * even if not specified.
     *
     * @return The names of all databases to migrate.
     * @throws DataMigrationException if the list of wikis cannot be obtained.
     */
    private Set<String> getDatabasesToMigrate() throws DataMigrationException
    {
        Set<String> databasesToMigrate = new LinkedHashSet<String>();

        // Always migrate the main database. We also want this to be the first database migrated so
        // it has to be the
        // first returned in the list.
        databasesToMigrate.add(getMainXWiki());

        // Add the databases listed by the user (if any). If there's no database name or
        // a single database named and if it's "all" or "ALL" then automatically add all the registered databases.
        if (isVirtualMode()) {
            String[] databases =
                getXWikiConfig().getPropertyAsList("xwiki.store.migration.databases");
            if ((databases.length == 0) || ((databases.length == 1) && databases[0].equalsIgnoreCase("all"))) {
                databasesToMigrate.addAll(getVirtualWikisDatabaseNames());
            } else {
                Collections.addAll(databasesToMigrate, databases);
            }
        }

        return databasesToMigrate;
    }

    /**
     * It is assumed that before calling this method the XWiki context has been set with the
     * database to migrate.
     *
     * @throws DataMigrationException if there is an error updating the database.
     */
    private void startMigrationsForDatabase() throws DataMigrationException
    {
        try {
            Collection<XWikiMigration> neededMigrations = getNeededMigrations();
            updateSchema(neededMigrations);
            startMigrations(neededMigrations);
        } catch (Exception e) {
            updateMigrationStatus(getDBVersion(), e);
            String message = String.format("Failed to migrate database [%s]...", getXWikiContext().getDatabase());
            logger.info(message, e);
            throw new DataMigrationException(message, e);
        }
    }

    /**
     * @return collection of {@link DataMigration} in ascending order, which need be
     *         executed.
     * @throws DataMigrationException if any error
     */
    protected Collection<XWikiMigration> getNeededMigrations() throws DataMigrationException
    {
        XWikiDBVersion curversion = getDBVersion();
        Collection<XWikiMigration> neededMigrations = new ArrayList<XWikiMigration>();
        String database = null;
        if (logger.isInfoEnabled()) {
            database = getXWikiContext().getDatabase();
        }

        for (XWikiMigration migration : this.migrations) {
            if (migration.isForced || (migration.dataMigration.getVersion().compareTo(curversion) > 0
                                        && migration.dataMigration.shouldExecute(curversion)))
            {
                neededMigrations.add(migration);
            }
        }

        if (logger.isInfoEnabled()) {
            if (!neededMigrations.isEmpty()) {
                logger.info(
                    "The following data migration(s) will be applied for database [{}] currently in version [{}]:",
                    database, curversion);
                for (XWikiMigration migration : neededMigrations) {
                    logger.info("  {} - {}{}", new String[] {migration.dataMigration.getName(),
                        migration.dataMigration.getDescription(),
                        (migration.isForced ? " (forced)" : "")});
                }
            } else {
                if (curversion != null) {
                    logger.info("No data migration to apply for database [{}] currently in version [{}]",
                        database, curversion);
                } else {
                    logger.info("No data migration to apply for empty database [{}]",
                        database);
                }
            }
        }

        return neededMigrations;
    }

    /**
     * @return a map of forced {@link DataMigration} for this manager
     * @throws DataMigrationException id any error
     */
    protected Map<XWikiDBVersion, XWikiMigration> getForcedMigrations() throws DataMigrationException
    {
        SortedMap<XWikiDBVersion, XWikiMigration> forcedMigrations = new TreeMap<XWikiDBVersion, XWikiMigration>();
        for (String hint : getXWikiConfig().getPropertyAsList("xwiki.store.migration.force")) {
            try {
                DataMigration dataMigration = componentManager.getInstance(DataMigration.class, hint);
                forcedMigrations.put(dataMigration.getVersion(), new XWikiMigration(dataMigration, true));
            } catch (ComponentLookupException e) {
                throw new DataMigrationException("Forced dataMigration " + hint + " component could not be found", e);
            }
        }
        return forcedMigrations;
    }

    /**
     * @param migrations - run this migrations in order of collection
     * @throws DataMigrationException if any error
     */
    protected void startMigrations(Collection<XWikiMigration> migrations) throws DataMigrationException
    {
        XWikiDBVersion curversion = getDBVersion();
        String database = null;
        if (logger.isInfoEnabled()) {
            database = getXWikiContext().getDatabase();
        }

        for (XWikiMigration migration : migrations) {
            if (logger.isInfoEnabled()) {
                logger.info("Starting data migration [{}] with version [{}] on database [{}]",
                    migration.dataMigration.getName(), migration.dataMigration.getVersion(), database);
            }

            migration.dataMigration.migrate();

            if (migration.dataMigration.getVersion().compareTo(curversion) > 0) {
                curversion = migration.dataMigration.getVersion();
                updateMigrationStatus(curversion);
                if (logger.isInfoEnabled()) {
                    logger.info("Data migration [{}] applied successfully, database [{}] upgraded to version [{}]",
                        migration.dataMigration.getName(), database, getDBVersion());
                }
            } else if (logger.isInfoEnabled()) {
                logger.info("Data migration [{}] applied successfully, database [{}] stay in version [{}]",
                    migration.dataMigration.getName(), database, getDBVersion());
            }
        }

        // If migration is launch on an empty DB or latest migration was unneeded, properly set the latest DB version
        if (curversion == null) {
            setDBVersion(getLatestVersion());
        } else if (getLatestVersion().compareTo(curversion) > 0) {
            updateMigrationStatus(getLatestVersion());
            if (logger.isInfoEnabled() && curversion != null) {
                logger.info("Database [{}] upgraded to latest version [{}] without needing{} data migration",
                    database, getDBVersion(), (migrations.size() > 0) ? " further" : "");
            }
        }
    }

    /**
     * @return List of all {@link DataMigration} for this manager
     * @throws DataMigrationException if any error
     */
    protected abstract List<? extends DataMigration> getAllMigrations() throws DataMigrationException;
}
