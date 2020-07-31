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
import java.util.List;
import java.util.Map;

import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Describes the live data configuration.
 * 
 * @version $Id$
 * @since 12.6
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Unstable
public class LiveDataConfiguration extends WithParameters
{
    /**
     * Describes the configuration used to display the live data.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LiveDataMeta extends WithParameters
    {
        private Collection<LayoutDescriptor> layouts;

        private String defaultLayout;

        private Collection<LiveDataPropertyDescriptor> propertyDescriptors;

        private Collection<LiveDataPropertyDescriptor> propertyTypes;

        private Collection<FilterDescriptor> filters;

        private String defaultFilter;

        private Collection<DisplayerDescriptor> displayers;

        private String defaultDisplayer;

        private PaginationConfiguration pagination;

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
        public Collection<LayoutDescriptor> getLayouts()
        {
            return layouts;
        }

        /**
         * Sets the list of supported layouts.
         * 
         * @param layouts the new list of supported layouts
         */
        public void setLayouts(Collection<LayoutDescriptor> layouts)
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
        public PaginationConfiguration getPagination()
        {
            return pagination;
        }

        /**
         * Sets the pagination configuration.
         * 
         * @param pagination the new pagination configuration
         */
        public void setPagination(PaginationConfiguration pagination)
        {
            this.pagination = pagination;
        }

        /**
         * Prevent {@code null} values where it's possible.
         */
        public void initialize()
        {
            if (this.layouts == null) {
                this.layouts = new ArrayList<>();
            }
            if (this.propertyDescriptors == null) {
                this.propertyDescriptors = new ArrayList<>();
            }
            this.propertyDescriptors.forEach(LiveDataPropertyDescriptor::initialize);
            if (this.propertyTypes == null) {
                this.propertyTypes = new ArrayList<>();
            }
            this.propertyTypes.forEach(LiveDataPropertyDescriptor::initialize);
            if (this.filters == null) {
                this.filters = new ArrayList<>();
            }
            this.filters.forEach(FilterDescriptor::initialize);
            if (this.displayers == null) {
                this.displayers = new ArrayList<>();
            }
            if (this.pagination == null) {
                this.pagination = new PaginationConfiguration();
            }
            this.pagination.initialize();
        }
    }

    /**
     * Holds the layout configuration.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LayoutDescriptor extends BaseDescriptor
    {
        private String name;

        private Map<String, Object> icon;

        /**
         * Default constructor.
         */
        public LayoutDescriptor()
        {
        }

        /**
         * Creates a descriptor for the layout with the given id.
         * 
         * @param id the layout id
         */
        public LayoutDescriptor(String id)
        {
            setId(id);
        }

        /**
         * @return the layout pretty name
         */
        public String getName()
        {
            return name;
        }

        /**
         * Set the layout pretty name.
         * 
         * @param name the new layout name
         */
        public void setName(String name)
        {
            this.name = name;
        }

        /**
         * @return the icon meta data
         */
        public Map<String, Object> getIcon()
        {
            return icon;
        }

        /**
         * Set the icon meta data.
         * 
         * @param icon the icon meta data
         */
        public void setIcon(Map<String, Object> icon)
        {
            this.icon = icon;
        }
    }

    /**
     * Describes the live data pagination configuration.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationConfiguration
    {
        private Integer maxShownPages;

        private List<Integer> pageSizes;

        private Boolean showNextPrevious;

        private Boolean showFirstLast;

        private Boolean showPageSizeDropdown;

        /**
         * @return the maximum number of page links to display in the pagination
         */
        public Integer getMaxShownPages()
        {
            return maxShownPages;
        }

        /**
         * Sets the maximum number of page links to display in the pagination.
         * 
         * @param maxShownPages the maximum number of page links to display in the pagination
         */
        public void setMaxShownPages(Integer maxShownPages)
        {
            this.maxShownPages = maxShownPages;
        }

        /**
         * @return the values to display in the page size drop down
         */
        public List<Integer> getPageSizes()
        {
            return pageSizes;
        }

        /**
         * Sets the values to display in the page size drop down that allows the user to change the page size.
         * 
         * @param pageSizes the values to display on the page size drop down
         */
        public void setPageSizes(List<Integer> pageSizes)
        {
            this.pageSizes = pageSizes;
        }

        /**
         * @return whether to show or not the next / previous pagination links
         */
        public Boolean getShowNextPrevious()
        {
            return showNextPrevious;
        }

        /**
         * Sets whether to show or not the next / previous pagination links.
         * 
         * @param showNextPrevious {@code true} to show the next / previous pagination links, {@code false} otherwise
         */
        public void setShowNextPrevious(Boolean showNextPrevious)
        {
            this.showNextPrevious = showNextPrevious;
        }

        /**
         * @return whether to show or not the first / last pagination links
         */
        public Boolean getShowFirstLast()
        {
            return showFirstLast;
        }

        /**
         * Sets whether to show or not the first / last pagination links.
         * 
         * @param showFirstLast {@code true} to show the first / last pagination links, {@code false} otherwise
         */
        public void setShowFirstLast(Boolean showFirstLast)
        {
            this.showFirstLast = showFirstLast;
        }

        /**
         * @return whether to show or not the page size drop down that the user can use to change the number of entries
         *         displayed per page
         */
        public Boolean getShowPageSizeDropdown()
        {
            return showPageSizeDropdown;
        }

        /**
         * Sets whether to show or not the page size drop down that the user can use to change the number of entries
         * displayed per page.
         * 
         * @param showPageSizeDropdown {@code true} to show the page size drop down, {@code false} otherwise
         */
        public void setShowPageSizeDropdown(Boolean showPageSizeDropdown)
        {
            this.showPageSizeDropdown = showPageSizeDropdown;
        }

        /**
         * Prevent {@code null} values where it's possible.
         */
        public void initialize()
        {
            if (this.pageSizes == null) {
                this.pageSizes = new ArrayList<>();
            }
        }
    }

    private LiveDataQuery query;

    private LiveData data;

    private LiveDataMeta meta;

    /**
     * @return the query used to retrieve the live data
     */
    public LiveDataQuery getQuery()
    {
        return query;
    }

    /**
     * Set the query used to retrieve the live data.
     * 
     * @param query the new query
     */
    public void setQuery(LiveDataQuery query)
    {
        this.query = query;
    }

    /**
     * @return the live data
     */
    public LiveData getData()
    {
        return data;
    }

    /**
     * Sets the live data.
     * 
     * @param data the new live data
     */
    public void setData(LiveData data)
    {
        this.data = data;
    }

    /**
     * @return the configuration used to display the live data
     */
    public LiveDataMeta getMeta()
    {
        return meta;
    }

    /**
     * Set the configuration used to display the live data.
     * 
     * @param meta the new meta configuration
     */
    public void setMeta(LiveDataMeta meta)
    {
        this.meta = meta;
    }

    /**
     * Prevent {@code null} values where it's possible.
     */
    public void initialize()
    {
        if (this.query == null) {
            this.query = new LiveDataQuery();
        }
        this.query.initialize();

        if (this.data == null) {
            this.data = new LiveData();
        }

        if (this.meta == null) {
            this.meta = new LiveDataMeta();
        }
        this.meta.initialize();
    }
}
