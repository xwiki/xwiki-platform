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
package org.xwiki.search.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object representing a quick search result.
 *
 * @version $Id$
 * @since 17.8.0RC1
 */
public class QuickSearchResult extends BaseElement
{
    private final WebElement container;

    /**
     * Creates a new instance based on the given element.
     * 
     * @param container the element that contains the quick search result
     */
    public QuickSearchResult(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the title of this quick search result
     */
    public String getTitle()
    {
        return this.container.findElement(By.cssSelector(".value")).getText();
    }
}
