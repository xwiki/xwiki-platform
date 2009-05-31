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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Property;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/objects")
public class ObjectsResource extends XWikiResource
{
    @GET
    public Objects getObjects(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") @DefaultValue("-1") Integer number) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        Objects objects = objectFactory.createObjects();

        List<com.xpn.xwiki.objects.BaseObject> objectList = new ArrayList<com.xpn.xwiki.objects.BaseObject>();

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

        Map<String, Vector<com.xpn.xwiki.objects.BaseObject>> classToObjectsMap = xwikiDocument.getxWikiObjects();
        for (String className : classToObjectsMap.keySet()) {
            Vector<com.xpn.xwiki.objects.BaseObject> xwikiObjects = classToObjectsMap.get(className);
            for (com.xpn.xwiki.objects.BaseObject object : xwikiObjects) {
                objectList.add(object);
            }
        }

        RangeIterable<com.xpn.xwiki.objects.BaseObject> ri =
            new RangeIterable<com.xpn.xwiki.objects.BaseObject>(objectList, start, number);

        for (com.xpn.xwiki.objects.BaseObject object : ri) {
            /* By deleting objects, some of them might become null, so we must check for this */
            if (object != null) {
                objects.getObjectSummaries().add(
                    DomainObjectFactory.createObjectSummary(objectFactory, uriInfo.getBaseUri(), xwikiContext, doc,
                        object, false));
            }
        }

        return objects;
    }

    @POST
    public Response addObject(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, Object object) throws XWikiException
    {
        if (object.getClassName() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", xwikiUser)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
        int objectNumber = xwikiDocument.createNewObject(object.getClassName(), xwikiContext);
        BaseObject xwikiObject = xwikiDocument.getObject(object.getClassName(), objectNumber);
        if (xwikiObject == null) {
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

        // We must initialize all the fields to an empty value in order to correctly create the object
        BaseClass xwikiClass = xwiki.getClass(xwikiObject.getClassName(), xwikiContext);
        for (java.lang.Object propertyNameObject : xwikiClass.getPropertyNames()) {
            String propertyName = (String) propertyNameObject;
            xwikiObject.set(propertyName, "", xwikiContext);
        }

        for (Property property : object.getProperties()) {
            xwikiObject.set(property.getName(), property.getValue(), xwikiContext);
        }

        doc.save();

        return Response.created(
            UriBuilder.fromUri(uriInfo.getBaseUri()).path(ObjectResource.class).build(wikiName, spaceName, pageName,
                object.getClassName(), objectNumber)).entity(
            DomainObjectFactory
                .createObject(objectFactory, uriInfo.getBaseUri(), xwikiContext, doc, xwikiObject, false)).build();
    }

}
