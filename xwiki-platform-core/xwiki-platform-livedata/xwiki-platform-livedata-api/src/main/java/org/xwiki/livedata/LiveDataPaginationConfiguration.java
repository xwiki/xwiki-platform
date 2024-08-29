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

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Describes the live data pagination configuration.
 * 
 * @version $Id$
 * @since 12.10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveDataPaginationConfiguration
{
    private Integer maxShownPages;

    private List<Integer> pageSizes;

    private Boolean showEntryRange;

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
     * @return whether to show or not the current entry range
     */
    public Boolean getShowEntryRange()
    {
        return showEntryRange;
    }

    /**
     * Sets whether to show or not the current entry range.
     * 
     * @param showEntryRange {@code true} to show the current entry range, {@code false} otherwise
     */
    public void setShowEntryRange(Boolean showEntryRange)
    {
        this.showEntryRange = showEntryRange;
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
        if (this.maxShownPages == null) {
            this.maxShownPages = 10;
        }
        if (this.pageSizes == null) {
            this.pageSizes = Arrays.asList(15, 25, 50, 100);
        }
        if (this.showEntryRange == null) {
            this.showEntryRange = true;
        }
        if (this.showNextPrevious == null) {
            this.showNextPrevious = true;
        }
    }
}
