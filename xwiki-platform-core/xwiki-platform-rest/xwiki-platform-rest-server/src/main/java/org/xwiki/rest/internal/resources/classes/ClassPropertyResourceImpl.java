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
package org.xwiki.rest.internal.resources.classes;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Class;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.resources.classes.ClassPropertyResource;
import org.xwiki.rest.resources.classes.ClassResource;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.classes.ClassPropertyResourceImpl")
public class ClassPropertyResourceImpl extends XWikiResource implements ClassPropertyResource
{
    @Override
    public Property getClassProperty(String wikiName, String className, String propertyName) throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            com.xpn.xwiki.api.Class xwikiClass = Utils.getXWikiApi(componentManager).getClass(className);
            if (xwikiClass == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            Class clazz = DomainObjectFactory.createClass(objectFactory, uriInfo.getBaseUri(), wikiName, xwikiClass);

            for (Property property : clazz.getProperties()) {
                if (property.getName().equals(propertyName)) {
                    String classUri = Utils.createURI(uriInfo.getBaseUri(), ClassResource.class, wikiName,
                        xwikiClass.getName()).toString();
                    Link classLink = objectFactory.createLink();
                    classLink.setHref(classUri);
                    classLink.setRel(Relations.CLASS);
                    property.getLinks().add(classLink);

                    return property;
                }
            }

            throw new WebApplicationException(Status.NOT_FOUND);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }
}
