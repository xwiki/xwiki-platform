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
import org.xwiki.rest.model.jaxb.Properties;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/classes/{className}/properties")
public interface ClassPropertiesResource
{
    /**
     * Returns the property definitions of an XClass.
     *
     * @param wikiName the identifier of the wiki containing the class, for example {@code xwiki} for the main wiki
     * @param className the reference of the XClass whose property definitions are returned, for example
     *  {@code XWiki.XWikiUsers}
     * @return the property definitions of the class, together with a link back to the class itself
     * @throws XWikiRestException if the class properties cannot be retrieved; the class must exist and the current user
     *  must be allowed to view it, otherwise a {@code 404} or {@code 401} response is returned respectively
     */
    @GET Properties getClassProperties(
            @PathParam("wikiName") String wikiName,
            @PathParam("className") String className
    ) throws XWikiRestException;
}
