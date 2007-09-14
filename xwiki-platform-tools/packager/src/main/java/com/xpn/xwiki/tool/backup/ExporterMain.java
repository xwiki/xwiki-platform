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
package com.xpn.xwiki.tool.backup;

import com.xpn.xwiki.XWikiException;

import java.io.File;

/**
 * Class to call from the command line to export XWiki documents found in a database to the file
 * system.
 *
 * @version $Id: $
 */
public final class ExporterMain
{
    /**
     * This class is not meant to be instantiated.
     */
    private ExporterMain()
    {
    }

    /**
     * Command line hook so that the exporter can be called directly on the command line.
     *
     * @param args the first argument is the directory where to export the documents, the second
     *             argument is some database name (TODO: find out what this name is really) and the
     *             third argument is the Hibernate config fill containing the database definition
     *             (JDBC driver, username and password, etc)
     * @throws com.xpn.xwiki.XWikiException in case of an error while exporting
     */
    public static void main(String[] args) throws XWikiException
    {
        File exportDirectory = new File(args[0]);
        String databaseName = args[1];
        File hibernateConfig = new File(args[2]);

        new Exporter().exportDocuments(exportDirectory, databaseName, hibernateConfig);
    }
}
