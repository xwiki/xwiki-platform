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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.RangeIterable;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.objects.ObjectsResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.objects.ObjectsResourceImpl")
public class ObjectsResourceImpl extends BaseObjectsResource implements ObjectsResource
{
    @Inject
    private ModelFactory factory;

    @Override
    public Objects getObjects(String wikiName, String spaceName, String pageName, Integer start, Integer number,
        Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            Objects objects = objectFactory.createObjects();

            List<BaseObject> objectList = getBaseObjects(doc.getDocumentReference());

            RangeIterable<BaseObject> ri = new RangeIterable<BaseObject>(objectList, start, number);

            for (BaseObject object : ri) {
                /* By deleting objects, some of them might become null, so we must check for this */
                if (object != null) {
                    objects.getObjectSummaries().add(
                        DomainObjectFactory.createObjectSummary(objectFactory, uriInfo.getBaseUri(),
                            Utils.getXWikiContext(componentManager), doc, object, false,
                            Utils.getXWikiApi(componentManager), withPrettyNames));
                }
            }

            return objects;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Response addObject(String wikiName, String spaceName, String pageName, Boolean minorRevision, Object object)
        throws XWikiRestException
    {
        if (object.getClassName() == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        try {
            List<String> spaces = parseSpaceSegments(spaceName);
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaces, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            if (!doc.hasAccessLevel("edit", Utils.getXWikiUser(componentManager))) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            com.xpn.xwiki.api.Object xwikiObject = doc.newObject(object.getClassName());

            if (xwikiObject == null) {
                throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
            }

            this.factory.toObject(xwikiObject, object);

            doc.save("", Boolean.TRUE.equals(minorRevision));

            BaseObject baseObject = getBaseObject(doc, object.getClassName(), xwikiObject.getNumber());

            return Response
                .created(Utils.createURI(this.uriInfo.getBaseUri(), ObjectResource.class, wikiName, spaces, pageName,
                    object.getClassName(), baseObject.getNumber()))
                .entity(this.factory.toRestObject(this.uriInfo.getBaseUri(), doc, baseObject, false, false)).build();
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
