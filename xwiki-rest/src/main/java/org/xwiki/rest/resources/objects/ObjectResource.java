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

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.ObjectSummary;
import org.xwiki.rest.model.Property;
import org.xwiki.rest.model.XStreamFactory;

import com.thoughtworks.xstream.XStream;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
public class ObjectResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        String className = (String) getRequest().getAttributes().get(Constants.CLASS_NAME_PARAMETER);
        String objectNumberString = (String) getRequest().getAttributes().get(Constants.OBJECT_NUMBER_PARAMETER);

        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
        if (documentInfo == null) {
            return null;
        }

        Document doc = documentInfo.getDocument();

        int objectNumber;
        try {
            objectNumber = Integer.parseInt(objectNumberString);
        } catch (NumberFormatException e) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return null;
        }

        try {
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            BaseObject xwikiObject = xwikiDocument.getObject(className, objectNumber);
            if (xwikiObject == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return null;
            }

            ObjectSummary objectSummary =
                DomainObjectFactory.createObjectSummary2(getRequest(), resourceClassRegistry, xwikiContext, doc,
                    xwikiObject);

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), objectSummary);
        } catch (XWikiException e1) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        }
    }

    @Override
    public boolean allowPut()
    {
        return true;
    }

    @Override
    public boolean allowDelete()
    {
        return true;
    }

    @Override
    public void handlePut()
    {
        MediaType mediaType = getRequest().getEntity().getMediaType();
        if (!mediaType.equals(MediaType.APPLICATION_XML)) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
            return;
        }

        String className = (String) getRequest().getAttributes().get(Constants.CLASS_NAME_PARAMETER);
        String objectNumberString = (String) getRequest().getAttributes().get(Constants.OBJECT_NUMBER_PARAMETER);

        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
        if (documentInfo == null) {
            return;
        }

        Document doc = documentInfo.getDocument();

        int objectNumber;
        try {
            objectNumber = Integer.parseInt(objectNumberString);
        } catch (NumberFormatException e) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
        }

        try {
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            BaseObject xwikiObject = xwikiDocument.getObject(className, objectNumber);
            if (xwikiObject == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return;
            }

            XStream xstream = XStreamFactory.getXStream();

            ObjectSummary objectSummary = null;

            /* If we receive an XML that is not convertible to a Page object we reject it */
            try {
                objectSummary = (ObjectSummary) xstream.fromXML(getRequest().getEntity().getStream());
            } catch (Exception e) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
                return;
            }

            for (Property property : objectSummary.getPropertyList().getProperties()) {
                xwikiObject.set(property.getName(), property.getValue(), xwikiContext);
            }

            doc.save();

            xwikiObject = xwikiDocument.getObject(className, objectNumber);

            getResponse().setStatus(Status.SUCCESS_ACCEPTED);

            /* Set the entity as being the new/updated document XML representation */
            getResponse().setEntity(
                new StringRepresentation(Utils.toXml(DomainObjectFactory.createObjectSummary2(getRequest(),
                    resourceClassRegistry, xwikiContext, doc, xwikiObject)), MediaType.APPLICATION_XML));

            return;
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            } else {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            }

            return;
        }
    }

    @Override
    public void handleDelete()
    {
        String className = (String) getRequest().getAttributes().get(Constants.CLASS_NAME_PARAMETER);
        String objectNumberString = (String) getRequest().getAttributes().get(Constants.OBJECT_NUMBER_PARAMETER);

        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
        if (documentInfo == null) {
            return;
        }

        Document doc = documentInfo.getDocument();

        int objectNumber;
        try {
            objectNumber = Integer.parseInt(objectNumberString);
        } catch (NumberFormatException e) {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
        }

        try {
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            BaseObject xwikiObject = xwikiDocument.getObject(className, objectNumber);
            if (xwikiObject == null) {
                getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
                return;
            }

            xwikiDocument.removeObject(xwikiObject);

            doc.save();

        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
            } else {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            }

            return;
        }

    }

}
