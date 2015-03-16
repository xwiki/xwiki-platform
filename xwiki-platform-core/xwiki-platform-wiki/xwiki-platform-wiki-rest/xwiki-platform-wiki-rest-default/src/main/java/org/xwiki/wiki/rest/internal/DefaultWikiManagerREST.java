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
package org.xwiki.wiki.rest.internal;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.resources.ModificationsResource;
import org.xwiki.rest.resources.classes.ClassesResource;
import org.xwiki.rest.resources.spaces.SpacesResource;
import org.xwiki.rest.resources.wikis.WikiResource;
import org.xwiki.rest.resources.wikis.WikiSearchQueryResource;
import org.xwiki.rest.resources.wikis.WikiSearchResource;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.rest.WikiManagerREST;
import org.xwiki.wiki.template.WikiTemplateManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation for {@link org.xwiki.wiki.rest.WikiManagerREST}.
 *
 * @since 5.4RC1
 * @version $Id$
 */
@Component
@Named("org.xwiki.wiki.rest.internal.DefaultWikiManagerREST")
@Path("/wikimanager")
public class DefaultWikiManagerREST extends XWikiResource implements WikiManagerREST
{
    @Inject
    private WikiManager wikiManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiTemplateManager wikiTemplateManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    @POST
    public Response createWiki(@QueryParam("template") String template, Wiki wiki) throws XWikiRestException
    {
        XWikiContext xcontext = getXWikiContext();
        WikiDescriptor descriptor = null;

        try {
            // Create the wiki
            descriptor = wikiManager.create(wiki.getId(), wiki.getId(), true);

            // Change the descriptor
            if (wiki.getOwner() != null) {
                descriptor.setOwnerId(wiki.getOwner());
            } else {
                descriptor.setOwnerId(entityReferenceSerializer.serialize(xcontext.getUserReference()));
            }
            descriptor.setPrettyName(wiki.getName());
            descriptor.setDescription(wiki.getDescription());

            // Save the descriptor
            wikiDescriptorManager.saveDescriptor(descriptor);

            // Apply a template (if needed)
            if (template != null) {
                WikiProvisioningJob job = wikiTemplateManager.applyTemplate(descriptor.getId(), template);
                job.join();
            }

            // Build the response
            Wiki result = createWiki(objectFactory, uriInfo.getBaseUri(), wiki.getId(), wiki.getOwner(),
                    wiki.getDescription());

            // Add a link to the WebHome of the newly created wiki.
            Link home = objectFactory.createLink();
            home.setRel(Relations.HOME);
            home.setHref(xcontext.getWiki().getURL(descriptor.getMainPageReference(), "view", xcontext));
            result.getLinks().add(home);

            // Return the final response
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
