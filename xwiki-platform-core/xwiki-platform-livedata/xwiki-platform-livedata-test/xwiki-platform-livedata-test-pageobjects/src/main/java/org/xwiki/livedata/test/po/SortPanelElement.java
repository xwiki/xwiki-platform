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

package org.xwiki.livedata.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the advanced panel for sorting columns.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
public class SortPanelElement extends AbstractLiveDataAdvancedPanelElement
{
    /**
     * Default constructor.
     * @param liveData the livedata of the panel.
     * @param container the container of the panel.
     */
    public SortPanelElement(LiveDataElement liveData, WebElement container)
    {
        super(liveData, container);
    }

    /**
     * Remove all sorting on all columns.
     */
    public void clearAllSort()
    {
        this.clickOnMouseOverLinks(By.className("sort-entry"), By.className("delete-sort"));
        this.liveData.waitUntilReady();
    }
}
