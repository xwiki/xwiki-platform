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
package org.xwiki.livedata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Describes the configuration used to display the live data.
 * 
 * @version $Id$
 * @since 12.10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveDataMeta implements InitializableLiveDataElement
{
    private Collection<LiveDataLayoutDescriptor> layouts;

    private String defaultLayout;

    private Collection<LiveDataPropertyDescriptor> propertyDescriptors;

    private Collection<LiveDataPropertyDescriptor> propertyTypes;

    private Collection<FilterDescriptor> filters;

    private String defaultFilter;

    private Collection<DisplayerDescriptor> displayers;

    private String defaultDisplayer;

    private LiveDataPaginationConfiguration pagination;

    private LiveDataEntryDescriptor entryDescriptor;

    private Collection<LiveDataActionDescriptor> actions;

    private LiveDataSelectionConfiguration selection;

    private String description;

    /**
     * @return the default layout used to display the live data
     */
    public String getDefaultLayout()
    {
        return defaultLayout;
    }

    /**
     * Set the default layout used to display the live data.
     * 
     * @param defaultLayout the new default layout
     */
    public void setDefaultLayout(String defaultLayout)
    {
        this.defaultLayout = defaultLayout;
    }

    /**
     * @return the default filter widget used to filter the live data properties
     */
    public String getDefaultFilter()
    {
        return defaultFilter;
    }

    /**
     * Set the default filter widget to use when filtering live data properties.
     * 
     * @param defaultFilter the identifier of the new default filter
     */
    public void setDefaultFilter(String defaultFilter)
    {
        this.defaultFilter = defaultFilter;
    }

    /**
     * @return the default displayer used to display live data properties
     */
    public String getDefaultDisplayer()
    {
        return defaultDisplayer;
    }

    /**
     * Set the default displayer used to display live data properties.
     * 
     * @param defaultDisplayer the identifier of the new default displayer
     */
    public void setDefaultDisplayer(String defaultDisplayer)
    {
        this.defaultDisplayer = defaultDisplayer;
    }

    /**
     * @return the list of supported layouts
     */
    public Collection<LiveDataLayoutDescriptor> getLayouts()
    {
        return layouts;
    }

    /**
     * Sets the list of supported layouts.
     * 
     * @param layouts the new list of supported layouts
     */
    public void setLayouts(Collection<LiveDataLayoutDescriptor> layouts)
    {
        this.layouts = layouts;
    }

    /**
     * @return the list of known properties
     */
    public Collection<LiveDataPropertyDescriptor> getPropertyDescriptors()
    {
        return propertyDescriptors;
    }

    /**
     * Sets the list of known properties.
     * 
     * @param propertyDescriptors the new list of known properties
     */
    public void setPropertyDescriptors(Collection<LiveDataPropertyDescriptor> propertyDescriptors)
    {
        this.propertyDescriptors = propertyDescriptors;
    }

    /**
     * @return the list of known property types
     */
    public Collection<LiveDataPropertyDescriptor> getPropertyTypes()
    {
        return propertyTypes;
    }

    /**
     * Sets the list of known property types.
     * 
     * @param propertyTypes the new list of known property types
     */
    public void setPropertyTypes(Collection<LiveDataPropertyDescriptor> propertyTypes)
    {
        this.propertyTypes = propertyTypes;
    }

    /**
     * @return the list of known filter widgets
     */
    public Collection<FilterDescriptor> getFilters()
    {
        return filters;
    }

    /**
     * Sets the list of known filter widgets.
     * 
     * @param filters the new list of known filter widgets
     */
    public void setFilters(Collection<FilterDescriptor> filters)
    {
        this.filters = filters;
    }

    /**
     * @return the list of known property displayers
     */
    public Collection<DisplayerDescriptor> getDisplayers()
    {
        return displayers;
    }

    /**
     * Sets the list of known property displayers.
     * 
     * @param displayers the new list of known property displayers
     */
    public void setDisplayers(Collection<DisplayerDescriptor> displayers)
    {
        this.displayers = displayers;
    }

    /**
     * @return the pagination configuration
     */
    public LiveDataPaginationConfiguration getPagination()
    {
        return pagination;
    }

    /**
     * Sets the pagination configuration.
     * 
     * @param pagination the new pagination configuration
     */
    public void setPagination(LiveDataPaginationConfiguration pagination)
    {
        this.pagination = pagination;
    }

    /**
     * @return the descriptor of the live data entries
     */
    public LiveDataEntryDescriptor getEntryDescriptor()
    {
        return entryDescriptor;
    }

    /**
     * Sets the descriptor of the live data entries.
     * 
     * @param entryDescriptor the new entry descriptor
     */
    public void setEntryDescriptor(LiveDataEntryDescriptor entryDescriptor)
    {
        this.entryDescriptor = entryDescriptor;
    }

    /**
     * @return the descriptors of supported live data actions
     * @since 12.10.1
     * @since 13.0
     */
    public Collection<LiveDataActionDescriptor> getActions()
    {
        return actions;
    }

    /**
     * Sets the supported actions.
     * 
     * @param actions the descriptors for the supported actions
     * @since 12.10.1
     * @since 13.0
     */
    public void setActions(Collection<LiveDataActionDescriptor> actions)
    {
        this.actions = actions;
    }

    /**
     * @return the live data entry selection
     * @since 12.10.1
     * @since 13.0
     */
    public LiveDataSelectionConfiguration getSelection()
    {
        return selection;
    }

    /**
     * Sets the live data entry selection.
     * 
     * @param selection the new live data entry selection
     * @since 12.10.1
     * @since 13.0
     */
    public void setSelection(LiveDataSelectionConfiguration selection)
    {
        this.selection = selection;
    }

    /**
     * @return an optional textual description of the Live Data
     * @since 16.0.0RC1
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description an optional textual description of the Live Data
     * @since 16.0.0RC1
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    private <A extends InitializableLiveDataElement> Collection<A> initializeAndCleanUpCollection(Collection<A>
        collection)
    {
        Collection<A> result;
        if (collection == null) {
            result = new ArrayList<>();
        } else {
            result = collection;
            result.stream().filter(Objects::nonNull).forEach(A::initialize);
        }
        return result;
    }

    private <A extends InitializableLiveDataElement> A initialize(A descriptor, A newInstance)
    {
        A result = descriptor;
        if (result == null) {
            result = newInstance;
        }
        result.initialize();
        return result;
    }

    @Override
    public void initialize()
    {
        this.layouts = initializeAndCleanUpCollection(this.layouts);
        this.propertyDescriptors = initializeAndCleanUpCollection(this.propertyDescriptors);
        this.propertyTypes = initializeAndCleanUpCollection(this.propertyTypes);
        this.filters = initializeAndCleanUpCollection(this.filters);
        this.actions = initializeAndCleanUpCollection(this.actions);

        if (this.displayers == null) {
            this.displayers = new ArrayList<>();
        }

        this.pagination = initialize(this.pagination, new LiveDataPaginationConfiguration());
        this.entryDescriptor = initialize(this.entryDescriptor, new LiveDataEntryDescriptor());
        this.selection = initialize(this.selection, new LiveDataSelectionConfiguration());
    }

    /**
     * @since 17.4.0RC1
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LiveDataMeta that = (LiveDataMeta) o;

        return new EqualsBuilder()
            .append(this.layouts, that.layouts)
            .append(this.defaultLayout, that.defaultLayout)
            .append(this.propertyDescriptors, that.propertyDescriptors)
            .append(this.propertyTypes, that.propertyTypes)
            .append(this.filters, that.filters)
            .append(this.defaultFilter, that.defaultFilter)
            .append(this.displayers, that.displayers)
            .append(this.defaultDisplayer, that.defaultDisplayer)
            .append(this.pagination, that.pagination)
            .append(this.entryDescriptor, that.entryDescriptor)
            .append(this.actions, that.actions)
            .append(this.selection, that.selection)
            .append(this.description, that.description)
            .isEquals();
    }

    /**
     * @since 17.4.0RC1
     */
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.layouts)
            .append(this.defaultLayout)
            .append(this.propertyDescriptors)
            .append(this.propertyTypes)
            .append(this.filters)
            .append(this.defaultFilter)
            .append(this.displayers)
            .append(this.defaultDisplayer)
            .append(this.pagination)
            .append(this.entryDescriptor)
            .append(this.actions)
            .append(this.selection)
            .append(this.description)
            .toHashCode();
    }
}
