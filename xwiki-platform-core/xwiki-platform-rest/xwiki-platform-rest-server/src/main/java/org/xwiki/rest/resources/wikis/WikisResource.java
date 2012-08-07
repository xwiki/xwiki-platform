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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Relations;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.WikiDescriptor;
import org.xwiki.rest.model.jaxb.Wikis;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerException;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerPlugin;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerPluginApi;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.wikis.WikisResource")
@Path("/wikis")
public class WikisResource extends XWikiResource
{
    private static final String PLUGIN_ID = "wikimanager";

    @GET
    public Wikis get() throws XWikiException
    {
        List<String> databaseNames =
            Utils.getXWiki(componentManager).getVirtualWikisDatabaseNames(Utils.getXWikiContext(componentManager));

        if (databaseNames.isEmpty()) {
            databaseNames.add("xwiki");
        }

        Wikis wikis = objectFactory.createWikis();

        for (String databaseName : databaseNames) {
            wikis.getWikis().add(DomainObjectFactory.createWiki(objectFactory, uriInfo.getBaseUri(), databaseName));
        }

        return wikis;
    }

    @POST
    public Response createWiki(WikiDescriptor descriptor) throws XWikiException
    {
        /* Get the wiki manager plugin */
        XWikiContext xwikiContext = Utils.getXWikiContext(componentManager);
        WikiManagerPlugin wikiManagerPlugin =
            new WikiManagerPlugin(PLUGIN_ID, WikiManagerPlugin.class.getName(), xwikiContext);
        wikiManagerPlugin.init(xwikiContext);
        WikiManagerPluginApi wikiManager =
            (WikiManagerPluginApi) wikiManagerPlugin.getPluginApi(wikiManagerPlugin, xwikiContext);

        /*
         * Put the document associated to the current user in order to avoid side effects and faulty interactions in
         * code that restores the context. See http://jira.xwiki.org/browse/XWIKI-8119
         */
        XWikiDocument userDocument = xwikiContext.getWiki().getDocument(xwikiContext.getUserReference(), xwikiContext);
        xwikiContext.setDoc(userDocument);

        /* Create the wiki */
        XWikiServer wikiServer = wikiManager.createWikiDocument();
        if (descriptor.getId() != null) {
            wikiServer.setServer(descriptor.getId());
        } else {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Wiki id must be specified")
                .build());
        }

        if (descriptor.getOwner() != null) {
            wikiServer.setOwner(descriptor.getOwner());
        } else {
            wikiServer.setOwner(xwikiContext.getUser());
        }

        wikiServer.setDescription(descriptor.getDescription());
        wikiServer.setWikiPrettyName(descriptor.getPrettyName());

        int result = wikiManager.createNewWiki(descriptor.getId(), descriptor.getTemplate(), null, wikiServer, true);

        switch (result) {
            case XWikiExceptionApi.ERROR_NOERROR:
                break;
            case WikiManagerException.ERROR_WM_WIKIALREADYEXISTS:
                throw new WebApplicationException(Response.status(Status.CONFLICT)
                    .entity(String.format("Wiki '%s' already exists", descriptor.getId())).build());
            default:
                throw new WebApplicationException(Response.serverError()
                    .entity(String.format("Error creating wiki (Error number %d)", result)).build());
        }

        try {
            /* Build the response. */
            Wiki wiki = DomainObjectFactory.createWiki(objectFactory, uriInfo.getBaseUri(), descriptor.getId());

            /* Add a link to the WebHome of the newly created wiki. */
            Link home = objectFactory.createLink();
            home.setRel(Relations.HOME);
            home.setHref(wikiServer.getHomePageUrl().toString());
            wiki.getLinks().add(home);

            return Response
                .created(UriBuilder.fromUri(uriInfo.getBaseUri()).path(WikiResource.class).build(descriptor.getId()))
                .entity(wiki).build();
        } catch (Exception e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

}
