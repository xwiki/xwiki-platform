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
package org.xwiki.image.style.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.image.style.ImageStyleException;
import org.xwiki.image.style.rest.model.jaxb.Styles;

/**
 * Rest endpoint for the image styles.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Path("/wikis/{wikiName}/imageStyles")
public interface ImageStylesResource
{
    /**
     * Return the list of styles for a given wiki.
     *
     * @param wikiName the name of the wiki (e.g., {@code xwiki})
     * @return the list of image styles
     * @throws ImageStyleException in case of error while retrieving the list of styles
     */
    @GET
    Styles getStyles(@PathParam("wikiName") String wikiName) throws ImageStyleException;

    /**
     * Return the identifier of the default style for a given document.
     *
     * @param wikiName the name of the wiki (e.g., {@code xwiki})
     * @param documentReference the document reference to resolve the default style for
     * @return the identifier of the default style
     * @throws ImageStyleException in case of error while retrieving the default style
     */
    @GET
    @Path("/default")
    Response getDefaultStyleIdentifier(@PathParam("wikiName") String wikiName,
        @QueryParam("documentReference") String documentReference) throws ImageStyleException;
}
