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
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Properties;
import org.xwiki.rest.resources.objects.ObjectPropertiesResource;
import org.xwiki.rest.resources.objects.ObjectResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.objects.ObjectPropertiesResourceImpl")

public class ObjectPropertiesResourceImpl extends BaseObjectsResource implements ObjectPropertiesResource
{
    @Override
    public Properties getObjectProperties(String wikiName, String spaceName, String pageName, String className,
            Integer objectNumber, Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);

            Document doc = documentInfo.getDocument();

            com.xpn.xwiki.objects.BaseObject baseObject = getBaseObject(doc, className, objectNumber);
            if (baseObject == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            Object object = DomainObjectFactory.createObject(objectFactory, uriInfo.getBaseUri(), Utils.getXWikiContext(
                    componentManager), doc, baseObject, false, Utils.getXWikiApi(componentManager), withPrettyNames);

            Properties properties = objectFactory.createProperties();
            properties.getProperties().addAll(object.getProperties());

            String objectUri = Utils.createURI(uriInfo.getBaseUri(), ObjectResource.class, doc.getWiki(),
                doc.getSpace(), doc.getName(), object.getClassName(), object.getNumber()).toString();
            Link objectLink = objectFactory.createLink();
            objectLink.setHref(objectUri);
            objectLink.setRel(Relations.OBJECT);
            properties.getLinks().add(objectLink);

            return properties;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
