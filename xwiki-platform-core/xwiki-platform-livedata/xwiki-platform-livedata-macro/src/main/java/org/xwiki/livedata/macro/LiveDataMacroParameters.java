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

import org.xwiki.livedata.internal.LiveDataRendererParameters;
import org.xwiki.livedata.internal.macro.LiveDataMacro;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.stability.Unstable;

/**
 * Parameters for {@link LiveDataMacro}.
 * 
 * @version $Id$
 * @since 12.10
 */
public class LiveDataMacroParameters extends LiveDataRendererParameters
{
    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The live data instance id.")
    public void setId(String id)
    {
        super.setId(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The comma-separated list of properties to fetch and display.")
    public void setProperties(String properties)
    {
        super.setProperties(properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The live data source to use, specified as a component hint.")
    public void setSource(String source)
    {
        super.setSource(source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The live data source parameters, specified as an URL query string.")
    public void setSourceParameters(String sourceParameters)
    {
        super.setSourceParameters(sourceParameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The properties to sort on the live data initially, specified as a comma-separated list of "
        + "property names, where each property name can be optionally suffixed with the sort order using :asc or "
        + ":desc.")
    public void setSort(String sort)
    {
        super.setSort(sort);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The initial filters to apply on the live data, specified as an URL query string.")
    public void setFilters(String filters)
    {
        super.setFilters(filters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The maximum number of live data entries to show on a page.")
    public void setLimit(Integer limit)
    {
        super.setLimit(limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The index of the first live data entry to show.")
    public void setOffset(Long offset)
    {
        super.setOffset(offset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The comma-separated list of layouts the user can choose from to display the live data. "
        + "The first layout in the list will be loaded initially.")
    public void setLayouts(String layouts)
    {
        super.setLayouts(layouts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("Show or hide the page size drop down that allows the user to change "
        + "the number of entries displayed per page.")
    public void setShowPageSizeDropdown(Boolean showPageSizeDropdown)
    {
        super.setShowPageSizeDropdown(showPageSizeDropdown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PropertyDescription("The comma-separated list of page sizes to display in the page size drop down.")
    public void setPageSizes(String pageSizes)
    {
        super.setPageSizes(pageSizes);
    }

    /**
     * {@inheritDoc}
     *
     * @since 16.0.0RC1
     */
    @Override
    @Unstable
    @PropertyDescription("An optional textual description of the Live Data.")
    public void setDescription(String description)
    {
        super.setDescription(description);
    }
}
