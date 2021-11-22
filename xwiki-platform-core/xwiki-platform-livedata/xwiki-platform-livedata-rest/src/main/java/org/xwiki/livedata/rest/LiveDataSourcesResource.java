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
import javax.ws.rs.QueryParam;

import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.model.jaxb.Sources;

/**
 * Provides the list of existing {@link LiveDataSource} implementations.
 * 
 * @version $Id$
 * @since 12.10
 */
@Path("/liveData/sources")
public interface LiveDataSourcesResource
{
    /**
     * Looks for {@link LiveDataSource} implementations in the current name-space or the specified one and returns the
     * list.
     * 
     * @param namespace the component manager name-space where to look for {@link LiveDataSource} implementations; if
     *            not specified then the context / current name-space is used
     * @return the list of {@link LiveDataSource} implementations available in the specified name-space or the current
     *         name-space
     * @throws Exception if retrieving the list of {@link LiveDataSource} implementations fails
     */
    @GET
    Sources getSources(
        @QueryParam("namespace") @DefaultValue("") String namespace
    ) throws Exception;
}
