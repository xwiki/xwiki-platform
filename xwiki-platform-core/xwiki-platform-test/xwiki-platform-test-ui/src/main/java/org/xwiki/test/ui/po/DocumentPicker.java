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
package org.xwiki.test.ui.po;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * Represents the Document Picker.
 *
 * @version $Id$
 * @since 7.2M3
 */
public class DocumentPicker extends BaseElement
{
    protected WebElement container;

    /**
     * Use this constructor only if there is only one document picker on the page.
     */
    public DocumentPicker()
    {
        this.container = getDriver().findElement(By.className("location-picker"));
        waitUntilReady();
    }

    public DocumentPicker(WebElement container)
    {
        this.container = container;
        waitUntilReady();
    }

    public String getTitle()
    {
        return getTitleInput().getAttribute("value");
    }

    public DocumentPicker setTitle(String title)
    {
        getDriver().setTextInputValue(getTitleInput(), title);
        return this;
    }

    /**
     * Wait until the name field has been changed.
     * @param name the new name
     * @return this.
     */
    public DocumentPicker waitForName(String name)
    {
        try {
            getDriver().waitUntilCondition(webDriver -> name.equals(getName()));
        } catch (TimeoutException e) {
            throw new WebDriverException(String.format("Expecting name: [%s] and obtained name [%s]", name, getName()),
                e);
        }
        return this;
    }

    public WebElement getTitleInput()
    {
        return this.container.findElement(By.className("location-title-field"));
    }

    public BreadcrumbElement getLocation()
    {
        return new BreadcrumbElement(this.container.findElement(By.className("breadcrumb")));
    }

    public DocumentPicker toggleLocationAdvancedEdit()
    {
        this.container.findElement(By.className("location-action-edit")).click();
        return this;
    }

    public String getParent()
    {
        return getParentInput().getAttribute("value");
    }

    public DocumentPicker setParent(String parent)
    {
        return setAdvancedField(getParentInput(), parent);
    }

    public WebElement getParentInput()
    {
        return this.container.findElement(By.className("location-parent-field"));
    }

    public String getName()
    {
        return getNameInput().getAttribute("value");
    }

    public DocumentPicker setName(String name)
    {
        return setAdvancedField(getNameInput(), name);
    }

    public WebElement getNameInput()
    {
        return this.container.findElement(By.className("location-name-field"));
    }

    private DocumentPicker setAdvancedField(WebElement field, String value)
    {
        if (!field.isDisplayed()) {
            toggleLocationAdvancedEdit();
        }
        getDriver().setTextInputValue(field, value);
        return this;
    }

    public DocumentPicker setWiki(String wikiName)
    {
        WebElement selectWebElement = this.container.findElement(By.className("location-wiki-field"));
        if (!selectWebElement.isDisplayed())
        {
            toggleLocationAdvancedEdit();
        }

        Select wikiSelect = new Select(selectWebElement);
        wikiSelect.selectByValue(wikiName);
        return this;
    }

    /**
     * Clicks the "pick document" button that triggers a modal pop-up to be displayed.
     * <p>
     * The caller is responsible for handling the modal (or instantiating the right page object element), such we limit
     * the extra coupling that would be required from the test framework if it were to instantiate and return the page
     * object for the modal pop-up.
     */
    public void browseDocuments()
    {
        this.container.findElement(By.className("location-action-pick")).click();
    }

    /**
     * Wait for the Breadcrumb to display the passed path string and throw an exception if the timeout is reached. Note
     * that we need to wait since the Breadcrumb is updated live and asserting its content without waiting would lead to
     * false positives.
     * <p>
     * Note: This method can not be implemented inside {@link BreadcrumbElement} because a change of parent replaces
     * completely the {@link BreadcrumbElement}'s container and thus it becomes stale. To avoid that, at each wait
     * iteration, we lookup the current breadcrumb element and not a cached one.
     *
     * @param expectedPathString the path string to wait for
     * @since 7.2RC1
     */
    public void waitForLocation(final String expectedPathString)
    {
        // TODO: Ugly hack. Would need to find a better solution
        final StringBuilder currentValue = new StringBuilder();

        try {
            getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    try {
                        String value = getLocation().getPathAsString();

                        currentValue.setLength(0);
                        currentValue.append(value);

                        return expectedPathString.equals(value);
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
        } catch (WebDriverException e) {
            // Display a nicer error message than would be displayed otherwise
            throw new WebDriverException(String.format("Found [%s], was expecting [%s]", currentValue.toString(),
                expectedPathString), e);
        }
    }

    /**
     * Wait for the Breadcrumb to display the passed path and throw an exception if the timeout is reached. Note that we
     * need to wait since the Breadcrumb is updated live and asserting its content without waiting would lead to false
     * positives.
     * <p>
     * Note: This method can not be implemented inside {@link BreadcrumbElement} because a change of parent replaces
     * completely the {@link BreadcrumbElement}'s container and thus it becomes stale. To avoid that, at each wait
     * iteration, we lookup the current breadcrumb element and not a cached one.
     *
     * @param expectedPath the path to wait for
     * @since 7.2RC1
     */
    public void waitForLocation(final List<String> expectedPath)
    {
        // TODO: Ugly hack. Would need to find a better solution
        final List<String> currentPath = new ArrayList<String>();

        try {
            getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    try {
                        List<String> path = getLocation().getPath();

                        currentPath.clear();
                        currentPath.addAll(path);

                        return expectedPath.equals(path);
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
        } catch (WebDriverException e) {
            // Display a nicer error message than would be displayed otherwise
            throw new WebDriverException(String.format("Found %s, was expecting %s", currentPath, expectedPath), e);
        }
    }

    /**
     * In order to use the document picker we need to wait for the JavaScript code to initialize it (e.g. to add its
     * event listeners).
     */
    private void waitUntilReady()
    {
        List<WebElement> pickAction =
            getDriver().findElementsWithoutWaiting(this.container, By.className("location-action-pick"));
        List<WebElement> editAction =
            getDriver().findElementsWithoutWaiting(this.container, By.className("location-action-edit"));
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public @Nullable Boolean apply(@Nullable WebDriver driver)
            {
                // Wait until the document picker JavaScript initialization code is executed.
                // The pick action is available only if the document tree is available.
                return (pickAction.isEmpty() || getJQueryListenerCount(pickAction.get(0), "click") > 1)
                    // The edit action is available only for advanced users.
                    && (editAction.isEmpty() || getJQueryListenerCount(editAction.get(0), "click") > 0);
            }
        });
    }

    private int getJQueryListenerCount(WebElement element, String event)
    {
        StringBuilder script = new StringBuilder();
        script.append("var element = arguments[0];\n");
        script.append("var event = arguments[1];\n");
        script.append("var callback = arguments[2];\n");
        script.append("require(['jquery'], function($) {\n");
        // This is internal jQuery API but we'll notice if they change it: the test will fail.
        script.append("  var listeners = $._data(element, 'events') || {};\n");
        script.append("  callback((listeners[event] || []).length);\n");
        script.append("});\n");
        return ((Long) getDriver().executeAsyncScript(script.toString(), element, event)).intValue();
    }
}
