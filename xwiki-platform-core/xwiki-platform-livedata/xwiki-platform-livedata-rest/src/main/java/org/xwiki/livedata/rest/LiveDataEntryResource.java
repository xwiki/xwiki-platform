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

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.model.jaxb.Entry;
import org.xwiki.livedata.rest.model.jaxb.StringMap;
import org.xwiki.stability.Unstable;

/**
 * Represents a live data entry.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Path("/liveData/sources/{sourceId}/entries/{entryId}")
@Unstable
public interface LiveDataEntryResource
{
    /**
     * Fetches an entry from the specified live data source.
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param sourceParams the live data source parameters
     * @param entryId identifies the entry to retrieve
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @return the specified entry
     * @throws Exception if retrieving the entry fails
     */
    @GET
    Entry getEntry(
        @PathParam("sourceId") String sourceId,
        @QueryParam("sourceParams") @DefaultValue("{}") StringMap sourceParams,
        @PathParam("entryId") String entryId,
        @QueryParam("namespace") @DefaultValue("") String namespace
    ) throws Exception;

    /**
     * Updates an existing entry.
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param sourceParams the live data source parameters
     * @param entry the new entry values
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @param entryId identifies the entry to update
     * @return the response
     * @throws Exception if updating the specified entry fails
     */
    @PUT
    Response updateEntry(
        @PathParam("sourceId") String sourceId,
        @QueryParam("sourceParams") @DefaultValue("{}") StringMap sourceParams,
        @PathParam("entryId") String entryId,
        Entry entry,
        @QueryParam("namespace") @DefaultValue("") String namespace
    ) throws Exception;

    /**
     * Deletes an entry from the data set.
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param sourceParams the live data source parameters
     * @param entryId identifies the entry to delete
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @throws Exception if deleting the specified entry fails
     */
    @DELETE
    void deleteEntry(
        @PathParam("sourceId") String sourceId,
        @QueryParam("sourceParams") @DefaultValue("{}") StringMap sourceParams,
        @PathParam("entryId") String entryId,
        @QueryParam("namespace") @DefaultValue("") String namespace
    ) throws Exception;
}
