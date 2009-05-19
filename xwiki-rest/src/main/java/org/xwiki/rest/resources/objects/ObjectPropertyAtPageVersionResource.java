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

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.objects.ObjectPropertyAtPageVersionResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/history/{version}/objects/{className}/{objectNumber}/properties/{propertyName}")
public class ObjectPropertyAtPageVersionResource extends XWikiResource
{
    @GET
    public Property getObjectProperty(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @PathParam("version") String version,
        @PathParam("className") String className, @PathParam("objectNumber") Integer objectNumber,
        @PathParam("propertyName") String propertyName) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, version, true, false);

        Document doc = documentInfo.getDocument();

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
        xwikiDocument = xwiki.getDocument(xwikiDocument, doc.getVersion(), xwikiContext);

        com.xpn.xwiki.objects.BaseObject baseObject = xwikiDocument.getObject(className, objectNumber);
        if (baseObject == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Object object =
            DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), xwikiContext, doc, baseObject, true);

        for (Property property : object.getProperties()) {
            if (property.getName().equals(propertyName)) {
                String objectUri =
                    UriBuilder.fromUri(uriInfo.getBaseUri()).path(ObjectAtPageVersionResource.class).build(doc.getWiki(),
                        doc.getSpace(), doc.getName(), version, object.getClassName(), object.getNumber()).toString();
                Link objectLink = objectFactory.createLink();
                objectLink.setHref(objectUri);
                objectLink.setRel(Relations.OBJECT);
                property.getLinks().add(objectLink);

                return property;
            }
        }

        throw new WebApplicationException(Status.NOT_FOUND);
    }

}
