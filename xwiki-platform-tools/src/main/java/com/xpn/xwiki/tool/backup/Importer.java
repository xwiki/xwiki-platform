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
import com.xpn.xwiki.plugin.packaging.Package;
import com.xpn.xwiki.plugin.packaging.PackageException;

import java.io.File;
import java.io.IOException;

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
    }
}
