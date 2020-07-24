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
package org.xwiki.livedata.macro;

import org.xwiki.livedata.internal.macro.LiveDataMacro;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.stability.Unstable;

/**
 * Parameters for {@link LiveDataMacro}.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@Unstable
public class LiveDataMacroParameters
{
    private String id;

    private String properties;

    private String source;

    private String sourceParameters;

    private String sort;

    private String hiddenFilters;

    private String filters;

    private int limit = 15;

    private long offset;

    /**
     * @return the live data instance id
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the live data instance id.
     * 
     * @param id the live data instance id
     */
    @PropertyDescription("The live data instance id.")
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the comma-separated list of properties to fetch and display
     */
    public String getProperties()
    {
        return properties;
    }

    /**
     * Sets the comma-separated list of properties to fetch and display.
     * 
     * @param properties the comma-separated list of properties to fetch and display
     */
    @PropertyDescription("The comma-separated list of properties to fetch and display.")
    public void setProperties(String properties)
    {
        this.properties = properties;
    }

    /**
     * @return the live data source component implementation hint
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Sets the live data source component implementation hint.
     * 
     * @param source the live data source component implementation hint
     */
    @PropertyDescription("The live data source to use, specified as a component hint.")
    public void setSource(String source)
    {
        this.source = source;
    }

    /**
     * @return the live data source parameters, as an URL query string
     */
    public String getSourceParameters()
    {
        return sourceParameters;
    }

    /**
     * Sets the live data source parameters.
     * 
     * @param sourceParameters the live data source parameters, specified as an URL query string
     */
    @PropertyDescription("The live data source parameters, specified as an URL query string.")
    public void setSourceParameters(String sourceParameters)
    {
        this.sourceParameters = sourceParameters;
    }

    /**
     * @return the properties to sort on the live data initially
     */
    public String getSort()
    {
        return sort;
    }

    /**
     * Sets the properties to sort on the live data initially. The value is a comma-separated list of property names,
     * where each property name can optionally be suffixed with the sort order using {@code :asc} or {@code :desc}.
     * 
     * @param sort the properties to sort on the live data initially
     */
    @PropertyDescription("The properties to sort on the live data initially, specified as a comma-separated list of "
        + "property names, where each property name can be optionally suffixed with the sort order using :asc or "
        + ":desc.")
    public void setSort(String sort)
    {
        this.sort = sort;
    }

    /**
     * @return the hidden filters, that the user cannot change from the user interface
     */
    public String getHiddenFilters()
    {
        return hiddenFilters;
    }

    /**
     * Sets the hidden filters, that the user cannot change from the user interface, specified as an URL query string.
     * 
     * @param hiddenFilters the hidden filters, specified as an URL query string
     */
    @PropertyDescription("The hidden filters, that the user cannot change from the live data widget, specified as an "
        + "URL query string.")
    public void setHiddenFilters(String hiddenFilters)
    {
        this.hiddenFilters = hiddenFilters;
    }

    /**
     * @return the initial filters to apply on the live data
     */
    public String getFilters()
    {
        return filters;
    }

    /**
     * Sets the initial filters to apply on the live data, specified as an URL query string.
     * 
     * @param filters the initial filters to apply on the live data
     */
    @PropertyDescription("The initial filters to apply on the live data, specified as an URL query string.")
    public void setFilters(String filters)
    {
        this.filters = filters;
    }

    /**
     * @return the maximum number of live data entries to show on a page
     */
    public int getLimit()
    {
        return limit;
    }

    /**
     * Sets the maximum number of live data entries to show on a page.
     * 
     * @param limit the maximum number of live data entries to show on a page
     */
    @PropertyDescription("The maximum number of live data entries to show on a page.")
    public void setLimit(int limit)
    {
        this.limit = limit;
    }

    /**
     * @return the index of the first live data entry to show
     */
    public long getOffset()
    {
        return offset;
    }

    /**
     * Sets the index of the first live data entry to show.
     * 
     * @param offset the index of the first live data entry to show
     */
    @PropertyDescription("The index of the first live data entry to show.")
    public void setOffset(long offset)
    {
        this.offset = offset;
    }
}
