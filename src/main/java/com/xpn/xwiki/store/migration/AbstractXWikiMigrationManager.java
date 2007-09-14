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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

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
        XWikiDBVersion curversion = getDBVersion(context);
        if (LOG.isInfoEnabled()) {
            LOG.info("current data version = " + curversion.toString());
        }
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
        String[] forcedMigrations = context.getWiki().getConfig()
            .getPropertyAsList("xwiki.store.migration.force");
        if (forcedMigrations.length > 0) {
            for (int i = 0; i < forcedMigrations.length; i++) {
                XWikiMigratorInterface migrator = (XWikiMigratorInterface)
                    Class.forName(forcedMigrations[i]).newInstance();
                neededMigrations.put(migrator.getVersion(), migrator); 
            }
        } else {
            Set ignoredMigrations = new HashSet(Arrays.asList(context.getWiki().getConfig()
                .getPropertyAsList("xwiki.store.migration.ignored")));
            List allMigrations = getAllMigrations(context);
            for (Iterator it = allMigrations.iterator(); it.hasNext();) {
                XWikiMigratorInterface migrator = (XWikiMigratorInterface) 
                    it.next();
                if (ignoredMigrations.contains(migrator.getClass().getName())
                    || ignoredMigrations.contains(migrator.getVersion().toString())) {
                    continue;
                }
                if (migrator.getVersion().compareTo(curversion) >= 0) {
                    neededMigrations.put(migrator.getVersion(), migrator);
                }
            }
        }
        return neededMigrations.values(); 
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
                LOG.info("running migrator '" + migrator.getClass().getName()
                    + "' with version " + migrator.getVersion());
            }
            migrate(migrator, context);
            if (migrator.getVersion().compareTo(curversion) > 0) {
                setDBVersion(migrator.getVersion().increment(), context);
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
     * @return List of all {@link AbstractXWikiMigrator} for this manager
     * @throws XWikiException if any error
     */
    protected abstract List getAllMigrations(XWikiContext context) throws XWikiException;
}
