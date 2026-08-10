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
import org.xwiki.rest.model.jaxb.Attachments;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/attachments")
public interface WikiAttachmentsResource
{
    /**
     * Returns the attachments stored in the given wiki, with optional filtering and pagination.
     *
     * @param wikiName the identifier of the wiki whose attachments are listed, for example {@code xwiki} for the main
     *  wiki
     * @param name keeps only attachments whose file name contains this value (case-insensitive), for example
     *  {@code logo}; empty by default (no filtering)
     * @param page keeps only attachments held by a page whose full name contains this value (case-insensitive), for
     *  example {@code Main.WebHome}; empty by default (no filtering)
     * @param space keeps only attachments located in a space whose reference contains this value (case-insensitive),
     *  for example {@code Main}; empty by default (no filtering)
     * @param author keeps only attachments whose author reference contains this value (case-insensitive), for example
     *  {@code XWiki.Admin}; empty by default (no filtering)
     * @param types a comma-separated list of media types and/or dot-prefixed file-name extensions to keep, for example
     *  {@code image/png,.pdf}; empty by default (no filtering)
     * @param start the 0-based index of the first attachment to return, used together with {@code number} for
     *  pagination; defaults to {@code 0}
     * @param number the maximum number of attachments to return; defaults to {@code 25}, and a value that is negative
     *  or larger than the wiki's configured REST query limit is rejected with a {@code 400} response
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in addition to the technical references, at some extra cost; defaults to
     *  {@code false}
     * @return the matching attachments the current user is allowed to view, within the requested pagination window
     * @throws XWikiRestException if the attachments cannot be retrieved
     */
    // Needs a lot of parameters to bind path and query parameters
    @SuppressWarnings("checkstyle:ParameterNumber")
    @GET Attachments getAttachments(
            @PathParam("wikiName") String wikiName,
            @QueryParam("name") @DefaultValue("") String name,
            @QueryParam("page") @DefaultValue("") String page,
            @QueryParam("space") @DefaultValue("") String space,
            @QueryParam("author") @DefaultValue("") String author,
            @QueryParam("types") @DefaultValue("") String types,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") @DefaultValue("25") Integer number,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
