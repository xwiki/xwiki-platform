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
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/objects/{className}/{objectNumber}")
public class ObjectResource extends XWikiResource
{
    @GET
    public Object getObject(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("className") String className,
        @PathParam("objectNumber") Integer objectNumber) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

        com.xpn.xwiki.objects.BaseObject baseObject = xwikiDocument.getObject(className, objectNumber);
        if (baseObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), xwikiContext, doc, baseObject,
            false);
    }

    @PUT
    public Response updateObject(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("className") String className,
        @PathParam("objectNumber") Integer objectNumber, Object object) throws XWikiException
    {

        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", xwikiUser)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

        com.xpn.xwiki.objects.BaseObject baseObject = xwikiDocument.getObject(className, objectNumber);
        if (baseObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        for (Property property : object.getProperties()) {
            baseObject.set(property.getName(), property.getValue(), xwikiContext);
        }

        doc.save();

        baseObject = xwikiDocument.getObject(className, objectNumber);

        return Response.status(Status.ACCEPTED)
            .entity(
                DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), xwikiContext, doc, baseObject,
                    false)).build();
    }

    @DELETE
    public void deleteObject(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("className") String className,
        @PathParam("objectNumber") Integer objectNumber) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", xwikiUser)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

        com.xpn.xwiki.objects.BaseObject baseObject = xwikiDocument.getObject(className, objectNumber);
        if (baseObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        xwikiDocument.removeObject(baseObject);

        doc.save();
    }

}
