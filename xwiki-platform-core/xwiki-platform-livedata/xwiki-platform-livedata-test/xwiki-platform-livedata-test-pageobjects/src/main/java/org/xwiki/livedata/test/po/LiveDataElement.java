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

import java.util.Arrays;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Live Data page object. Provides the operations to obtain the page objects for the different live data layouts, and to
 * switch between them.
 *
 * @version $Id$
 * @since 13.4RC1
 */
public class LiveDataElement extends BaseElement
{
    // TODO: add the operations to switch between the layouts.

    private final String id;

    /**
     * Default constructor. Initializes a Live Data page object by its id.
     *
     * @param id the live data id
     */
    public LiveDataElement(String id)
    {
        this.id = id;
        waitUntilReady();
    }

    /**
     * @return a table layout page object for the live data
     */
    public TableLayoutElement getTableLayout()
    {
        TableLayoutElement tableLayoutElement = new TableLayoutElement(this.id);
        tableLayoutElement.waitUntilReady();
        return tableLayoutElement;
    }

    /**
     * @return a cart layout page object for the live data
     */
    public CardLayoutElement getCardLayout()
    {
        return new CardLayoutElement(this.id);
    }

    private void waitUntilReady()
    {
        // First the Live Data macro displays a simple div with the loading class.
        // This div is replaced by the Live Data Vue template once vue is loaded.
        getDriver().waitUntilCondition(
            input -> {
                String[] classes = getDriver().findElement(By.id(this.id)).getAttribute("class").split("\\s+");
                return !Arrays.asList(classes).contains("loading");
            });

        // Then, once the Vue template is loaded, a div with the loading class is inserted until the rest of the data 
        // and components required to display the Live Data are loaded too. 
        getDriver().waitUntilCondition(
            input -> getDriver().findElement(By.id(this.id)).findElements(By.cssSelector(".xwiki-livedata .loading"))
                .isEmpty()
        );
    }
}
