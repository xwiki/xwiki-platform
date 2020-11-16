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

import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.LiveDataEntryResource;
import org.xwiki.livedata.rest.model.jaxb.Entry;

/**
 * Default implementation of {@link LiveDataEntryResource}.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component
@Named("org.xwiki.livedata.internal.rest.DefaultLiveDataEntryResource")
@Singleton
public class DefaultLiveDataEntryResource extends AbstractLiveDataResource implements LiveDataEntryResource
{
    @Override
    public Entry getEntry(String sourceId, String namespace, String entryId) throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            LiveDataEntryStore entryStore = source.get().getEntries();
            Optional<Map<String, Object>> values = entryStore.get(entryId);
            if (values.isPresent()) {
                return createEntry(values.get(), entryId, querySource, namespace);
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @Override
    public Response updateEntry(String sourceId, String namespace, String entryId, Entry entry) throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            LiveDataEntryStore entryStore = source.get().getEntries();
            entry.getValues().put(entryStore.getIdProperty(), entryId);
            Optional<Object> updatedEntryId = entryStore.update(entry.getValues());
            if (updatedEntryId.isPresent()) {
                Optional<Map<String, Object>> values = entryStore.get(updatedEntryId.get());
                if (values.isPresent()) {
                    Entry updatedEntry = createEntry(values.get(), updatedEntryId.get(), querySource, namespace);
                    return Response.status(Status.ACCEPTED).entity(updatedEntry).build();
                }
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @Override
    public void deleteEntry(String sourceId, String namespace, String entryId) throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            Optional<Map<String, Object>> removedEntry = source.get().getEntries().remove(entryId);
            if (removedEntry.isPresent()) {
                return;
            }
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
}
