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

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Property;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/classes/{className}/properties/{propertyName}")
public interface ClassPropertyResource
{
    /**
     * Returns the definition of a single property of an XClass.
     *
     * @param wikiName the identifier of the wiki containing the class, for example {@code xwiki} for the main wiki
     * @param className the reference of the XClass the property belongs to, for example {@code XWiki.XWikiUsers}
     * @param propertyName the name of the property to return, for example {@code email}
     * @return the requested property definition, together with a link back to its class
     * @throws XWikiRestException if the class property cannot be retrieved; a {@code 404} response is returned when the
     *  class or the property does not exist, and a {@code 401} response when the current user is not allowed to view
     *  the class
     */
    @GET Property getClassProperty(
            @PathParam("wikiName") String wikiName,
            @PathParam("className") String className,
            @PathParam("propertyName") String propertyName
    ) throws XWikiRestException;
}
