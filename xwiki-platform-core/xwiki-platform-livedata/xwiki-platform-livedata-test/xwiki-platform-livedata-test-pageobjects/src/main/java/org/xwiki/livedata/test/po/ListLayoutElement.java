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
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ConfirmationBox;
import org.xwiki.test.ui.po.FormContainerElement;

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

    // The displayed part of the main values, without their label (displayed while editing).
    private static final By MAIN_VALUE = By.cssSelector(".layout-list-item-main > .livedata-displayer");

    private static final By DELETE_ACTION = By.cssSelector(".layout-list-item-delete");

    private static final By NEW_ITEM = By.cssSelector(".layout-list-new-item");

    private static final By EDITOR_INPUT = By.cssSelector(".livedata-displayer .edit input");

    // The displayed part of the secondary values, without their label for screen readers.
    private static final By SECONDARY_DISPLAYED_VALUES =
        By.cssSelector(".layout-list-item-secondary-value > .livedata-displayer");

    private static final By SECONDARY_LABELS =
        By.cssSelector(".layout-list-item-secondary-value > .layout-list-item-label");

    private static final By LABELS = By.className("layout-list-item-label");

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
     * The labels of the properties are only displayed while editing, in edit mode or when editing a value.
     *
     * @param itemNumber the number of the entry, starting at 1
     * @return the labels of the values of the entry that are displayed, in the order they are displayed
     * @since 18.10.0RC1
     */
    public List<String> getDisplayedLabels(int itemNumber)
    {
        return getDriver().findElementsWithoutWaiting(getItems().get(itemNumber - 1), LABELS).stream()
            .filter(WebElement::isDisplayed).map(WebElement::getText).toList();
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

    /**
     * @return {@code true} if the list offers to add an entry, which happens in edit mode, {@code false} otherwise
     * @since 18.10.0RC1
     */
    public boolean canAddEntry()
    {
        return !getDriver().findElementsWithoutWaiting(getLayout(), NEW_ITEM).isEmpty();
    }

    /**
     * @return {@code true} if the entries offer to be deleted, which happens in edit mode, {@code false} otherwise
     * @since 18.10.0RC1
     */
    public boolean hasDeleteActions()
    {
        return !getDriver().findElementsWithoutWaiting(getLayout(), DELETE_ACTION).isEmpty();
    }

    /**
     * Add an item to the list. The entry itself is only created once a first value is set on that item, see
     * {@link #createNewEntry(String, String)}. The first editable value of the new item is edited right away.
     *
     * @since 18.10.0RC1
     */
    public void startNewEntry()
    {
        getLayout().findElement(NEW_ITEM).findElement(By.tagName("a")).click();
        getDriver().waitUntilElementIsVisible(getLayout(), EDITOR_INPUT);
    }

    /**
     * Fill a field of the new item and validate it, which creates the entry, then wait for the entry to be saved.
     *
     * @param fieldName the name of the field to fill (i.e., the corresponding XClass property)
     * @param value the value to set
     * @since 18.10.0RC1
     */
    public void createNewEntry(String fieldName, String value)
    {
        List<WebElement> items = getItems();
        WebElement newItem = items.get(items.size() - 1);
        WebElement input = newItem.findElement(By.cssSelector(String.format("[name$='_%s']", fieldName)));
        new FormContainerElement(By.cssSelector(".livedata-displayer .edit")).setFieldValue(input, value);
        input.sendKeys(Keys.ENTER);
        // Once saved, the new entry offers to be deleted instead of canceled.
        int itemNumber = items.size();
        getDriver().waitUntilCondition(webDriver -> {
            try {
                return "Delete".equals(getItems().get(itemNumber - 1).findElement(DELETE_ACTION)
                    .getDomAttribute("title"));
            } catch (StaleElementReferenceException | IndexOutOfBoundsException e) {
                return false;
            }
        });
        waitUntilReady();
    }

    /**
     * Delete an entry, from its delete action, then confirm the deletion and wait for the entry to be removed.
     *
     * @param itemNumber the number of the entry, starting at 1
     * @since 18.10.0RC1
     */
    public void deleteEntry(int itemNumber)
    {
        int itemCount = countItems();
        getItems().get(itemNumber - 1).findElement(DELETE_ACTION).click();
        new ConfirmationBox().clickYes();
        waitUntilItemCountEqualsTo(itemCount - 1);
        waitUntilReady();
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
