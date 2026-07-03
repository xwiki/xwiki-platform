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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.rest.LiveDataSourcesResource;
import org.xwiki.livedata.rest.model.jaxb.Source;
import org.xwiki.livedata.rest.model.jaxb.Sources;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Link;

/**
 * Default implementation of {@link LiveDataSourcesResource}.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Named("org.xwiki.livedata.internal.rest.DefaultLiveDataSourcesResource")
public class DefaultLiveDataSourcesResource extends AbstractLiveDataResource implements LiveDataSourcesResource
{
    @Override
    public Sources getSources(String namespace) throws Exception
    {
        Optional<Collection<String>> sourceIds = this.liveDataSourceManager.getAvailableSources(namespace);
        if (sourceIds.isPresent()) {
            Link self = new Link().withRel(Relations.SELF).withHref(this.uriInfo.getAbsolutePath().toString());

            List<Source> sources = sourceIds.get().stream().map(this::getLiveDataQuerySource)
                .map(querySource -> createSource(querySource, namespace)).collect(Collectors.toList());
            return (Sources) new Sources().withSources(sources).withLinks(self);
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
}
