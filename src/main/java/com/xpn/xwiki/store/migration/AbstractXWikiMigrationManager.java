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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Template for {@link XWikiMigrationManagerInterface}.
 * @version $Id: $
 */
public abstract class AbstractXWikiMigrationManager implements XWikiMigrationManagerInterface
{
    /** logger. */
    private static final Log LOG = LogFactory.getLog(AbstractXWikiMigrationManager.class);
    /**
     * Unified constructor for all subclasses.
     * @param context - used everywhere
     */
    public AbstractXWikiMigrationManager(XWikiContext context) { }
    /**
     * read data version from xwiki.cfg.
     * @param context used for read config
     * @return data version if set, or null.
     */
    protected XWikiDBVersion getDBVersionFromConfig(XWikiContext context)
    {
        String ver = context.getWiki().getConfig().getProperty("xwiki.store.migration.version");
        return ver == null ? null : new XWikiDBVersion(Integer.parseInt(ver));
    }
    /**
     * {@inheritDoc}
     */
    public XWikiDBVersion getDBVersion(XWikiContext context) throws XWikiException
    {
        XWikiDBVersion result = getDBVersionFromConfig(context);
        return result == null ? new XWikiDBVersion(0) : result;
    }
    /**
     * @param version to set
     * @param context used everywhere
     * @throws XWikiException if any error
     */
    protected abstract void setDBVersion(XWikiDBVersion version, XWikiContext context)
        throws XWikiException;

    /**
     * {@inheritDoc}
     */
    public void startMigrations(XWikiContext context) throws XWikiException
    {
        if (context.getWiki().isVirtual()) {
            // Save context values so that we can restore them as they were before the migration.
            boolean currentIsVirtual = context.isVirtual();
            String currentDatabase = context.getDatabase();
            String currentOriginalDatabase = context.getOriginalDatabase();

            try {
                for (Iterator it = getDatabasesToMigrate(context).iterator(); it.hasNext();) {
                    String database = (String) it.next();
                    LOG.info("Starting migration for database [" + database + "]...");
                    // Set up the context so that it points to the virtual wiki corresponding to the database.
                    context.setVirtual(true);
                    context.setDatabase(database);
                    context.setOriginalDatabase(database);
                    try {
                        startMigrationsForDatabase(context);
                    } catch (XWikiException e) {
                        LOG.info("Failed to migrate database [" + database + "]...", e);
                    }
                }
            } finally {
                context.setVirtual(currentIsVirtual);
                context.setDatabase(currentDatabase);
                context.setOriginalDatabase(currentOriginalDatabase);
            }
        } else {
            // Just migrate the main wiki
            startMigrationsForDatabase(context);
        }
    }

    /**
     * @return the names of all databases to migrate. This is controlled through the "xwiki.store.migration.databases"
     *         configuration property in xwiki.cfg. A value of "all" will add all databases. Note that the main database
     *         is automatically added even if not specified.
     */
    private Set getDatabasesToMigrate(XWikiContext context) throws XWikiException
    {
        Set databasesToMigrate = new ListOrderedSet();

        // Always migrate the main database. We also want this to be the first database migrated so it has to be the
        // first returned in the list.
        databasesToMigrate.add(context.getMainXWiki());

        // Add the databases listed by the user (if any). If there's a single database named and if it's "all" or "ALL"
        // then automatically add all the registered databases.
        if (context.getWiki().isVirtual()) {
            String[] databases = context.getWiki().getConfig().getPropertyAsList("xwiki.store.migration.databases");
            if ((databases.length == 1) && databases[0].equalsIgnoreCase("all")) {
                databasesToMigrate.addAll(context.getWiki().getVirtualWikisDatabaseNames(context));
            } else {
                for (int i = 0; i < databases.length; i++) {
                    databasesToMigrate.add(databases[i]);
                }
            }
        }

        return databasesToMigrate;
    }

    /**
     * It is assumed that before calling this method the XWiki context has been set with the database to migrate.
     */
    private void startMigrationsForDatabase(XWikiContext context) throws XWikiException
    {
        XWikiDBVersion curversion = getDBVersion(context);
        try {
            Collection neededMigrations = getNeededMigrations(context);
            startMigrations(neededMigrations, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_MIGRATION, "Migration failed", e);
        }
    }

    /**
     * @return collection of {@link XWikiMigratorInterface} in ascending order,
     *   which need be executed.
     * @param context used everywhere
     * @throws Exception if any error 
     */
    protected Collection getNeededMigrations(XWikiContext context) throws Exception
    {
        XWikiDBVersion curversion = getDBVersion(context);
        SortedMap neededMigrations = new TreeMap();
        String[] forcedMigrations = context.getWiki().getConfig().getPropertyAsList("xwiki.store.migration.force");
        if (forcedMigrations.length > 0) {
            for (int i = 0; i < forcedMigrations.length; i++) {
                XWikiMigratorInterface migrator =
                    (XWikiMigratorInterface) Class.forName(forcedMigrations[i]).newInstance();
                neededMigrations.put(migrator.getVersion(), migrator); 
            }
        } else {
            Set ignoredMigrations = new HashSet(Arrays.asList(context.getWiki().getConfig()
                .getPropertyAsList("xwiki.store.migration.ignored")));
            List allMigrations = getAllMigrations(context);
            for (Iterator it = allMigrations.iterator(); it.hasNext();) {
                XWikiMigratorInterface migrator = (XWikiMigratorInterface) it.next();
                if (ignoredMigrations.contains(migrator.getClass().getName())
                    || ignoredMigrations.contains(migrator.getVersion().toString()))
                {
                    continue;
                }
                if (migrator.getVersion().compareTo(curversion) >= 0) {
                    neededMigrations.put(migrator.getVersion(), migrator);
                }
            }
        }

        Collection neededMigrationsAsCollection = neededMigrations.values();
        if (LOG.isInfoEnabled()) {
            if (!neededMigrations.isEmpty()) {
                LOG.info("Current storage version = [" + curversion.toString() + "]");
                LOG.info("List of migrations that will be executed:");
                for (Iterator it = neededMigrationsAsCollection.iterator(); it.hasNext();) {
                    XWikiMigratorInterface migrator = (XWikiMigratorInterface) it.next();
                    LOG.info("  " + migrator.getName() + " - " + migrator.getDescription());
                }
            } else {
                LOG.info("No storage migration required since current version is [" + curversion.toString() + "]");
            }
        }

        return neededMigrationsAsCollection; 
    }
    /**
     * @param migrations - run this migrations in order of collection
     * @param context - used everywhere
     * @throws XWikiException if any error
     */
    protected void startMigrations(Collection migrations, XWikiContext context)
        throws XWikiException
    {
        XWikiDBVersion curversion = getDBVersion(context);
        for (Iterator it = migrations.iterator(); it.hasNext();) {
            XWikiMigratorInterface migrator = (XWikiMigratorInterface) it.next();
            if (LOG.isInfoEnabled()) {
                LOG.info("Running migration [" + migrator.getName() + "] with version [" + migrator.getVersion() + "]");
            }
            migrate(migrator, context);
            if (migrator.getVersion().compareTo(curversion) > 0) {
                setDBVersion(migrator.getVersion().increment(), context);
                if (LOG.isInfoEnabled()) {
                    LOG.info("New storage version is now [" + getDBVersion(context) + "]");
                }
            }
        }
    }
    /**
     * @param migrator to execute
     * @param context used everywhere
     * @throws XWikiException if any error
     */
    protected void migrate(XWikiMigratorInterface migrator, XWikiContext context)
        throws XWikiException
    {
        migrator.migrate(this, context);
    }
    /**
     * @param context used everywhere
     * @return List of all {@link XWikiMigratorInterface} for this manager
     * @throws XWikiException if any error
     */
    protected abstract List getAllMigrations(XWikiContext context) throws XWikiException;
}
