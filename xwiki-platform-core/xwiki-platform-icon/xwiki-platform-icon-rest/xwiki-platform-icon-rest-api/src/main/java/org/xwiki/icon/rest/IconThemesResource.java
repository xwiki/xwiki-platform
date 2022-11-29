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
package org.xwiki.icon.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.icon.rest.model.jaxb.Icons;

/**
 * Exposes the wiki icon themes and their icons through REST.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Path("/wikis/{wikiName}/iconThemes")
public interface IconThemesResource
{
    /**
     * Returns the icons metadata of the requested icons list, for a given icon theme.
     *
     * @param wikiName the name of the wiki holding the icons
     * @param iconTheme the name of the icon theme holding the icons
     * @param names a list of icon names to return
     * @return the list of resolved icons metadata
     */
    @GET
    @Path("/{iconTheme}/icons")
    Icons getIconsByTheme(@PathParam("wikiName") String wikiName, @PathParam("iconTheme") String iconTheme,
        @QueryParam("name") List<String> names);

    /**
     * Returns the icons metadata of the requested icons list, for the default icon theme.
     *
     * @param wikiName the name of the wiki holding the icons
     * @param names a list of icon names to return
     * @return the list of resolved icons metadata
     */
    @GET
    @Path("/icons")
    Icons getIcons(@PathParam("wikiName") String wikiName, @QueryParam("name") List<String> names);
}
