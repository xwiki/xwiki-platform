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
import org.xwiki.livedata.rest.model.jaxb.PropertyDescriptor;

/**
 * Represents a live data property.
 * 
 * @version $Id$
 * @since 12.10
 */
@Path("/liveData/sources/{sourceId}/properties/{propertyId}")
public interface LiveDataPropertyResource
{
    /**
     * Provides information about a live data property.
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @param propertyId identifies the live data property
     * @return the descriptor of the specified live data property
     * @throws Exception if retrieving the property fails
     */
    @GET
    PropertyDescriptor getProperty(
        @PathParam("sourceId") String sourceId,
        @QueryParam("namespace") @DefaultValue("") String namespace,
        @PathParam("propertyId") String propertyId
    ) throws Exception;

    /**
     * Updates the descriptor of a live data property.
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @param propertyId identifies the live data property to update
     * @param propertyDescriptor the updated descriptor of the specified property
     * @return the response
     * @throws Exception if updating the property fails
     */
    @PUT
    Response updateProperty(
        @PathParam("sourceId") String sourceId,
        @QueryParam("namespace") @DefaultValue("") String namespace,
        @PathParam("propertyId") String propertyId,
        PropertyDescriptor propertyDescriptor
    ) throws Exception;

    /**
     * Deletes a live data property.
     * 
     * @param sourceId indicates the {@link LiveDataSource} component implementation
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @param propertyId identifies the live data property to delete
     * @throws Exception if deleting the specified property fails
     */
    @DELETE
    void deleteProperty(
        @PathParam("sourceId") String sourceId,
        @QueryParam("namespace") @DefaultValue("") String namespace,
        @PathParam("propertyId") String propertyId
    ) throws Exception;
}
