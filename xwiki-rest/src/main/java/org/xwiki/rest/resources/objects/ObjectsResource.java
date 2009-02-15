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

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.xwiki.rest.Constants;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.ObjectSummary;
import org.xwiki.rest.model.Objects;
import org.xwiki.rest.model.Property;
import org.xwiki.rest.model.XStreamFactory;

import com.thoughtworks.xstream.XStream;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * @version $Id$
 */
public class ObjectsResource extends XWikiResource
{
    @Override
    public Representation represent(Variant variant)
    {
        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
        if (documentInfo == null) {
            return null;
        }

        Document doc = documentInfo.getDocument();

        Objects objects = new Objects();

        List<com.xpn.xwiki.objects.BaseObject> objectList = new ArrayList<com.xpn.xwiki.objects.BaseObject>();
        try {
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            Map<String, Vector<com.xpn.xwiki.objects.BaseObject>> classToObjectsMap = xwikiDocument.getxWikiObjects();
            for (String className : classToObjectsMap.keySet()) {
                Vector<com.xpn.xwiki.objects.BaseObject> xwikiObjects = classToObjectsMap.get(className);
                for (com.xpn.xwiki.objects.BaseObject object : xwikiObjects) {
                    objectList.add(object);
                }
            }

            Form queryForm = getRequest().getResourceRef().getQueryAsForm();
            RangeIterable<com.xpn.xwiki.objects.BaseObject> ri =
                new RangeIterable<com.xpn.xwiki.objects.BaseObject>(objectList, Utils.parseInt(queryForm
                    .getFirstValue(Constants.START_PARAMETER), 0), Utils.parseInt(queryForm
                    .getFirstValue(Constants.NUMBER_PARAMETER), -1));

            for (com.xpn.xwiki.objects.BaseObject object : ri) {
                objects.addObjectSummary(DomainObjectFactory.createObjectSummary2(getRequest(), resourceClassRegistry,
                    xwikiContext, doc, object));
            }

            return getRepresenterFor(variant).represent(getContext(), getRequest(), getResponse(), objects);
        } catch (XWikiException e) {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean allowPost()
    {
        return true;
    }

    @Override
    public void handlePost()
    {
        MediaType mediaType = getRequest().getEntity().getMediaType();
        if (!mediaType.equals(MediaType.APPLICATION_XML)) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE);
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

        DocumentInfo documentInfo = getDocumentFromRequest(getRequest(), getResponse(), true, false);
        if (documentInfo == null) {
            return;
        }

        Document doc = documentInfo.getDocument();

        try {
            XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
            int objectNumber = xwikiDocument.createNewObject(objectSummary.getClassName(), xwikiContext);
            BaseObject xwikiObject = xwikiDocument.getObject(objectSummary.getClassName(), objectNumber);
            if (xwikiObject == null) {
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                return;
            }

            /* We must initialize all the fields to an empty value in order to correctly create the object */
            BaseClass xwikiClass = xwiki.getClass(xwikiObject.getClassName(), xwikiContext);
            for (Object propertyNameObject : xwikiClass.getPropertyNames()) {
                String propertyName = (String) propertyNameObject;
                xwikiObject.set(propertyName, "", xwikiContext);
            }

            for (Property property : objectSummary.getPropertyList().getProperties()) {
                xwikiObject.set(property.getName(), property.getValue(), xwikiContext);
            }

            doc.save();

            getResponse().setStatus(Status.SUCCESS_CREATED);

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

}
