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
package org.xwiki.rest.resources.wikis;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Wiki;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import com.xpn.xwiki.plugin.packaging.PackageAPI;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.wikis.WikiResource")
@Path("/wikis/{wikiName}")
public class WikiResource extends XWikiResource
{
    @GET
    public Wiki get(@PathParam("wikiName") String wikiName) throws XWikiException
    {
        if (wikiExists(wikiName)) {
            return DomainObjectFactory.createWiki(objectFactory, uriInfo.getBaseUri(), wikiName);
        }

        throw new WebApplicationException(Status.NOT_FOUND);
    }

    @POST
    public Wiki importXAR(@PathParam("wikiName") String wikiName, InputStream is) throws XWikiException
    {
        if (!wikiExists(wikiName)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        /* Use the package plugin for importing pages */
        XWikiContext xwikiContext = getXWikiContext();
        PackageAPI importer = ((PackageAPI) xwikiContext.getWiki().getPluginApi("package", xwikiContext));

        String database = xwikiContext.getDatabase();

        try {
            xwikiContext.setDatabase(wikiName);
            importer.Import(is);
            if (importer.install() == com.xpn.xwiki.plugin.packaging.DocumentInfo.INSTALL_IMPOSSIBLE) {
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            throw new WebApplicationException(e);
        } finally {
            xwikiContext.setDatabase(database);
        }

        return DomainObjectFactory.createWiki(objectFactory, uriInfo.getBaseUri(), wikiName);
    }

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

}
