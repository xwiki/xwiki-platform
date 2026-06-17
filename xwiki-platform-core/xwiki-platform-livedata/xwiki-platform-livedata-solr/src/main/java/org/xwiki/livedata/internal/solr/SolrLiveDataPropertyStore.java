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
import java.util.Map;

import jakarta.inject.Named;

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
 * @since 18.6.0RC1
 */
@Component
@Named(SolrLiveDataPropertyStore.ROLE_HINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SolrLiveDataPropertyStore extends WithParameters implements LiveDataPropertyDescriptorStore
{
    /**
     * The hint of this component implementation.
     */
    public static final String ROLE_HINT = SolrLiveDataEntryStore.ROLE_HINT;

    private static final String STRING_TYPE = "String";

    private static final String DATE_TYPE = "Date";

    private static final String BOOLEAN_TYPE = "Boolean";

    private static final String CONTAINS_OPERATOR = "contains";

    private static final String EQUALS_OPERATOR = "equals";

    private static final String STARTS_WITH_OPERATOR = "startsWith";

    private static final String BEFORE_OPERATOR = "before";

    private static final String AFTER_OPERATOR = "after";

    private static final String BETWEEN_OPERATOR = "between";

    private static final String TEXT_FILTER = "text";

    private static final String DATE_FILTER = "date";

    /**
     * The date format passed to the date filter widget, matching the one of the base {@code date} filter in
     * {@code liveDataConfiguration.json}.
     */
    private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm";

    private static final String BOOLEAN_FILTER = "boolean";

    private static final String LINK_DISPLAYER = "link";

    private static final String HTML_DISPLAYER = "html";

    // The "text" displayer id happens to be the same literal as the "text" filter id; reuse the constant to avoid a
    // duplicated string literal.
    private static final String TEXT_DISPLAYER = TEXT_FILTER;

    private static final String PROPERTY_HREF = "propertyHref";

    /**
     * Maps the {@code doc.*} columns rendered as a link to the entry property holding their target URL.
     */
    private static final Map<String, String> LINK_HREF_PROPERTIES = Map.of(
        SolrLiveDataEntryStore.TITLE_PROPERTY, SolrLiveDataEntryStore.URL_PROPERTY,
        SolrLiveDataEntryStore.FULLNAME_PROPERTY, SolrLiveDataEntryStore.URL_PROPERTY,
        SolrLiveDataEntryStore.AUTHOR_PROPERTY, SolrLiveDataEntryStore.AUTHOR_URL_PROPERTY,
        SolrLiveDataEntryStore.CREATOR_PROPERTY, SolrLiveDataEntryStore.CREATOR_URL_PROPERTY);

    @Override
    public Collection<LiveDataPropertyDescriptor> get() throws LiveDataException
    {
        List<LiveDataPropertyDescriptor> properties = new ArrayList<>();
        for (String property : SolrLiveDataEntryStore.SOLR_FIELDS.keySet()) {
            properties.add(getDescriptor(property));
        }
        // The location is a derived column (not backed by a Solr field): it is built from the document reference and
        // rendered as an HTML breadcrumb of links.
        properties.add(getLocationDescriptor());
        return properties;
    }

    private LiveDataPropertyDescriptor getDescriptor(String property)
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setId(property);
        // The column header name is left unset on purpose: it is localized by the SolrLiveDataConfigurationResolver
        // from the caller-provided "translationPrefix" source parameter (the live table source contract).
        descriptor.setType(getType(property));
        // A property is sortable when the entry store knows a Solr field to sort it on.
        descriptor.setSortable(SolrLiveDataEntryStore.SOLR_SORT_FIELDS.containsKey(property));
        descriptor.setFilterable(true);
        descriptor.setDisplayer(getDisplayer(property));
        descriptor.setFilter(getFilter(property));
        return descriptor;
    }

    private LiveDataPropertyDescriptor getLocationDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setId(SolrLiveDataEntryStore.LOCATION_PROPERTY);
        descriptor.setType(STRING_TYPE);
        // There is no Solr field backing the location, so it can be neither sorted nor filtered on.
        descriptor.setSortable(false);
        descriptor.setFilterable(false);
        descriptor.setDisplayer(new DisplayerDescriptor(HTML_DISPLAYER));
        return descriptor;
    }

    private DisplayerDescriptor getDisplayer(String property)
    {
        String hrefProperty = LINK_HREF_PROPERTIES.get(property);
        if (hrefProperty != null) {
            // Render the value as a link, the target URL being provided by a dedicated entry property.
            DisplayerDescriptor displayer = new DisplayerDescriptor(LINK_DISPLAYER);
            displayer.setParameter(PROPERTY_HREF, hrefProperty);
            return displayer;
        } else if (SolrLiveDataEntryStore.DATE_PROPERTIES.contains(property)) {
            // The date value is already formatted server-side, so it is displayed as plain text (not re-parsed by the
            // client-side "date" displayer).
            return new DisplayerDescriptor(TEXT_DISPLAYER);
        }
        return null;
    }

    private FilterDescriptor getFilter(String property)
    {
        FilterDescriptor filter;
        if (SolrLiveDataEntryStore.DATE_PROPERTIES.contains(property)) {
            // Date columns are filtered through Solr range queries (see SolrLiveDataEntryStore#toDateClause).
            filter = new FilterDescriptor(DATE_FILTER);
            filter.addOperator(BETWEEN_OPERATOR, null);
            filter.addOperator(BEFORE_OPERATOR, null);
            filter.addOperator(AFTER_OPERATOR, null);
            filter.setDefaultOperator(BETWEEN_OPERATOR);
            // The date filter widget needs the date format to (de)serialize the picked dates; we build a full filter
            // descriptor (custom operators) which would otherwise shadow the base "date" filter and lose its format.
            filter.setParameter("dateFormat", DATE_FORMAT);
        } else if (SolrLiveDataEntryStore.HIDDEN_PROPERTY.equals(property)) {
            filter = new FilterDescriptor(BOOLEAN_FILTER);
            filter.addOperator(EQUALS_OPERATOR, null);
            filter.setDefaultOperator(EQUALS_OPERATOR);
        } else {
            filter = new FilterDescriptor(TEXT_FILTER);
            filter.addOperator(CONTAINS_OPERATOR, null);
            filter.addOperator(EQUALS_OPERATOR, null);
            filter.addOperator(STARTS_WITH_OPERATOR, null);
            filter.setDefaultOperator(CONTAINS_OPERATOR);
        }
        return filter;
    }

    private String getType(String property)
    {
        if (SolrLiveDataEntryStore.DATE_PROPERTIES.contains(property)) {
            return DATE_TYPE;
        } else if (SolrLiveDataEntryStore.HIDDEN_PROPERTY.equals(property)) {
            return BOOLEAN_TYPE;
        }
        return STRING_TYPE;
    }
}
