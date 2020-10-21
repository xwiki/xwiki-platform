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
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.LiveDataPropertyResource;
import org.xwiki.livedata.rest.model.jaxb.PropertyDescriptor;
import org.xwiki.livedata.rest.model.jaxb.StringMap;

/**
 * Default implementation of {@link LiveDataPropertyResource}.
 * 
 * @version $Id$
 * @since 12.9
 */
@Component
@Named("org.xwiki.livedata.internal.rest.DefaultLiveDataPropertyResource")
@Singleton
public class DefaultLiveDataPropertyResource extends AbstractLiveDataResource implements LiveDataPropertyResource
{
    @Override
    public PropertyDescriptor getProperty(String hint, StringMap sourceParams, String id, String namespace)
        throws Exception
    {
        Optional<LiveDataSource> source = getLiveDataSource(hint, sourceParams, namespace);
        if (source.isPresent()) {
            Optional<LiveDataPropertyDescriptor> property = source.get().getProperties().get(id);
            if (property.isPresent()) {
                return createProperty(property.get(), hint, namespace);
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @Override
    public Response updateProperty(String hint, StringMap sourceParams, String id,
        PropertyDescriptor propertyDescriptor, String namespace) throws Exception
    {
        Optional<LiveDataSource> source = getLiveDataSource(hint, sourceParams, namespace);
        if (source.isPresent()) {
            // Force the id specified in the URL.
            propertyDescriptor.setId(id);
            if (source.get().getProperties().update(convert(propertyDescriptor))) {
                Optional<LiveDataPropertyDescriptor> property = source.get().getProperties().get(id);
                if (property.isPresent()) {
                    PropertyDescriptor updatedProperty = createProperty(property.get(), hint, namespace);
                    return Response.status(Status.ACCEPTED).entity(updatedProperty).build();
                }
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @Override
    public void deleteProperty(String hint, StringMap sourceParams, String id, String namespace) throws Exception
    {
        Optional<LiveDataSource> source = getLiveDataSource(hint, sourceParams, namespace);
        if (source.isPresent()) {
            Optional<LiveDataPropertyDescriptor> removedProperty = source.get().getProperties().remove(id);
            if (removedProperty.isPresent()) {
                return;
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
}
