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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.livedata.LiveDataSource;
import org.xwiki.stability.Unstable;

/**
 * Represents a property of a live data entry.
 * 
 * @version $Id$
 * @since 12.10
 */
@Path("/liveData/sources/{sourceId}/entries/{entryId}/properties/{propertyId}")
@Unstable
public interface LiveDataEntryPropertyResource
{
    /**
     * Retrieves the value of a property from a specified live data entry.
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @param entryId identifies the live data entry
     * @param propertyId identifies the property whose value to return
     * @return the value of the specified property
     * @throws Exception if retrieving the property value fails
     */
    @GET
    Object getProperty(
        @PathParam("sourceId") String sourceId,
        @QueryParam("namespace") @DefaultValue("") String namespace,
        @PathParam("entryId") String entryId,
        @PathParam("propertyId") String propertyId
    ) throws Exception;

    /**
     * Sets the value of a property from a live data entry.
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation whose entry needs to be updated
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @param entryId identifies the live data entry whose property needs to be set
     * @param propertyId identifies the property whose value needs to be set
     * @param value the new property value
     * @return the response
     * @throws Exception if settings the property value fails
     */
    @PUT
    Response setProperty(
        @PathParam("sourceId") String sourceId,
        @QueryParam("namespace") @DefaultValue("") String namespace,
        @PathParam("entryId") String entryId,
        @PathParam("propertyId") String propertyId,
        String value
    ) throws Exception;
}
