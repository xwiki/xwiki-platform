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
package org.xwiki.rest.resources.pages;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Tags;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/tags")
public interface PageTagsResource
{
    @GET Tags getPageTags(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") String spaceName,
            @PathParam("pageName") String pageName
    ) throws XWikiRestException;

    @PUT Response setTags(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") String spaceName,
            @PathParam("pageName") String pageName,
            Tags tags
    ) throws XWikiRestException;
}
