/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.tool.backup;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.plugin.packaging.Package;
import com.xpn.xwiki.plugin.packaging.PackageException;

import java.io.File;
import java.io.IOException;

import org.hibernate.Session;

/**
 * Import a set of XWiki documents into an existing database.
 *
 * @version $Id$
 */
public class Importer extends AbstractPackager
{
    /**
     * Import documents defined in an XML file located in the passed document definition directory
     * into a database defined by its passed name and by an Hibernate configuration file.
     *
     * <p>Note: I would have liked to call this method "import" but it's a reserved keyword...
     * Strange that it's not allowed for method names though.</p>
     *
     * @param sourceDirectory the directory where the package.xml file is located and where the
     *        documents to import are located
     * @param databaseName some database name (TODO: find out what this name is really)
     * @param hibernateConfig the Hibernate config fill containing the database definition (JDBC
     *        driver, username and password, etc)
     * @throws XWikiException if the import failed for any reason
     * @todo Replace the Hibernate config file with a list of parameters required for the
     *       importation
     */
    public void importDocuments(File sourceDirectory, String databaseName, File hibernateConfig)
        throws XWikiException
    {
        XWikiContext context = createXWikiContext(databaseName, hibernateConfig);

        Package pack = new Package();
        pack.setWithVersions(false);

        // TODO: The readFromDir method should not throw IOExceptions, only PackageException.
        // See http://jira.xwiki.org/jira/browse/XWIKI-458
        try {
            pack.readFromDir(sourceDirectory, context);
        } catch (IOException e) {
            throw new PackageException(PackageException.ERROR_PACKAGE_UNKNOWN,
                "Failed to import documents from [" + sourceDirectory + "]", e);
        }

        pack.install(context);

        // We MUST shutdown HSQLDB because otherwise the last transactions will not be flushed
        // to disk and will be lost. In practice this means the last Document imported has a
        // very high chance of not making it...
        // TODO: Find a way to implement this generically for all databases and inside
        // XWikiHibernateStore (cf http://jira.xwiki.org/jira/browse/XWIKI-471).
        // WARNING: This Packager may not work with databases other than HSQLDB because of this
        // and this needs to be fixed ASAP...
        shutdownHSQLDB(context);
    }

    /**
     * Shutdowns HSQLDB.
     *
     * @param context the XWiki Context object from which we can retrieve the Store implementation
     * @throws XWikiException in case of shutdown error
     */
    private void shutdownHSQLDB(XWikiContext context) throws XWikiException
    {
        XWikiStoreInterface store = context.getWiki().getStore();
        if (XWikiCacheStore.class.isAssignableFrom(store.getClass())) {
            store = ((XWikiCacheStore) store).getStore();
        }

        if (XWikiHibernateStore.class.isAssignableFrom(store.getClass())) {
            XWikiHibernateStore hibernateStore = (XWikiHibernateStore) store;

            boolean bTransaction = true;
            try {
                hibernateStore.checkHibernate(context);
                bTransaction = hibernateStore.beginTransaction(false, context);
                Session session = hibernateStore.getSession(context);
                session.connection().createStatement().execute("SHUTDOWN");
                if (bTransaction) {
                    hibernateStore.endTransaction(context, false, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new PackageException(PackageException.ERROR_PACKAGE_UNKNOWN,
                    "Failed to shutdown database", e);
            } finally {
                try {
                    if (bTransaction) {
                        hibernateStore.endTransaction(context, false, false);
                    }
                } catch (Exception e) {
                }
            }
        }

    }
}
