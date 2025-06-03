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

import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.LiveDataPropertyTypeResource;
import org.xwiki.livedata.rest.model.jaxb.PropertyDescriptor;

/**
 * Default implementation of {@link LiveDataPropertyTypeResource}.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Named("org.xwiki.livedata.internal.rest.DefaultLiveDataPropertyTypeResource")
public class DefaultLiveDataPropertyTypeResource extends AbstractLiveDataResource
    implements LiveDataPropertyTypeResource
{
    @Override
    public PropertyDescriptor getType(String sourceId, String namespace, String typeId) throws Exception
    {
        LiveDataConfiguration config = getLiveDataConfig(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(config.getQuery().getSource(), namespace);
        if (source.isPresent()) {
            Optional<LiveDataPropertyDescriptor> propertyType = config.getMeta().getPropertyTypes().stream()
                .filter(type -> Objects.equals(typeId, type.getId())).findFirst();
            if (propertyType.isPresent()) {
                return createPropertyType(propertyType.get(), config.getQuery().getSource(), namespace);
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
}
