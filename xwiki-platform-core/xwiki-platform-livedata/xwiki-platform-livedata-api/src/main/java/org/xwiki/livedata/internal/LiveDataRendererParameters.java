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
package org.xwiki.livedata.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Parameters for {@link LiveDataRenderer}.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
public class LiveDataRendererParameters
{
    private String id;

    private String properties;

    private String source;

    private String sourceParameters;

    private String sort;

    private String filters;

    private Integer limit;

    private Long offset;

    private String layouts;

    private Boolean showPageSizeDropdown;

    private String pageSizes;

    private String description;

    /**
     * @return the Live Data instance id
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * Sets the live data instance id.
     *
     * @param id the Live Data instance id
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the comma-separated list of properties to fetch and display
     */
    public String getProperties()
    {
        return this.properties;
    }

    /**
     * Sets the comma-separated list of properties to fetch and display.
     *
     * @param properties the comma-separated list of properties to fetch and display
     */
    public void setProperties(String properties)
    {
        this.properties = properties;
    }

    /**
     * @return the Live Data source component implementation hint
     */
    public String getSource()
    {
        return this.source;
    }

    /**
     * Sets the Live Data source component implementation hint.
     *
     * @param source the Live Data source component implementation hint
     */
    public void setSource(String source)
    {
        this.source = source;
    }

    /**
     * @return the Live Data source parameters, as an URL query string
     */
    public String getSourceParameters()
    {
        return this.sourceParameters;
    }

    /**
     * Sets the Live Data source parameters.
     *
     * @param sourceParameters the Live Data source parameters, specified as an URL query string
     */
    public void setSourceParameters(String sourceParameters)
    {
        this.sourceParameters = sourceParameters;
    }

    /**
     * @return the properties to sort on the Live Data initially
     */
    public String getSort()
    {
        return this.sort;
    }

    /**
     * Sets the properties to sort on the Live Data initially. The value is a comma-separated list of property names,
     * where each property name can optionally be suffixed with the sort order using {@code :asc} or {@code :desc}.
     *
     * @param sort the properties to sort on the Live Data initially
     */
    public void setSort(String sort)
    {
        this.sort = sort;
    }

    /**
     * @return the initial filters to apply on the Live Data
     */
    public String getFilters()
    {
        return this.filters;
    }

    /**
     * Sets the initial filters to apply on the Live Data, specified as an URL query string.
     *
     * @param filters the initial filters to apply on the Live Data
     */
    public void setFilters(String filters)
    {
        this.filters = filters;
    }

    /**
     * @return the maximum number of Live Data entries to show on a page
     */
    public Integer getLimit()
    {
        return this.limit;
    }

    /**
     * Sets the maximum number of Live Data entries to show on a page.
     *
     * @param limit the maximum number of Live Data entries to show on a page
     */
    public void setLimit(Integer limit)
    {
        this.limit = limit;
    }

    /**
     * @return the index of the first Live Data entry to show
     */
    public Long getOffset()
    {
        return this.offset;
    }

    /**
     * Sets the index of the first Live Data entry to show.
     *
     * @param offset the index of the first Live Data entry to show
     */
    public void setOffset(Long offset)
    {
        this.offset = offset;
    }

    /**
     * @return a comma-separated list of layout identifiers, indicating the layouts the user can choose from to display
     *     the Live Data
     */
    public String getLayouts()
    {
        return this.layouts;
    }

    /**
     * Sets the layouts that the user can choose from to display the Live Data. The first layout in the list will be
     * loaded initially.
     *
     * @param layouts a comma-separated list of layout identifiers
     */
    public void setLayouts(String layouts)
    {
        this.layouts = layouts;
    }

    /**
     * @return whether to show or not the page size drop down that allows the user to change the number of entries
     *     displayed per page
     */
    public Boolean getShowPageSizeDropdown()
    {
        return this.showPageSizeDropdown;
    }

    /**
     * Sets whether to show or not the page size drop down that allows the user to change the number of entries
     * displayed per page.
     *
     * @param showPageSizeDropdown {@code true} to show the page size drop down, {@code false} otherwise; leave
     *     {@code null} to inherit from the default configuration
     */
    public void setShowPageSizeDropdown(Boolean showPageSizeDropdown)
    {
        this.showPageSizeDropdown = showPageSizeDropdown;
    }

    /**
     * @return the values to display in the page size drop down
     */
    public String getPageSizes()
    {
        return this.pageSizes;
    }

    /**
     * Sets the values to display in the page size drop down.
     *
     * @param pageSizes a comma-separated list of page sizes to display in the page size drop down
     */
    public void setPageSizes(String pageSizes)
    {
        this.pageSizes = pageSizes;
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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LiveDataRendererParameters that = (LiveDataRendererParameters) o;

        return new EqualsBuilder()
            .append(this.id, that.id)
            .append(this.properties, that.properties)
            .append(this.source, that.source)
            .append(this.sourceParameters, that.sourceParameters)
            .append(this.sort, that.sort)
            .append(this.filters, that.filters)
            .append(this.limit, that.limit)
            .append(this.offset, that.offset)
            .append(this.layouts, that.layouts)
            .append(this.showPageSizeDropdown, that.showPageSizeDropdown)
            .append(this.pageSizes, that.pageSizes)
            .append(this.description, that.description)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.id)
            .append(this.properties)
            .append(this.source)
            .append(this.sourceParameters)
            .append(this.sort)
            .append(this.filters)
            .append(this.limit)
            .append(this.offset)
            .append(this.layouts)
            .append(this.showPageSizeDropdown)
            .append(this.pageSizes)
            .append(this.description)
            .toHashCode();
    }
}
