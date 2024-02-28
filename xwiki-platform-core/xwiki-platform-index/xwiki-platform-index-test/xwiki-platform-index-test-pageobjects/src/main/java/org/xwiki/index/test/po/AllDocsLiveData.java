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
package org.xwiki.index.test.po;

import org.xwiki.livedata.test.po.LiveDataElement;

/**
 * Represents the livetable for index all docs.
 *
 * @version $Id$
 * @since 11.6RC1
 * @since 11.5
 */
public class AllDocsLiveData extends LiveDataElement
{
    /**
     * Default constructor.
     */
    public AllDocsLiveData()
    {
        super("alldocs");
    }

    /**
     * Filter a column using the provided value.
     *
     * @param columnLabel the label of the column to filter
     * @param filterValue the value to use to filter
     */
    public void filterColumn(String columnLabel, String filterValue)
    {
        // TODO: replace column number with the column label.
        getTableLayout().filterColumn(columnLabel, filterValue);
    }

    /**
     * Click on a Live Data action.
     *
     * @param rowNumber the number of the row of the action (start at 1 for the first row)
     * @param actionName the name of the action to click (e.g., 'copy')
     */
    public void clickAction(int rowNumber, String actionName)
    {
        getTableLayout().clickAction(rowNumber, actionName);
    }
}
