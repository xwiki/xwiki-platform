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

package org.xwiki.notifications.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/watch")
public interface NotificationsWatchResource
{
    @Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}")
    @GET
    Response getPageWatchStatus(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String pageName) throws Exception;

    @Path("/wikis/{wikiName}")
    @PUT
    Response watchWiki(
        @PathParam("wikiName") String wikiName);

    @Path("/wikis/{wikiName}/spaces/{spaceName: .+}")
    @PUT
    Response watchSpace(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames) throws Exception;

    @Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}")
    @PUT
    Response watchPage(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String pageName) throws Exception;

    @Path("/wikis/{wikiName}")
    @DELETE
    Response unwatchWiki(
        @PathParam("wikiName") String wikiName);

    @Path("/wikis/{wikiName}/spaces/{spaceName: .+}")
    @DELETE
    Response unwatchSpace(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames) throws Exception;

    @Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}")
    @DELETE
    Response unwatchPage(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String pageName) throws Exception;
}
