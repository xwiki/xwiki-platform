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
package org.xwiki.rest.internal.resources.wikis;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.resources.wikis.WikiResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.packaging.PackageAPI;

/**
 * Resource for interacting with a specific wiki.
 *
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.wikis.WikiResourceImpl")
public class WikiResourceImpl extends XWikiResource implements WikiResource
{
    /**
     * The possible option for managing history when importing wiki documents.
     */
    private enum HistoryOptions
    {
        /**
         * Add a new version.
         */
        ADD,
        /**
         * Reset the version to 1.1.
         */
        RESET,
        /**
         * Replace the current version.
         */
        REPLACE
    }

    @Override
    public Wiki get(String wikiName) throws XWikiRestException
    {
        try {
            if (wikiExists(wikiName)) {
                return DomainObjectFactory.createWiki(objectFactory, uriInfo.getBaseUri(), wikiName);
            }

            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Wiki importXAR(String wikiName, Boolean backup, String history, InputStream is) throws XWikiRestException
    {
        try {
            if (!wikiExists(wikiName)) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            /* Use the package plugin for importing pages */
            XWikiContext xwikiContext = getXWikiContext();
            PackageAPI importer = ((PackageAPI) xwikiContext.getWiki().getPluginApi("package", xwikiContext));
            if (importer == null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                        "Can't access Package plugin API. Generally mean you don't have enough rights.");
            }

            String database = xwikiContext.getWikiId();

            try {
                xwikiContext.setWikiId(wikiName);
                importer.setBackupPack(backup);

                importer.Import(is);

                HistoryOptions historyOption = parseHistoryOption(history, HistoryOptions.ADD);

                switch (historyOption) {
                    case RESET:
                        importer.setPreserveVersion(false);
                        importer.setWithVersions(false);
                        break;
                    case REPLACE:
                        importer.setPreserveVersion(false);
                        importer.setWithVersions(true);
                        break;
                    default:
                    case ADD:
                        importer.setPreserveVersion(true);
                        importer.setWithVersions(false);
                        break;
                }

                // Set the backup pack option
                importer.setBackupPack(backup);

                if (importer.install() == com.xpn.xwiki.plugin.packaging.DocumentInfo.INSTALL_IMPOSSIBLE) {
                    throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                }
            } catch (IOException e) {
                throw new WebApplicationException(e);
            } finally {
                xwikiContext.setWikiId(database);
            }

            return DomainObjectFactory.createWiki(objectFactory, uriInfo.getBaseUri(), wikiName);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    /**
     * Check if a wiki exists.
     *
     * @param wikiName the wiki name.
     * @return true if the wiki exists.
     * @throws XWikiException if something goes wrong.
     */
    protected boolean wikiExists(String wikiName) throws XWikiException
    {
        List<String> databaseNames =
                Utils.getXWiki(componentManager).getVirtualWikisDatabaseNames(Utils.getXWikiContext(componentManager));

        if (databaseNames.isEmpty()) {
            databaseNames.add("xwiki");
        }

        for (String databaseName : databaseNames) {
            if (databaseName.equals(wikiName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the HistoryOptions enum object corresponding to a string.
     *
     * @param value a string representing a history option.
     * @param defaultValue the value to be returned in the case no corresponding history option is found.
     * @return the history option enum object,
     */
    protected HistoryOptions parseHistoryOption(String value, HistoryOptions defaultValue)
    {
        try {
            if (value != null) {
                return HistoryOptions.valueOf(value.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            // Invalid query type string.
        }

        return defaultValue;
    }
}
