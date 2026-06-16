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
package org.xwiki.livedata.internal.solr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.WithParameters;

/**
 * {@link LiveDataPropertyDescriptorStore} implementation that exposes the document properties supported by the Solr
 * live data source as columns.
 * <p>
 * Columns are referenced through {@code doc.*} property identifiers (the same ones used by the other live data sources
 * and the default configuration); the {@link SolrLiveDataEntryStore} maps them to the corresponding Solr fields. The
 * caller selects which of these columns to display through the standard live data {@code properties} mechanism. Object
 * (class) properties are not supported yet.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@Component
@Named(SolrLiveDataPropertyStore.ROLE_HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SolrLiveDataPropertyStore extends WithParameters implements LiveDataPropertyDescriptorStore
{
    /**
     * The hint of this component implementation.
     */
    public static final String ROLE_HINT = "solr";

    private static final String STRING_TYPE = "String";

    private static final String DATE_TYPE = "Date";

    private static final String BOOLEAN_TYPE = "Boolean";

    private static final String CONTAINS_OPERATOR = "contains";

    private static final String TEXT_FILTER = "text";

    private static final Set<String> DATE_PROPERTIES = Set.of("doc.creationDate", "doc.date");

    private static final String HIDDEN_PROPERTY = "doc.hidden";

    @Override
    public Collection<LiveDataPropertyDescriptor> get() throws LiveDataException
    {
        List<LiveDataPropertyDescriptor> properties = new ArrayList<>();
        for (String property : SolrLiveDataEntryStore.SOLR_FIELDS.keySet()) {
            properties.add(getDescriptor(property));
        }
        return properties;
    }

    private LiveDataPropertyDescriptor getDescriptor(String property)
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setId(property);
        descriptor.setName(property);
        descriptor.setType(getType(property));
        // A property is sortable when the entry store knows a Solr field to sort it on.
        descriptor.setSortable(SolrLiveDataEntryStore.SOLR_SORT_FIELDS.containsKey(property));
        descriptor.setFilterable(true);
        if (SolrLiveDataEntryStore.TITLE_PROPERTY.equals(property)) {
            // Render the title as a link pointing to the document, the URL being provided by the entry's "url"
            // property.
            DisplayerDescriptor displayer = new DisplayerDescriptor("link");
            displayer.setParameter("propertyHref", SolrLiveDataEntryStore.URL_PROPERTY);
            descriptor.setDisplayer(displayer);
        }
        FilterDescriptor filter = new FilterDescriptor(TEXT_FILTER);
        filter.setDefaultOperator(CONTAINS_OPERATOR);
        descriptor.setFilter(filter);
        return descriptor;
    }

    private String getType(String property)
    {
        if (DATE_PROPERTIES.contains(property)) {
            return DATE_TYPE;
        } else if (HIDDEN_PROPERTY.equals(property)) {
            return BOOLEAN_TYPE;
        }
        return STRING_TYPE;
    }
}
