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

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.LiveDataPropertiesResource;
import org.xwiki.livedata.rest.LiveDataPropertyResource;
import org.xwiki.livedata.rest.LiveDataSourceResource;
import org.xwiki.livedata.rest.model.jaxb.Properties;
import org.xwiki.livedata.rest.model.jaxb.PropertyDescriptor;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;

/**
 * Default implementation of {@link LiveDataPropertiesResource}.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Named("org.xwiki.livedata.internal.rest.DefaultLiveDataPropertiesResource")
public class DefaultLiveDataPropertiesResource extends AbstractLiveDataResource implements LiveDataPropertiesResource
{
    @Override
    public Properties getProperties(String sourceId, String namespace) throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            return createProperties(source.get().getProperties().get(), querySource, namespace);
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    private Properties createProperties(Collection<LiveDataPropertyDescriptor> propertyDescriptors,
        LiveDataQuery.Source source, String namespace)
    {
        Link self = new Link().withRel(Relations.SELF).withHref(this.uriInfo.getAbsolutePath().toString());
        Link parent =
            withNamespaceAndSourceParams(createLink(Relations.PARENT, LiveDataSourceResource.class, source.getId()),
                namespace, source.getParameters());

        List<PropertyDescriptor> properties = propertyDescriptors.stream()
            .map(propertyDescriptor -> createPropertyType(propertyDescriptor, source, namespace))
            .collect(Collectors.toList());
        return (Properties) new Properties().withProperties(properties).withLinks(self, parent);
    }

    @Override
    public Response addProperty(String sourceId, String namespace, PropertyDescriptor propertyDescriptor)
        throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            if (source.get().getProperties().save(convert(propertyDescriptor))) {
                Optional<LiveDataPropertyDescriptor> property =
                    source.get().getProperties().get(propertyDescriptor.getId());
                if (property.isPresent()) {
                    PropertyDescriptor addedProperty = createProperty(property.get(), querySource, namespace);
                    URI location = Utils.createURI(this.uriInfo.getBaseUri(), LiveDataPropertyResource.class,
                        querySource.getId(), addedProperty.getId());
                    location = withNamespaceAndSourceParams(location, namespace, querySource.getParameters());
                    return Response.created(location).entity(addedProperty).build();
                }
            }

            // The property was not added.
            return null;
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
}
