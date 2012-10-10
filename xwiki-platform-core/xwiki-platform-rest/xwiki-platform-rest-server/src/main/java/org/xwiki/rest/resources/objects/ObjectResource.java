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
package org.xwiki.rest.resources.objects;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.objects.ObjectResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/objects/{className}/{objectNumber}")
public class ObjectResource extends BaseObjectsResource
{
    @GET
    public Object getObject(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("className") String className,
        @PathParam("objectNumber") Integer objectNumber,
        @QueryParam("prettynames") @DefaultValue("0") Boolean withPrettyNames) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        com.xpn.xwiki.objects.BaseObject baseObject = getBaseObject(doc, className, objectNumber);
        if (baseObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), Utils
            .getXWikiContext(componentManager), doc, baseObject, false, Utils.getXWikiApi(componentManager), withPrettyNames);
    }

    @PUT
    public Response updateObject(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("className") String className,
        @PathParam("objectNumber") Integer objectNumber, Object object) throws XWikiException
    {

        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", Utils.getXWikiUser(componentManager))) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        com.xpn.xwiki.objects.BaseObject baseObject = getBaseObject(doc, className, objectNumber);
        if (baseObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        for (Property property : object.getProperties()) {
            baseObject.set(property.getName(), property.getValue(), Utils.getXWikiContext(componentManager));
        }

        doc.save();

        baseObject = getBaseObject(doc, className, objectNumber);

        return Response.status(Status.ACCEPTED).entity(
            DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), Utils
                .getXWikiContext(componentManager), doc, baseObject, false, Utils.getXWikiApi(componentManager), false)).build();
    }

    @DELETE
    public void deleteObject(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("className") String className,
        @PathParam("objectNumber") Integer objectNumber) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", Utils.getXWikiUser(componentManager))) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        com.xpn.xwiki.api.Object object = doc.getObject(className, objectNumber);
        if(object == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        
        doc.removeObject(object);

        doc.save();
    }

}
