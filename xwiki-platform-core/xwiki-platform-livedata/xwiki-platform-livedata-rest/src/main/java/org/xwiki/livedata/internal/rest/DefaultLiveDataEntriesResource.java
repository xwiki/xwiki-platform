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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.LiveDataEntriesResource;
import org.xwiki.livedata.rest.LiveDataEntryResource;
import org.xwiki.livedata.rest.LiveDataSourceResource;
import org.xwiki.livedata.rest.model.jaxb.Entries;
import org.xwiki.livedata.rest.model.jaxb.Entry;
import org.xwiki.livedata.rest.model.jaxb.StringMap;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;

/**
 * Default implementation of {@link LiveDataEntriesResource}.
 * 
 * @version $Id$
 * @since 12.9
 */
@Component
@Named("org.xwiki.livedata.internal.rest.DefaultLiveDataEntriesResource")
@Singleton
public class DefaultLiveDataEntriesResource extends AbstractLiveDataResource implements LiveDataEntriesResource
{
    @Override
    public Entries getEntries(String hint, LiveDataQuery query, String namespace) throws Exception
    {
        // Prevent null pointer exceptions.
        query.initialize();

        Optional<LiveDataSource> source = getLiveDataSource(hint, query.getSource().getParameters(), namespace);
        if (source.isPresent()) {
            return createEntries(source.get().getEntries().get(query), hint, namespace);
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @Override
    public Response addEntry(String hint, StringMap parameters, String namespace, Entry entry) throws Exception
    {
        Optional<LiveDataSource> source = getLiveDataSource(hint, parameters, namespace);
        if (source.isPresent()) {
            Optional<Object> entryId = source.get().getEntries().add(entry.getValues());
            if (entryId.isPresent()) {
                Optional<Map<String, Object>> values = source.get().getEntries().get(entryId);
                if (values.isPresent()) {
                    Entry addedEntry = createEntry(values.get(), hint, namespace);
                    URI location =
                        Utils.createURI(this.uriInfo.getBaseUri(), LiveDataEntryResource.class, hint, entryId);
                    location = withNamespaceAndParams(location, namespace, parameters);
                    return Response.created(location).entity(addedEntry).build();
                }
            }

            // The entry was not added.
            return null;
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    private Entries createEntries(LiveData liveData, String hint, String namespace)
    {
        Link self = new Link().withRel(Relations.SELF).withHref(this.uriInfo.getAbsolutePath().toString());
        Link parent = withNamespace(createLink(Relations.PARENT, LiveDataSourceResource.class, hint), namespace);

        List<Entry> entries = liveData.getEntries().stream().map(values -> this.createEntry(values, hint, namespace))
            .collect(Collectors.toList());
        return (Entries) new Entries().withEntries(entries).withLinks(self, parent);
    }
}
