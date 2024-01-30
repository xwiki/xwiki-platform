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
import java.util.HashMap;
import java.util.HashSet;
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
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

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

    @Inject
    protected JobProgressManager progress;

    @Inject
    protected HibernateConfiguration hibernateConfiguration;

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
         *
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
    private static final class ThreadLock extends ThreadLocal<Integer>
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
         *
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
         *
         * @param version the current database version
         */
        public MigrationStatus(XWikiDBVersion version)
        {
            this.version = version;
            this.migrationAttempted = false;
        }

        /**
         * Build a status following a migration storing the current database version and the exception resulting from
         * the migration. A null value for the exception means a successful migration.
         *
         * @param version the current database version
         * @param migrationException the exception resulting from the migration.
         */
        public MigrationStatus(XWikiDBVersion version, Exception migrationException)
        {
            this.version = version;
            this.migrationAttempted = true;
            this.migrationException = migrationException;
        }

        @Override
        public XWikiDBVersion getDBVersion()
        {
            return this.version;
        }

        @Override
        public boolean hasDataMigrationBeenAttempted()
        {
            return this.migrationAttempted;
        }

        @Override
        public boolean hasBeenSuccessfullyMigrated()
        {
            return this.migrationAttempted && this.migrationException == null;
        }

        @Override
        public Exception getLastMigrationException()
        {
            return this.migrationException;
        }
    }

    /**
     * A cache of wiki database version.
     */
    private final Map<String, MigrationStatus> statusCache = new HashMap<>();

    /**
     * The final database version when the migration process finishes. This is use to compute the DBVersion of an empty
     * store and quickly check the outdated status of existing DB.
     */
    private XWikiDBVersion targetVersion;

    /**
     * Internal class used to clean the database version cache on wiki deletion.
     */
    private final class WikiDeletedEventListener implements EventListener
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
            AbstractDataMigrationManager.this.statusCache.remove(((WikiDeletedEvent) event).getWikiId());
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
        ExecutionContext context = this.execution.getContext();
        return (XWikiContext) context.getProperty("xwikicontext");
    }

    /**
     * @return XWikiConfig to read configuration from xwiki.cfg
     * @deprecated
     */
    @Deprecated
    protected XWikiConfig getXWikiConfig()
    {
        return getXWikiContext().getWiki().getConfig();
    }

    /**
     * @deprecated Virtual mode is on by default, starting with XWiki 5.0M2.
     * @return true if running in virtual mode
     */
    @Deprecated
    protected boolean isVirtualMode()
    {
        return true;
    }

    /**
     * @return list of virtual database names
     * @throws DataMigrationException on error
     */
    @Deprecated
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
        checkMigrationsVersions();
        try {
            SortedMap<XWikiDBVersion, XWikiMigration> availableMigrations = new TreeMap<>();
            Map<XWikiDBVersion, XWikiMigration> forcedMigrations = getForcedMigrations();
            if (!forcedMigrations.isEmpty()) {
                availableMigrations.putAll(forcedMigrations);
            } else {
                Set<String> ignoredMigrations = new HashSet<>(this.hibernateConfiguration.getIgnoredMigrations());
                for (DataMigration migrator : getAllMigrations()) {
                    XWikiDBVersion migratorVersion = migrator.getVersion();
                    if (isMigrationIgnored(migrator, ignoredMigrations)) {
                        continue;
                    }
                    XWikiMigration migration = new XWikiMigration(migrator, false);
                    availableMigrations.put(migratorVersion, migration);
                }
            }

            this.targetVersion = !availableMigrations.isEmpty() ? availableMigrations.lastKey() : new XWikiDBVersion(0);
            this.migrations = availableMigrations.values();
        } catch (Exception e) {
            throw new InitializationException("Migration Manager initialization failed", e);
        }

        this.observationManager.addListener(new WikiDeletedEventListener());
    }

    public boolean isMigrationIgnored(DataMigration migration, Set<String> ignoredMigrations)
    {
        return ignoredMigrations.contains(migration.getClass().getName())
            || ignoredMigrations.contains(migration.getVersion().toString());
    }

    /**
     * Ensure we don't have two migrations with same DB version.
     * Note that it's possible to use the ignored migrations feature to bypass this check: if two migrations has same
     * version one can ignore them in which case there will be only a warning.
     *
     * @throws InitializationException in case we found same DB version or the list of migration cannot be obtained.
     */
    private void checkMigrationsVersions() throws InitializationException
    {
        try {
            List<HibernateDataMigration> migrationList =
                this.componentManager.getInstanceList(HibernateDataMigration.class);
            Set<String> ignoredMigrations = new HashSet<>(this.hibernateConfiguration.getIgnoredMigrations());
            Map<XWikiDBVersion, String> hintMap = new HashMap<>();
            for (HibernateDataMigration dataMigration : migrationList) {
                XWikiDBVersion version = dataMigration.getVersion();
                if (hintMap.containsKey(version)) {
                    if (isMigrationIgnored(dataMigration, ignoredMigrations)) {
                        this.logger.warn("Two migrations with same version [{}] were found: [{}] and [{}] but "
                            + "migration [{}] is ignored.",
                            version,
                            hintMap.get(version),
                            dataMigration.getClass().getName(),
                            dataMigration.getClass().getName());
                    } else {
                        throw new InitializationException(
                            String.format("Two migrations with same version [%s] were found: [%s] and [%s]",
                                version,
                                hintMap.get(version),
                                dataMigration.getClass().getName()));
                    }
                }
                hintMap.put(version, dataMigration.getClass().getName());
            }
        } catch (ComponentLookupException e) {
            throw new InitializationException("Unable to retrieve the list of hibernate data migrations", e);
        }
    }

    /**
     * read data version from xwiki.cfg.
     *
     * @return data version if set, or null.
     */
    protected XWikiDBVersion getDBVersionFromConfig()
    {
        String ver = this.hibernateConfiguration.getMigrationVersion();
        return ver == null ? null : new XWikiDBVersion(Integer.parseInt(ver));
    }

    /**
     * Read data version from database.
     *
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
        this.lock.lock();
        try {
            String wikiName = getXWikiContext().getWikiId();
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
            this.lock.unlock();
        }
    }

    @Override
    public DataMigrationStatus getDataMigrationStatus() throws DataMigrationException
    {
        this.lock.lock();
        try {
            String wikiName = getXWikiContext().getWikiId();
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
            this.lock.unlock();
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
        this.lock.lock();
        try {
            initializeEmptyDB();
        } finally {
            this.lock.unlock();
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
     *
     * @param version database version to be stored
     * @throws DataMigrationException if any error
     */
    protected final void setDBVersion(XWikiDBVersion version) throws DataMigrationException
    {
        updateMigrationStatus(version, false, null);
    }

    /**
     * Update database version and status cache after a successful migration.
     *
     * @param version new database version
     * @throws DataMigrationException if any error
     */
    private void updateMigrationStatus(XWikiDBVersion version) throws DataMigrationException
    {
        updateMigrationStatus(version, true, null);
    }

    /**
     * Update status cache based on last migration failure.
     *
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
     *
     * @param version current or new database version
     * @param migrationAttempted true if this update is the result of a migration process
     * @param e exception thrown by the last migration or null if the migration was successful
     * @throws DataMigrationException if any error
     */
    private synchronized void updateMigrationStatus(XWikiDBVersion version, boolean migrationAttempted, Exception e)
        throws DataMigrationException
    {
        String wikiName = getXWikiContext().getWikiId();
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
     *
     * @param migrations the migration that will be executed (since 4.0M1)
     * @throws DataMigrationException if any error
     */
    protected abstract void updateSchema(Collection<XWikiMigration> migrations) throws DataMigrationException;

    @Override
    public void checkDatabase() throws MigrationRequiredException, DataMigrationException
    {
        if (!this.lock.tryLock()) {
            return;
        }
        try {
            if (getDatabaseStatus() == null) {
                initializeCurrentDatabase();
            }

            // Proceed with migration (only once)
            if (this.migrations != null) {
                tryToProcceedToMigration();
            }

            preventAccessToOutdatedDb();
        } finally {
            this.lock.unlock();
        }
    }

    private void initializeCurrentDatabase() throws DataMigrationException
    {
        try {
            initNewDB();
        } catch (DataMigrationException e) {
            String message =
                String.format("The empty database %s seems to be not writable, please check your configuration!",
                    getXWikiContext().getWikiId());
            this.logger.error(message, e);
            throw new DataMigrationException(message, e);
        }
    }

    private DataMigrationStatus getDatabaseStatus() throws DataMigrationException
    {
        DataMigrationStatus status;
        try {
            status = getDataMigrationStatus();
        } catch (DataMigrationException e) {
            String message = String.format("Database %s seems to be inaccessible, please check your configuration!",
                getXWikiContext().getWikiId());
            this.logger.error(message, e);
            throw new DataMigrationException(message, e);
        }
        return status;
    }

    /**
     * Check database status, and throws if access should not be allowed.
     *
     * @throws DataMigrationException when a database has failed to migrate.
     * @throws MigrationRequiredException when a database has not been migrated and require migration.
     */
    private void preventAccessToOutdatedDb() throws DataMigrationException, MigrationRequiredException
    {
        DataMigrationStatus status = getDataMigrationStatus();

        if (getLatestVersion().compareTo(status.getDBVersion()) > 0) {
            if (status.hasDataMigrationBeenAttempted() && !status.hasBeenSuccessfullyMigrated()) {
                String message = String.format(
                    "Migration of database [%s] has failed, it could not be safely used! Database is currently in"
                        + " version [%d] while the required version is [%d].",
                    getXWikiContext().getWikiId(), status.getDBVersion().getVersion(), getLatestVersion().getVersion());
                throw new DataMigrationException(message, status.getLastMigrationException());
            } else {
                String message = String.format(
                    "Since database [%s] needs to be migrated, it couldn't be safely used! Please check your"
                        + " configuration to enable required migration for upgrading database from version [%d]"
                        + " to version [%d].",
                    getXWikiContext().getWikiId(), status.getDBVersion().getVersion(), getLatestVersion().getVersion());
                throw new MigrationRequiredException(message);
            }
        }
    }

    /**
     * Start migrations if migrations are enabled.
     *
     * @throws DataMigrationException
     */
    private void tryToProcceedToMigration() throws DataMigrationException
    {
        if (this.hibernateConfiguration.isMigrationEnabled()) {
            // Run migrations
            this.logger.info("Storage schema updates and data migrations are enabled");

            startMigrationsOnlyOnce();

            // TODO: Improve or remove this which is inappropriate in a container environment
            if (this.hibernateConfiguration.isExitAfterMigration()) {
                this.logger.error("Exiting because xwiki.store.migration.exitAfterEnd is set");
                System.exit(0);
            }
        }
    }

    /**
     * Start the migration process only once by synchronization and semaphore.
     *
     * @throws DataMigrationException
     */
    private synchronized void startMigrationsOnlyOnce() throws DataMigrationException
    {
        // migrations hold available migrations and is used like a semaphore to avoid multiple run
        if (this.migrations == null) {
            return;
        }

        try {
            startMigrations();
        } finally {
            this.migrations = null;
        }
    }

    /**
     * Start the migration process. This one is not thread safe and should be synchronized. The migrations field should
     * not be null.
     *
     * @throws DataMigrationException in case of any error
     */
    protected void startMigrations() throws DataMigrationException
    {
        Set<String> databasesToMigrate = getDatabasesToMigrate();

        this.progress.pushLevelProgress(databasesToMigrate.size(), this);

        try {
            // We should migrate the main wiki first to be able to access subwiki descriptors if needed.
            if (!migrateDatabase(getMainXWiki())) {
                String message = "Main wiki database migration failed, it is not safe to continue!";
                this.logger.error(message);
                throw new DataMigrationException(message);
            }

            int errorCount = 0;
            for (String database : databasesToMigrate) {
                this.progress.startStep(this);

                if (!migrateDatabase(database)) {
                    errorCount++;
                }

                this.progress.endStep(this);
            }

            if (errorCount > 0) {
                String message = String.format("%s wiki database migration(s) failed.", errorCount);
                this.logger.error(message);
                throw new DataMigrationException(message);
            }
        } finally {
            this.progress.popLevelProgress(this);
        }
    }

    /**
     * Returns the names of the databases that should be migrated. The main wiki database should have been migrated and
     * is never returned. This is controlled through the "xwiki.store.migration.databases" configuration property in
     * xwiki.cfg. A value of "all" or no value at all will add all databases. Note that the main database is
     * automatically added even if not specified.
     *
     * @return The names of all databases to migrate.
     * @throws DataMigrationException if the list of wikis cannot be obtained.
     */
    private Set<String> getDatabasesToMigrate() throws DataMigrationException
    {
        Set<String> databasesToMigrate = new HashSet<>();

        // Add the databases listed by the user (if any). If there's no database name or
        // a single database named and if it's "all" or "ALL" then automatically add all the registered databases.
        List<String> databases = this.hibernateConfiguration.getMigrationDatabases();
        if (databases.isEmpty() || (databases.size() == 1 && databases.get(0).equals("all"))) {
            // The main wiki will also be included, but, since we are using a Set, it should not be a problem.
            List<String> allwikis = getVirtualWikisDatabaseNames();
            databasesToMigrate.addAll(allwikis);
        } else {
            databasesToMigrate.addAll(databases);
        }

        // Remove the main wiki if listed since it should have already been migrated.
        databasesToMigrate.remove(getMainXWiki());

        return databasesToMigrate;
    }

    /**
     * Migrate a given database and log error appropriately.
     *
     * @param database name of the database to migrate.
     * @return false if there is an error updating the database.
     */
    private boolean migrateDatabase(String database)
    {
        XWikiContext context = getXWikiContext();

        // Save context values so that we can restore them as they were before the migration.
        String currentDatabase = context.getWikiId();
        String currentOriginalDatabase = context.getOriginalWikiId();

        try {
            // Set up the context so that it points to the virtual wiki corresponding to the
            // database.
            context.setWikiId(database);
            context.setOriginalWikiId(database);

            Collection<XWikiMigration> neededMigrations = getNeededMigrations();
            updateSchema(neededMigrations);
            startMigrations(neededMigrations);
        } catch (Exception e) {
            try {
                updateMigrationStatus(getDBVersion(), e);
            } catch (DataMigrationException e1) {
                // Should not happen and could be safely ignored.
            }
            String message = String.format("Failed to migrate database [%s]...", database);
            this.logger.error(message, e);
            return false;
        } finally {
            context.setWikiId(currentDatabase);
            context.setOriginalWikiId(currentOriginalDatabase);
        }
        return true;
    }

    /**
     * @return collection of {@link DataMigration} in ascending order, which need be executed.
     * @throws DataMigrationException if any error
     */
    protected Collection<XWikiMigration> getNeededMigrations() throws DataMigrationException
    {
        XWikiDBVersion curversion = getDBVersion();
        Collection<XWikiMigration> neededMigrations = new ArrayList<>();

        for (XWikiMigration migration : this.migrations) {
            if (migration.isForced || (migration.dataMigration.getVersion().compareTo(curversion) > 0
                && migration.dataMigration.shouldExecute(curversion))) {
                neededMigrations.add(migration);
            }
        }

        if (this.logger.isInfoEnabled()) {
            logNeededMigrationReport(curversion, neededMigrations);
        }

        return neededMigrations;
    }

    private void logNeededMigrationReport(XWikiDBVersion curversion, Collection<XWikiMigration> neededMigrations)
    {
        String database = getXWikiContext().getWikiId();
        if (!neededMigrations.isEmpty()) {
            this.logger.info("The following data migration(s) will be applied for wiki [{}] currently in version [{}]:",
                database, curversion);
            for (XWikiMigration migration : neededMigrations) {
                this.logger.info("  {} - {}{}", migration.dataMigration.getName(),
                    migration.dataMigration.getDescription(), (migration.isForced ? " (forced)" : ""));
            }
        } else {
            if (curversion != null) {
                this.logger.info("No data migration to apply for wiki [{}] currently in version [{}]", database,
                    curversion);
            } else {
                this.logger.info("No data migration to apply for empty wiki [{}]", database);
            }
        }
    }

    /**
     * @return a map of forced {@link DataMigration} for this manager
     * @throws DataMigrationException id any error
     */
    protected Map<XWikiDBVersion, XWikiMigration> getForcedMigrations() throws DataMigrationException
    {
        SortedMap<XWikiDBVersion, XWikiMigration> forcedMigrations = new TreeMap<>();
        for (String hint : this.hibernateConfiguration.getForcedMigrations()) {
            try {
                DataMigration dataMigration = this.componentManager.getInstance(DataMigration.class, hint);
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
        if (this.logger.isInfoEnabled()) {
            database = getXWikiContext().getWikiId();
        }

        this.progress.pushLevelProgress(migrations.size(), this);

        try {
            for (XWikiMigration migration : migrations) {
                this.progress.startStep(this);

                if (this.logger.isInfoEnabled()) {
                    this.logger.info("Starting data migration [{}] with version [{}] on database [{}]",
                        migration.dataMigration.getName(), migration.dataMigration.getVersion(), database);
                }

                migration.dataMigration.migrate();

                if (migration.dataMigration.getVersion().compareTo(curversion) > 0) {
                    curversion = migration.dataMigration.getVersion();
                    updateMigrationStatus(curversion);
                    if (this.logger.isInfoEnabled()) {
                        this.logger.info(
                            "Data migration [{}] applied successfully, database [{}] upgraded to version [{}]",
                            migration.dataMigration.getName(), database, getDBVersion());
                    }
                } else if (this.logger.isInfoEnabled()) {
                    this.logger.info("Data migration [{}] applied successfully, database [{}] stay in version [{}]",
                        migration.dataMigration.getName(), database, getDBVersion());
                }

                this.progress.endStep(this);
            }
        } finally {
            this.progress.popLevelProgress(this);
        }

        // If migration is launch on an empty DB or latest migration was unneeded, properly set the latest DB version
        setDatabaseToLastestVersion(curversion);
    }

    /**
     * Set the database to the latest version when migration has all been processed. If migration is launch on an empty
     * DB or latest migration was unneeded, this method ensure that the database is properly set the latest DB version.
     *
     * @param currentVersion the current database version
     * @throws DataMigrationException if the version update fails
     */
    private void setDatabaseToLastestVersion(XWikiDBVersion currentVersion) throws DataMigrationException
    {
        if (currentVersion == null) {
            setDBVersion(getLatestVersion());
        } else if (getLatestVersion().compareTo(currentVersion) > 0) {
            updateMigrationStatus(getLatestVersion());
            if (this.logger.isInfoEnabled()) {
                this.logger.info("Database [{}] upgraded to latest version [{}] without needing{} data migration",
                    getXWikiContext().getWikiId(), getDBVersion(), (this.migrations.size() > 0) ? " further" : "");
            }
        }
    }

    /**
     * @return List of all {@link DataMigration} for this manager
     * @throws DataMigrationException if any error
     */
    protected abstract List<? extends DataMigration> getAllMigrations() throws DataMigrationException;
}
