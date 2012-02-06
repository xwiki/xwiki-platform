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
package org.xwiki.rest.resources.classes;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.Utils;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Class;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.classes.ClassResource")
@Path("/wikis/{wikiName}/classes/{className}")
public class ClassResource extends XWikiResource
{
    @GET
    public Class getClass(@PathParam("wikiName") String wikiName, @PathParam("className") String className)
        throws XWikiException
    {

        String database = Utils.getXWikiContext(componentManager).getDatabase();

        try {
            Utils.getXWikiContext(componentManager).setDatabase(wikiName);

            com.xpn.xwiki.api.Class xwikiClass = Utils.getXWikiApi(componentManager).getClass(className);
            if (xwikiClass == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            return DomainObjectFactory.createClass(objectFactory, uriInfo.getBaseUri(), wikiName, xwikiClass);
        } finally {
            Utils.getXWikiContext(componentManager).setDatabase(database);
        }
    }
}
