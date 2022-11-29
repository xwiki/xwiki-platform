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
package org.xwiki.netflux.rest;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.netflux.rest.model.jaxb.EntityChannel;
import org.xwiki.rest.XWikiRestException;

/**
 * Exposes the Netflux (real-time communication) channels associated to a wiki page through REST.
 * 
 * @version $Id$
 * @since 13.9RC1
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/channels")
public interface PageChannelsResource
{
    /**
     * Returns the channels associated with a given wiki page.
     * 
     * @param wikiName the wiki component of the page reference
     * @param spaceNames the space component of the page reference
     * @param pageName the page name
     * @param paths used to retrieve only the channels mapped to the specified paths
     * @param create whether to create or not the missing channels corresponding to the specified paths
     * @return the list of found channels
     * @throws XWikiRestException if retrieving the list of channels fails
     */
    @GET
    List<EntityChannel> getChannels(@PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames, @PathParam("pageName") String pageName,
        @QueryParam("path") List<String> paths, @QueryParam("create") @DefaultValue("false") Boolean create)
        throws XWikiRestException;
}
