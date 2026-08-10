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
package org.xwiki.rest.resources.wikis;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Pages;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/pages")
public interface WikiPagesResource
{
    /**
     * Returns the pages stored in the given wiki, with optional filtering and pagination.
     *
     * @param wikiName the identifier of the wiki whose pages are listed, for example {@code xwiki} for the main wiki
     * @param start the 0-based index of the first page to return, used together with {@code number} for pagination;
     *  defaults to {@code 0}
     * @param name keeps only pages whose full name contains this value (case-insensitive), for example
     *  {@code Main.WebHome}; empty by default (no filtering)
     * @param space keeps only pages located in a space whose reference contains this value (case-insensitive), for
     *  example {@code Main}; nested spaces are written {@code A/spaces/B} for the space {@code A.B}; empty by default
     *  (no filtering)
     * @param author keeps only pages whose content author contains this value (case-insensitive), for example
     *  {@code XWiki.Admin}; empty by default (no filtering)
     * @param number the maximum number of pages to return; defaults to {@code 25}, and a value that is negative or
     *  larger than the wiki's configured REST query limit is rejected with a {@code 400} response
     * @return the matching pages the current user is allowed to view, within the requested pagination window
     * @throws XWikiRestException if the pages cannot be retrieved
     */
    @GET Pages getPages(
            @PathParam("wikiName") String wikiName,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("name") @DefaultValue("") String name,
            @QueryParam("space") @DefaultValue("") String space,
            @QueryParam("author") @DefaultValue("") String author,
            @QueryParam("number") @DefaultValue("25") Integer number
    ) throws XWikiRestException;
}
