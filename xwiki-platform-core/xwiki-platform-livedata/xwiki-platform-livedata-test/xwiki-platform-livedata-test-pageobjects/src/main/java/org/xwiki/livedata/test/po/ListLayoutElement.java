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
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Provides the operations to interact with a Live Data when displayed with the list layout. Each entry is displayed on
 * a compact line: its main property, followed by its other visible properties (the secondary values).
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public class ListLayoutElement extends BaseElement
{
    private static final By ITEMS = By.cssSelector(".layout-list-root > .layout-list-item");

    private static final By MAIN_VALUE = By.cssSelector(".layout-list-item-main");

    // The displayed part of the secondary values, without their label for screen readers.
    private static final By SECONDARY_DISPLAYED_VALUES =
        By.cssSelector(".layout-list-item-secondary-value > .livedata-displayer");

    private static final By SECONDARY_LABELS =
        By.cssSelector(".layout-list-item-secondary-value > .layout-list-item-label");

    private final LiveDataElement liveData;

    /**
     * Default constructor. Initializes a Live Data list layout page object.
     *
     * @param liveData the live data displayed with the list layout
     */
    public ListLayoutElement(LiveDataElement liveData)
    {
        this.liveData = liveData;
    }

    /**
     * Waits until the list layout is displayed and its entries are loaded.
     */
    public void waitUntilReady()
    {
        getDriver().waitUntilCondition(webDriver -> {
            try {
                WebElement layout = getLayout();
                List<String> layoutLoaderClasses =
                    Arrays.asList(layout.findElement(By.className("layout-loader")).getDomAttribute("class")
                        .split("\\s+"));
                return !layoutLoaderClasses.contains("waiting") && !layoutLoaderClasses.contains("loading")
                    && layout.findElements(By.cssSelector(".layout-list-item .xwiki-loader")).isEmpty();
            } catch (StaleElementReferenceException e) {
                // Vue is (re)mounting the layout, it is not ready yet.
                return false;
            }
        }, 20);
    }

    /**
     * Waits until the number of entries displayed by the list matches the expected count.
     *
     * @param expectedItemCount the number of expected entries
     */
    public void waitUntilItemCountEqualsTo(int expectedItemCount)
    {
        getDriver().waitUntilCondition(webDriver -> {
            try {
                return countItems() == expectedItemCount;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    /**
     * @return the number of entries displayed by the list
     */
    public int countItems()
    {
        return getItems().size();
    }

    /**
     * @return the text of the main value of each displayed entry, in the order they are displayed
     */
    public List<String> getMainValues()
    {
        return getItems().stream().map(item -> getDriver().findElementWithoutWaiting(item, MAIN_VALUE).getText())
            .toList();
    }

    /**
     * @param itemNumber the number of the entry, starting at 1
     * @return the text of the secondary values of the entry, in the order they are displayed; the properties without
     *     a value for the entry are not displayed
     */
    public List<String> getSecondaryValues(int itemNumber)
    {
        return getDriver().findElementsWithoutWaiting(getItems().get(itemNumber - 1), SECONDARY_DISPLAYED_VALUES)
            .stream().map(WebElement::getText).toList();
    }

    /**
     * The secondary values are labelled with their property name, for screen readers only.
     *
     * @param itemNumber the number of the entry, starting at 1
     * @return the labels of the secondary values of the entry, in the order they are displayed
     * @since 18.10.0RC1
     */
    public List<String> getSecondaryLabels(int itemNumber)
    {
        return getDriver().findElementsWithoutWaiting(getItems().get(itemNumber - 1), SECONDARY_LABELS).stream()
            .map(label -> label.getDomProperty("textContent").trim()).toList();
    }

    /**
     * @return {@code true} if a marker is displayed before each entry, which happens when the entries display a
     *     single property, {@code false} otherwise
     * @since 18.10.0RC1
     */
    public boolean hasItemMarkers()
    {
        return getDriver().findElementsWithoutWaiting(getLayout(), By.cssSelector(".layout-list-single")).size() == 1;
    }

    /**
     * @return {@code true} if at least one of the displayed values offers to be edited, {@code false} otherwise
     */
    public boolean hasEditableValues()
    {
        return !getDriver().findElementsWithoutWaiting(getLayout(),
            By.cssSelector(".layout-list-item .livedata-displayer .editable")).isEmpty();
    }

    private List<WebElement> getItems()
    {
        return getDriver().findElementsWithoutWaiting(getLayout(), ITEMS);
    }

    private WebElement getLayout()
    {
        return getDriver().findElement(By.id(this.liveData.getId())).findElement(By.className("layout-list"));
    }
}
