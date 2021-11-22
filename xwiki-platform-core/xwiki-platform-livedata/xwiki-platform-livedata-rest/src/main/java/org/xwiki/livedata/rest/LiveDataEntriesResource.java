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
package org.xwiki.livedata.rest;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.model.jaxb.Entries;
import org.xwiki.livedata.rest.model.jaxb.Entry;

/**
 * Provides the list of entries from an existing {@link LiveDataSource}.
 * 
 * @version $Id$
 * @since 12.10
 */
@Path("/liveData/sources/{sourceId}/entries")
public interface LiveDataEntriesResource
{
    /**
     * Provides the list of live data entries. Here's an example URL:
     * 
     * <pre>{@code
     * /liveData/sources/liveTable/entries?
     * 
     *   namespace=wiki%3Axwiki&
     * 
     *   sourceParams.className=Help.Applications.Movies.Code.MoviesClass&
     *   sourceParams.translationPrefix=movies.livetable.&
     * 
     *   properties=doc.title&
     *   properties=genre&
     *   properties=releaseDate&
     *   properties=director&
     *   properties=_actions&
     * 
     *   filters.doc.title=contains%3Amee&
     *   matchAll=doc.title&
     * 
     *   sort=releaseDate&
     *   descending=false&
     * 
     *   offset=0&
     *   limit=10
     * }</pre>
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @param properties the list of properties to include in the returned live data entries
     * @param matchAll the list of properties for which to match all filter constraints
     * @param sort the list of properties to sort on
     * @param descending indicates the sort direction for the properties specified by the {@code sort} parameter
     * @param offset the index of the first entry to return
     * @param limit the maximum number of entries to return
     * @return the list of live data entries that match the query
     * @throws Exception if retrieving the live data entries fails
     */
    @GET
    Entries getEntries(
        @PathParam("sourceId") String sourceId,
        @QueryParam("namespace") @DefaultValue("") String namespace,
        @QueryParam("properties") List<String> properties,
        @QueryParam("matchAll") List<String> matchAll,
        @QueryParam("sort") List<String> sort,
        @QueryParam("descending") List<Boolean> descending,
        @QueryParam("offset") @DefaultValue("0") long offset,
        @QueryParam("limit") @DefaultValue("10") int limit
    ) throws Exception;

    /**
     * Adds the given entry to the specified live data source.
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @param entry the entry to add
     * @return the response
     * @throws Exception if adding the entry fails
     */
    @POST
    Response addEntry(
        @PathParam("sourceId") String sourceId,
        @QueryParam("namespace") @DefaultValue("") String namespace,
        Entry entry
    ) throws Exception;
}
