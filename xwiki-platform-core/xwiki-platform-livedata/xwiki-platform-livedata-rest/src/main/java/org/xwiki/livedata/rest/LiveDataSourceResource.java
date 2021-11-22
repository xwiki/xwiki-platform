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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.model.jaxb.Source;

/**
 * Exposes a {@link LiveDataSource} through REST.
 * 
 * @version $Id$
 * @since 12.10
 */
@Path("/liveData/sources/{sourceId}")
public interface LiveDataSourceResource
{
    /**
     * Provides information about the specified live data source component.
     * 
     * @param sourceId indicates the live data source component implementation
     * @param namespace the component manager name-space where to look for the specified live data source; if not
     *            specified then the context / current names-pace is used
     * @return information about the specified live data source
     * @throws Exception if retrieving the specified live data source fails
     */
    @GET
    Source getSource(
        @PathParam("sourceId") String sourceId,
        @QueryParam("namespace") @DefaultValue("") String namespace
    ) throws Exception;
}
