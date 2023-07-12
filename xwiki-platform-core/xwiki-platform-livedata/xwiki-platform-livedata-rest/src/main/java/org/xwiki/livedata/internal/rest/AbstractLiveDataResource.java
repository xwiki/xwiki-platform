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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.rest.LiveDataEntriesResource;
import org.xwiki.livedata.rest.LiveDataEntryResource;
import org.xwiki.livedata.rest.LiveDataPropertiesResource;
import org.xwiki.livedata.rest.LiveDataPropertyResource;
import org.xwiki.livedata.rest.LiveDataPropertyTypeResource;
import org.xwiki.livedata.rest.LiveDataPropertyTypesResource;
import org.xwiki.livedata.rest.LiveDataSourceResource;
import org.xwiki.livedata.rest.LiveDataSourcesResource;
import org.xwiki.livedata.rest.model.jaxb.Entry;
import org.xwiki.livedata.rest.model.jaxb.PropertyDescriptor;
import org.xwiki.livedata.rest.model.jaxb.Source;
import org.xwiki.livedata.rest.model.jaxb.StringMap;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;

/**
 * Base class for live data REST resources.
 * 
 * @version $Id$
 * @since 12.10
 */
public abstract class AbstractLiveDataResource extends XWikiResource
{
    private static final String SOURCE_PARAMS_PREFIX = "sourceParams.";

    @Inject
    protected LiveDataSourceManager liveDataSourceManager;

    @Inject
    protected LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigResolver;

    protected LiveDataConfiguration getLiveDataConfig(String sourceId) throws LiveDataException
    {
        LiveDataConfiguration config = new LiveDataConfiguration();
        config.setQuery(new LiveDataQuery());
        config.getQuery().setSource(getLiveDataQuerySource(sourceId));
        return this.defaultLiveDataConfigResolver.resolve(config);
    }

    protected LiveDataQuery.Source getLiveDataQuerySource(String sourceId)
    {
        LiveDataQuery.Source source = new LiveDataQuery.Source(sourceId);
        source.getParameters().putAll(getLiveDataSourceParameters());
        return source;
    }

    protected Map<String, Object> getLiveDataSourceParameters()
    {
        Map<String, Object> params = new HashMap<>();
        this.uriInfo.getQueryParameters().forEach((key, values) -> {
            if (key.startsWith(SOURCE_PARAMS_PREFIX)) {
                Object value = values.size() == 1 ? values.get(0) : values;
                params.put(key.substring(SOURCE_PARAMS_PREFIX.length()), value);
            }
        });
        return params;
    }

    protected Link createLink(String relation, java.lang.Class<?> resourceClass, java.lang.Object... pathElements)
    {
        String href = Utils.createURI(this.uriInfo.getBaseUri(), resourceClass, pathElements).toString();
        return new Link().withRel(relation).withHref(href);
    }

    protected Source createSource(LiveDataQuery.Source source, String namespace)
    {
        Link self =
            withNamespaceAndSourceParams(createLink(Relations.SELF, LiveDataSourceResource.class, source.getId()),
                namespace, source.getParameters());

        Link parent = withNamespace(createLink(Relations.PARENT, LiveDataSourcesResource.class), namespace);

        Link entries = withNamespaceAndSourceParams(
            createLink("http://www.xwiki.org/rel/entries", LiveDataEntriesResource.class, source.getId()), namespace,
            source.getParameters());

        Link properties = withNamespaceAndSourceParams(
            createLink(Relations.PROPERTIES, LiveDataPropertiesResource.class, source.getId()), namespace,
            source.getParameters());

        Link propertyTypes = withNamespaceAndSourceParams(
            createLink("http://www.xwiki.org/rel/propertyTypes", LiveDataPropertyTypesResource.class, source.getId()),
            namespace, source.getParameters());

        return (Source) new Source().withHint(source.getId()).withLinks(self, parent, entries, properties,
            propertyTypes);
    }

    protected Link withNamespace(Link link, String namespace)
    {
        return withNamespaceAndSourceParams(link, namespace, Collections.emptyMap());
    }

    protected Link withNamespaceAndSourceParams(Link link, String namespace, Map<String, Object> sourceParams)
    {
        try {
            link.setHref(withNamespaceAndSourceParams(new URI(link.getHref()), namespace, sourceParams).toString());
        } catch (URISyntaxException e) {
            // Shouldn't happen.
        }
        return link;
    }

    protected URI withNamespaceAndSourceParams(URI uri, String namespace, Map<String, Object> sourceParams)
    {
        UriBuilder uriBuilder = UriBuilder.fromUri(uri);
        if (!StringUtils.isEmpty(namespace)) {
            uriBuilder.queryParam("namespace", namespace);
        }
        sourceParams.entrySet().forEach(entry -> {
            Iterable<?> values;
            if (entry.getValue() instanceof Iterable) {
                values = (Iterable<?>) entry.getValue();
            } else {
                values = Collections.singletonList(entry.getValue());
            }
            values.forEach(value -> uriBuilder.queryParam(SOURCE_PARAMS_PREFIX + entry.getKey(), value));
        });
        return uriBuilder.build();
    }

    protected PropertyDescriptor createPropertyDescriptor(LiveDataPropertyDescriptor descriptor)
    {
        // Prevent null pointer exceptions.
        descriptor.initialize();

        StringMap icon = new StringMap();
        icon.putAll(descriptor.getIcon());

        return new PropertyDescriptor().withId(descriptor.getId()).withName(descriptor.getName())
            .withDescription(descriptor.getDescription()).withIcon(icon).withVisible(descriptor.isVisible())
            .withDisplayer(descriptor.getDisplayer()).withSortable(descriptor.isSortable())
            .withFilterable(descriptor.isFilterable()).withFilter(descriptor.getFilter()).withType(descriptor.getType())
            .withStyleName(descriptor.getStyleName());
    }

    protected PropertyDescriptor createProperty(LiveDataPropertyDescriptor descriptor, LiveDataQuery.Source source,
        String namespace)
    {
        PropertyDescriptor property = createPropertyDescriptor(descriptor);

        Link self = withNamespaceAndSourceParams(
            createLink(Relations.SELF, LiveDataPropertyResource.class, source.getId(), property.getId()), namespace,
            source.getParameters());
        property.getLinks().add(self);

        Link parent =
            withNamespaceAndSourceParams(createLink(Relations.PARENT, LiveDataPropertiesResource.class, source.getId()),
                namespace, source.getParameters());
        property.getLinks().add(parent);

        return property;
    }

    protected PropertyDescriptor createPropertyType(LiveDataPropertyDescriptor descriptor, LiveDataQuery.Source source,
        String namespace)
    {
        PropertyDescriptor propertyType = createPropertyDescriptor(descriptor);

        Link self = withNamespaceAndSourceParams(
            createLink(Relations.SELF, LiveDataPropertyTypeResource.class, source.getId(), propertyType.getId()),
            namespace, source.getParameters());
        propertyType.getLinks().add(self);

        Link parent = withNamespaceAndSourceParams(
            createLink(Relations.PARENT, LiveDataPropertyTypesResource.class, source.getId()), namespace,
            source.getParameters());
        propertyType.getLinks().add(parent);

        return propertyType;
    }

    protected Entry createEntry(Map<String, Object> values, Object entryId, LiveDataQuery.Source source,
        String namespace)
    {
        List<Link> links = new ArrayList<>();

        if (entryId != null) {
            links.add(withNamespaceAndSourceParams(
                createLink(Relations.SELF, LiveDataEntryResource.class, source.getId(), entryId), namespace,
                source.getParameters()));
        }

        links.add(
            withNamespaceAndSourceParams(createLink(Relations.PARENT, LiveDataEntriesResource.class, source.getId()),
                namespace, source.getParameters()));

        return (Entry) new Entry().withValues(StringMap.fromMap(values)).withLinks(links);
    }

    protected LiveDataPropertyDescriptor convert(PropertyDescriptor descriptor)
    {
        LiveDataPropertyDescriptor propertyDescriptor = new LiveDataPropertyDescriptor();
        propertyDescriptor.setId(descriptor.getId());
        propertyDescriptor.setName(descriptor.getName());
        propertyDescriptor.setDescription(descriptor.getDescription());
        propertyDescriptor.getIcon().putAll(descriptor.getIcon());
        propertyDescriptor.setType(descriptor.getType());
        propertyDescriptor.setVisible(descriptor.isVisible());
        propertyDescriptor.setSortable(descriptor.isSortable());
        propertyDescriptor.setFilterable(descriptor.isFilterable());
        propertyDescriptor.setDisplayer(descriptor.getDisplayer());
        propertyDescriptor.setFilter(descriptor.getFilter());
        propertyDescriptor.setStyleName(descriptor.getStyleName());
        return propertyDescriptor;
    }
}
