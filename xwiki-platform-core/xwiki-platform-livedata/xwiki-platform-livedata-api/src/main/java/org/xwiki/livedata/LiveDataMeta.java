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

import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.stability.Unstable;

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
public class LiveDataMeta
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
    @Unstable
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description an optional textual description of the Live Data
     * @since 16.0.0RC1
     */
    @Unstable
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Prevent {@code null} values where it's possible.
     */
    public void initialize()
    {
        if (this.layouts == null) {
            this.layouts = new ArrayList<>();
        }
        this.layouts.stream().filter(Objects::nonNull).forEach(LiveDataLayoutDescriptor::initialize);

        if (this.propertyDescriptors == null) {
            this.propertyDescriptors = new ArrayList<>();
        }
        this.propertyDescriptors.stream().filter(Objects::nonNull).forEach(LiveDataPropertyDescriptor::initialize);

        if (this.propertyTypes == null) {
            this.propertyTypes = new ArrayList<>();
        }
        this.propertyTypes.stream().filter(Objects::nonNull).forEach(LiveDataPropertyDescriptor::initialize);

        if (this.filters == null) {
            this.filters = new ArrayList<>();
        }
        this.filters.stream().filter(Objects::nonNull).forEach(FilterDescriptor::initialize);

        if (this.displayers == null) {
            this.displayers = new ArrayList<>();
        }

        if (this.pagination == null) {
            this.pagination = new LiveDataPaginationConfiguration();
        }
        this.pagination.initialize();

        if (this.entryDescriptor == null) {
            this.entryDescriptor = new LiveDataEntryDescriptor();
        }
        this.entryDescriptor.initialize();

        if (this.actions == null) {
            this.actions = new ArrayList<>();
        }
        this.actions.stream().filter(Objects::nonNull).forEach(LiveDataActionDescriptor::initialize);

        if (this.selection == null) {
            this.selection = new LiveDataSelectionConfiguration();
        }
        this.selection.initialize();
    }
}
