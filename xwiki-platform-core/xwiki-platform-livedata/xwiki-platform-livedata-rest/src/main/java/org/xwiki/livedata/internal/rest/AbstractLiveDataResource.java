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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
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
 * @since 12.6RC1
 */
public abstract class AbstractLiveDataResource extends XWikiResource
{
    @Inject
    protected LiveDataSourceManager liveDataSourceManager;

    protected Optional<LiveDataSource> getLiveDataSource(String sourceId, Map<String, Object> sourceParams,
        String namespace)
    {
        LiveDataQuery.Source source = new LiveDataQuery.Source();
        source.putAll(sourceParams);
        source.setId(sourceId);
        return this.liveDataSourceManager.get(source, namespace);
    }

    protected Link createLink(String relation, java.lang.Class<?> resourceClass, java.lang.Object... pathElements)
    {
        String href = Utils.createURI(this.uriInfo.getBaseUri(), resourceClass, pathElements).toString();
        return new Link().withRel(relation).withHref(href);
    }

    protected Source createSource(String hint, StringMap parameters, String namespace)
    {
        Link self = withNamespace(createLink(Relations.SELF, LiveDataSourceResource.class, hint), namespace);

        Link parent = withNamespace(createLink(Relations.PARENT, LiveDataSourcesResource.class), namespace);

        Link entries = withNamespace(
            createLink("http://www.xwiki.org/rel/entries", LiveDataEntriesResource.class, hint), namespace);

        Link properties =
            withNamespace(createLink(Relations.PROPERTIES, LiveDataPropertiesResource.class, hint), namespace);

        Link propertyTypes = withNamespace(
            createLink("http://www.xwiki.org/rel/propertyTypes", LiveDataPropertyTypesResource.class, hint), namespace);

        return (Source) new Source().withHint(hint).withLinks(self, parent, entries, properties, propertyTypes);
    }

    protected Link withNamespace(Link link, String namespace)
    {
        if (!StringUtils.isEmpty(namespace)) {
            try {
                link.setHref(link.getHref() + "?namespace=" + URLEncoder.encode(namespace, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // Shouldn't happen.
            }
        }
        return link;
    }

    protected URI withNamespaceAndParams(URI uri, String namespace, StringMap sourceParams)
    {
        return UriBuilder.fromUri(uri).queryParam("namespace", namespace)
            .queryParam("sourceParams", sourceParams.toString()).build();
    }

    protected PropertyDescriptor createPropertyDescriptor(LiveDataPropertyDescriptor descriptor)
    {
        StringMap icon = new StringMap();
        icon.putAll(descriptor.getIcon());

        StringMap displayer = new StringMap();
        displayer.putAll(descriptor.getDisplayer());

        StringMap filter = new StringMap();
        filter.putAll(descriptor.getFilter());

        return (PropertyDescriptor) new PropertyDescriptor().withId(descriptor.getId()).withName(descriptor.getName())
            .withDescription(descriptor.getDescription()).withIcon(icon).withSortable(descriptor.isSortable())
            .withDisplayer(displayer).withFilter(filter).withType(descriptor.getType())
            .withStyleName(descriptor.getStyleName());
    }

    protected PropertyDescriptor createProperty(LiveDataPropertyDescriptor descriptor, String sourceHint,
        String namespace)
    {
        PropertyDescriptor property = createPropertyDescriptor(descriptor);

        Link self = withNamespace(
            createLink(Relations.SELF, LiveDataPropertyResource.class, sourceHint, property.getId()), namespace);
        property.getLinks().add(self);

        Link parent =
            withNamespace(createLink(Relations.PARENT, LiveDataPropertiesResource.class, sourceHint), namespace);
        property.getLinks().add(parent);

        return property;
    }

    protected PropertyDescriptor createPropertyType(LiveDataPropertyDescriptor descriptor, String sourceHint,
        String namespace)
    {
        PropertyDescriptor propertyType = createPropertyDescriptor(descriptor);

        Link self = withNamespace(
            createLink(Relations.SELF, LiveDataPropertyTypeResource.class, sourceHint, propertyType.getId()),
            namespace);
        propertyType.getLinks().add(self);

        Link parent =
            withNamespace(createLink(Relations.PARENT, LiveDataPropertyTypesResource.class, sourceHint), namespace);
        propertyType.getLinks().add(parent);

        return propertyType;
    }

    protected Entry createEntry(Map<String, Object> values, String sourceHint, String namespace)
    {
        Link self = withNamespace(createLink(Relations.SELF, LiveDataEntryResource.class, sourceHint, values.get("id")),
            namespace);

        Link parent = withNamespace(createLink(Relations.PARENT, LiveDataEntriesResource.class, sourceHint), namespace);

        return (Entry) new Entry().withValues(StringMap.fromMap(values)).withLinks(self, parent);
    }

    protected LiveDataPropertyDescriptor convert(PropertyDescriptor descriptor)
    {
        LiveDataPropertyDescriptor propertyDescriptor = new LiveDataPropertyDescriptor();
        propertyDescriptor.setId(descriptor.getId());
        propertyDescriptor.setName(descriptor.getName());
        propertyDescriptor.setDescription(descriptor.getDescription());
        propertyDescriptor.getIcon().putAll(descriptor.getIcon());
        propertyDescriptor.setType(descriptor.getType());
        propertyDescriptor.setSortable(descriptor.isSortable());
        propertyDescriptor.getDisplayer().putAll(descriptor.getDisplayer());
        propertyDescriptor.getFilter().putAll(descriptor.getFilter());
        propertyDescriptor.setStyleName(descriptor.getStyleName());
        return propertyDescriptor;
    }
}
