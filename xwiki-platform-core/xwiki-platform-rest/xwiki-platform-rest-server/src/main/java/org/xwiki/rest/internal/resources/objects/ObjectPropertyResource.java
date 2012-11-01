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
package org.xwiki.rest.internal.resources.objects;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.internal.resources.objects.ObjectPropertyResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/objects/{className}/{objectNumber}/properties/{propertyName}")
public class ObjectPropertyResource extends XWikiResource
{
    @GET
    public Property getObjectProperty(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("className") String className,
        @PathParam("objectNumber") Integer objectNumber, @PathParam("propertyName") String propertyName,
        @QueryParam("prettynames") @DefaultValue("0") Boolean withPrettyNames)
        throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        XWikiDocument xwikiDocument =
            Utils.getXWiki(componentManager).getDocument(doc.getPrefixedFullName(),
                Utils.getXWikiContext(componentManager));

        com.xpn.xwiki.objects.BaseObject baseObject = xwikiDocument.getObject(className, objectNumber);
        if (baseObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Object object =
            DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), Utils
                .getXWikiContext(componentManager), doc, baseObject, false, Utils.getXWikiApi(componentManager), withPrettyNames);

        for (Property property : object.getProperties()) {
            if (property.getName().equals(propertyName)) {
                String objectUri =
                    UriBuilder.fromUri(uriInfo.getBaseUri()).path(ObjectResource.class).build(doc.getWiki(),
                        doc.getSpace(), doc.getName(), object.getClassName(), object.getNumber()).toString();
                Link objectLink = objectFactory.createLink();
                objectLink.setHref(objectUri);
                objectLink.setRel(Relations.OBJECT);
                property.getLinks().add(objectLink);

                return property;
            }
        }

        throw new WebApplicationException(Status.NOT_FOUND);
    }

    @PUT
    public Response updateObjectProperty(@PathParam("wikiName") String wikiName,
        @PathParam("spaceName") String spaceName, @PathParam("pageName") String pageName,
        @PathParam("className") String className, @PathParam("objectNumber") Integer objectNumber,
        @PathParam("propertyName") String propertyName, Property property) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", Utils.getXWikiUser(componentManager))) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        XWikiDocument xwikiDocument =
            Utils.getXWiki(componentManager).getDocument(doc.getPrefixedFullName(),
                Utils.getXWikiContext(componentManager));

        com.xpn.xwiki.objects.BaseObject baseObject = xwikiDocument.getObject(className, objectNumber);
        if (baseObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        baseObject.set(propertyName, property.getValue(), Utils.getXWikiContext(componentManager));

        doc.save();

        baseObject = xwikiDocument.getObject(className, objectNumber);
        Object object =
            DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), Utils
                .getXWikiContext(componentManager), doc, baseObject, false, Utils.getXWikiApi(componentManager), false);

        for (Property p : object.getProperties()) {
            if (p.getName().equals(propertyName)) {
                return Response.status(Status.ACCEPTED).entity(p).build();
            }
        }

        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }

}
