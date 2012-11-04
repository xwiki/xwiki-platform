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
package org.xwiki.manager.rest.internal.resources;

import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.xwiki.component.annotation.Component;
import org.xwiki.manager.rest.resources.WikiManagerResource;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.resources.ModificationsResource;
import org.xwiki.rest.resources.classes.ClassesResource;
import org.xwiki.rest.resources.spaces.SpacesResource;
import org.xwiki.rest.resources.wikis.WikiResource;
import org.xwiki.rest.resources.wikis.WikiSearchQueryResource;
import org.xwiki.rest.resources.wikis.WikiSearchResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.applicationmanager.core.api.XWikiExceptionApi;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerException;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerPlugin;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerPluginApi;
import com.xpn.xwiki.plugin.wikimanager.doc.XWikiServer;

/**
 * @version $Id$
 */
@Component("org.xwiki.manager.rest.internal.resources.WikiManagerResourceImpl")
@Path("/wikimanager")
public class WikiManagerResourceImpl extends XWikiResource implements WikiManagerResource
{
    @Override
    @POST
    public Response createWiki(@QueryParam("template") String template, Wiki wiki) throws XWikiRestException
    {
        /* Get the wiki manager plugin */
        XWikiContext xwikiContext = Utils.getXWikiContext(componentManager);
        WikiManagerPlugin wikiManagerPlugin =
                new WikiManagerPlugin("wikimanager", WikiManagerPlugin.class.getName(), xwikiContext);
        wikiManagerPlugin.init(xwikiContext);
        WikiManagerPluginApi wikiManager =
                (WikiManagerPluginApi) wikiManagerPlugin.getPluginApi(wikiManagerPlugin, xwikiContext);
        try {
            /* Create the wiki */
            XWikiServer wikiServer = wikiManager.createWikiDocument();
            if (wiki.getId() != null) {
                wikiServer.setServer(wiki.getId());
            } else {
                throw new WebApplicationException(
                        Response.status(Status.BAD_REQUEST).entity("Wiki id must be specified")
                                .build());
            }

            /* If the owner is not specified, set it to the user who made the request. */
            if (wiki.getOwner() != null) {
                wikiServer.setOwner(wiki.getOwner());
            } else {
                wikiServer.setOwner(xwikiContext.getUser());
            }

            wikiServer.setWikiPrettyName(wiki.getName());
            wikiServer.setDescription(wiki.getDescription());

            int resultCode = wikiManager.createNewWiki(wiki.getId(), template, null, wikiServer, true);

            switch (resultCode) {
                case XWikiExceptionApi.ERROR_NOERROR:
                    break;
                case WikiManagerException.ERROR_WM_WIKIALREADYEXISTS:
                    throw new WebApplicationException(Response.status(Status.CONFLICT)
                            .entity(String.format("Wiki '%s' already exists", wiki.getId())).build());
                default:
                    throw new WebApplicationException(Response.serverError()
                            .entity(String.format("Error creating wiki (Error number %d)", resultCode)).build());
            }

            /* Build the response. */
            Wiki result = createWiki(objectFactory, uriInfo.getBaseUri(), wiki.getId(), wiki.getOwner(),
                    wiki.getDescription());

            /* Add a link to the WebHome of the newly created wiki. */
            Link home = objectFactory.createLink();
            home.setRel(Relations.HOME);
            home.setHref(wikiServer.getHomePageUrl().toString());
            result.getLinks().add(home);

            return Response
                    .created(UriBuilder.fromUri(uriInfo.getBaseUri()).path(WikiResource.class).build(wiki.getId()))
                    .entity(result).build();
        } catch (Exception e) {
            throw new XWikiRestException(e);
        }
    }

    /**
     * Create the wiki model object containing wiki information.
     *
     * @param objectFactory the JAXB object factory for creating model objects.
     * @param baseUri the base URI for links.
     * @param wikiName the wiki name.
     * @param owner the wiki owner.
     * @param description the wiki description.
     * @return the wiki model object.
     */
    public static Wiki createWiki(ObjectFactory objectFactory, URI baseUri, String wikiName, String owner,
            String description)
    {
        Wiki wiki = objectFactory.createWiki().withId(wikiName).withName(wikiName).withOwner(owner)
                .withDescription(description);

        String spacesUri = UriBuilder.fromUri(baseUri).path(SpacesResource.class).build(wikiName).toString();
        Link spacesLink = objectFactory.createLink();
        spacesLink.setHref(spacesUri);
        spacesLink.setRel(Relations.SPACES);
        wiki.getLinks().add(spacesLink);

        String classesUri = UriBuilder.fromUri(baseUri).path(ClassesResource.class).build(wikiName).toString();
        Link classesLink = objectFactory.createLink();
        classesLink.setHref(classesUri);
        classesLink.setRel(Relations.CLASSES);
        wiki.getLinks().add(classesLink);

        String modificationsUri =
                UriBuilder.fromUri(baseUri).path(ModificationsResource.class).build(wikiName).toString();
        Link modificationsLink = objectFactory.createLink();
        modificationsLink.setHref(modificationsUri);
        modificationsLink.setRel(Relations.MODIFICATIONS);
        wiki.getLinks().add(modificationsLink);

        String searchUri = UriBuilder.fromUri(baseUri).path(WikiSearchResource.class).build(wikiName).toString();
        Link searchLink = objectFactory.createLink();
        searchLink.setHref(searchUri);
        searchLink.setRel(Relations.SEARCH);
        wiki.getLinks().add(searchLink);

        String queryUri = UriBuilder.fromUri(baseUri).path(WikiSearchQueryResource.class).build(wikiName).toString();
        Link queryLink = objectFactory.createLink();
        queryLink.setHref(queryUri);
        queryLink.setRel(Relations.QUERY);
        wiki.getLinks().add(queryLink);

        return wiki;
    }
}