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

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.resources.objects.ObjectResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.objects.ObjectResourceImpl")
public class ObjectResourceImpl extends BaseObjectsResource implements ObjectResource
{
    @Override
    public Object getObject(String wikiName, String spaceName, String pageName, String className, Integer objectNumber,
            Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            com.xpn.xwiki.objects.BaseObject baseObject = getBaseObject(doc, className, objectNumber);
            if (baseObject == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            return DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), Utils
                    .getXWikiContext(componentManager), doc, baseObject, false, Utils.getXWikiApi(componentManager),
                    withPrettyNames);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Response updateObject(String wikiName, String spaceName, String pageName, String className,
            Integer objectNumber, Object object) throws XWikiRestException
    {
        try {
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

            return Response.status(Status.ACCEPTED)
                    .entity(DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), Utils.getXWikiContext(
                            componentManager), doc, baseObject, false, Utils.getXWikiApi(componentManager), false))
                    .build();
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public void deleteObject(String wikiName, String spaceName, String pageName, String className, Integer objectNumber)
            throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            if (!doc.hasAccessLevel("edit", Utils.getXWikiUser(componentManager))) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            com.xpn.xwiki.api.Object object = doc.getObject(className, objectNumber);
            if (object == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            doc.removeObject(object);

            doc.save();
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
