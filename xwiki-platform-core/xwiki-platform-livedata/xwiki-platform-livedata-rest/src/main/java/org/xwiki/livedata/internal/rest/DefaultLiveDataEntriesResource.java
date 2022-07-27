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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.rest.LiveDataEntriesResource;
import org.xwiki.livedata.rest.LiveDataEntryResource;
import org.xwiki.livedata.rest.LiveDataSourceResource;
import org.xwiki.livedata.rest.model.jaxb.Entries;
import org.xwiki.livedata.rest.model.jaxb.Entry;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;

/**
 * Default implementation of {@link LiveDataEntriesResource}.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Named("org.xwiki.livedata.internal.rest.DefaultLiveDataEntriesResource")
@Singleton
public class DefaultLiveDataEntriesResource extends AbstractLiveDataResource implements LiveDataEntriesResource
{
    private static final String FILTERS_PREFIX = "filters.";

    @Inject
    private LiveDataResourceContextInitializer contextInitializer;

    @Override
    public Entries getEntries(String sourceId, String namespace, List<String> properties, List<String> matchAll,
        List<String> sort, List<Boolean> descending, long offset, int limit) throws Exception
    {
        this.contextInitializer.initialize(namespace);

        LiveDataConfiguration config = initConfig(sourceId, properties, matchAll, sort, descending, offset, limit);
        return getEntries(namespace, offset, limit, config);
    }

    @Override
    public Response addEntry(String sourceId, String namespace, Entry entry) throws Exception
    {
        LiveDataQuery.Source querySource = getLiveDataQuerySource(sourceId);
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(querySource, namespace);
        if (source.isPresent()) {
            LiveDataEntryStore entryStore = source.get().getEntries();
            Optional<Object> entryId = entryStore.save(entry.getValues());
            if (entryId.isPresent()) {
                Optional<Map<String, Object>> values = entryStore.get(entryId.get());
                if (values.isPresent()) {
                    Entry addedEntry = createEntry(values.get(), entryId.get(), querySource, namespace);
                    URI location = Utils.createURI(this.uriInfo.getBaseUri(), LiveDataEntryResource.class, sourceId,
                        entryId.get());
                    location = withNamespaceAndSourceParams(location, namespace, querySource.getParameters());
                    return Response.created(location).entity(addedEntry).build();
                }
            }

            // The entry was not added.
            return null;
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    private LiveDataConfiguration getConfig(String sourceId, List<String> properties, List<String> matchAll,
        List<String> sort, List<Boolean> descending, long offset, int limit) throws LiveDataException
    {
        LiveDataQuery query = new LiveDataQuery();
        query.setSource(getLiveDataQuerySource(sourceId));
        query.setProperties(properties.stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList()));
        query.setFilters(getFilters(matchAll));
        query.setSort(getSort(sort, descending));
        query.setOffset(offset);
        query.setLimit(limit);

        LiveDataConfiguration config = new LiveDataConfiguration();
        config.setQuery(query);
        return this.defaultLiveDataConfigResolver.resolve(config);
    }

    private List<SortEntry> getSort(List<String> sortList, List<Boolean> descendingList)
    {
        List<SortEntry> sortEntries = new ArrayList<>();
        for (int i = 0; i < sortList.size(); i++) {
            String property = sortList.get(i);
            boolean descending = i < descendingList.size() ? descendingList.get(i) : false;
            sortEntries.add(new SortEntry(property, descending));
        }
        return sortEntries;
    }

    private List<Filter> getFilters(List<String> matchAll)
    {
        List<Filter> filters = new ArrayList<>();
        this.uriInfo.getQueryParameters().forEach((key, values) -> {
            if (key.startsWith(FILTERS_PREFIX)) {
                String property = key.substring(FILTERS_PREFIX.length());
                Filter filter = getFilter(property, matchAll.contains(property), values);
                if (!filter.getConstraints().isEmpty()) {
                    filters.add(filter);
                }
            }
        });
        return filters;
    }

    private Filter getFilter(String property, boolean matchAll, List<String> constraints)
    {
        Filter filter = new Filter();
        filter.setProperty(property);
        filter.setMatchAll(matchAll);
        String operatorSeparator = ":";
        for (String constraint : constraints) {
            // All constraint should have an operator.
            if (constraint.contains(operatorSeparator)) {
                String[] parts = constraint.split(operatorSeparator, 2);
                String value = parts[1];
                String operator = StringUtils.isBlank(parts[0]) ? null : parts[0];
                filter.getConstraints().add(new Constraint(value, operator));
            }
        }
        return filter;
    }

    private Entries createEntries(LiveData liveData, String idProperty, LiveDataQuery.Source source, String namespace)
    {
        Link self = new Link().withRel(Relations.SELF).withHref(this.uriInfo.getAbsolutePath().toString());
        Link parent =
            withNamespaceAndSourceParams(createLink(Relations.PARENT, LiveDataSourceResource.class, source.getId()),
                namespace, source.getParameters());

        List<Entry> entries = liveData.getEntries().stream()
            .map(values -> this.createEntry(values, values.get(idProperty), source, namespace))
            .collect(Collectors.toList());
        return (Entries) new Entries().withEntries(entries).withCount(liveData.getCount()).withLinks(self, parent);
    }

    private LiveDataConfiguration initConfig(String sourceId, List<String> properties, List<String> matchAll,
        List<String> sort, List<Boolean> descending, long offset, int limit) throws LiveDataException
    {
        // Workaround for https://github.com/restlet/restlet-framework-java/issues/922 (JaxRs multivalue 
        // query-params gives list with null element).
        List<String> actualProperties = properties.stream().filter(Objects::nonNull).collect(Collectors.toList());
        List<String> actualMatchAll = matchAll.stream().filter(Objects::nonNull).collect(Collectors.toList());
        List<String> actualSort = sort.stream().filter(Objects::nonNull).collect(Collectors.toList());
        List<Boolean> actualDescending = descending.stream().filter(Objects::nonNull).collect(Collectors.toList());
        return getConfig(sourceId, actualProperties, actualMatchAll, actualSort, actualDescending, offset, limit);
    }

    private Entries getEntries(String namespace, long offset, int limit, LiveDataConfiguration config)
        throws LiveDataException
    {
        Optional<LiveDataSource> source = this.liveDataSourceManager.get(config.getQuery().getSource(), namespace);
        if (source.isPresent()) {
            String idProperty = config.getMeta().getEntryDescriptor().getIdProperty();
            LiveDataEntryStore entryStore = source.get().getEntries();
            return createEntries(entryStore.get(config.getQuery()), idProperty, config.getQuery().getSource(),
                namespace).withOffset(offset).withLimit(limit);
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
}
