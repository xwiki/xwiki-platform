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
package org.xwiki.livedata.internal.rest;

import java.util.Optional;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.LiveDataPropertyResource;
import org.xwiki.livedata.rest.model.jaxb.PropertyDescriptor;

/**
 * Default implementation of {@link LiveDataPropertyResource}.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Named("org.xwiki.livedata.internal.rest.DefaultLiveDataPropertyResource")
public class DefaultLiveDataPropertyResource extends AbstractLiveDataResource implements LiveDataPropertyResource
{
    @Override
    public PropertyDescriptor getProperty(String sourceId, String namespace, String propertyId) throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            Optional<LiveDataPropertyDescriptor> property = source.get().getProperties().get(propertyId);
            if (property.isPresent()) {
                return createProperty(property.get(), querySource, namespace);
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @Override
    public Response updateProperty(String sourceId, String namespace, String propertyId,
        PropertyDescriptor propertyDescriptor) throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            // Force the id specified in the URL.
            propertyDescriptor.setId(propertyId);
            if (source.get().getProperties().save(convert(propertyDescriptor))) {
                Optional<LiveDataPropertyDescriptor> property = source.get().getProperties().get(propertyId);
                if (property.isPresent()) {
                    PropertyDescriptor updatedProperty = createProperty(property.get(), querySource, namespace);
                    return Response.status(Status.ACCEPTED).entity(updatedProperty).build();
                }
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @Override
    public void deleteProperty(String sourceId, String namespace, String propertyId) throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            Optional<LiveDataPropertyDescriptor> removedProperty = source.get().getProperties().remove(propertyId);
            if (removedProperty.isPresent()) {
                return;
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
}
