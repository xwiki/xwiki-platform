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
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.XWikiWebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Represent an select field enhanced with the "bootstrap-select" plugin.
 *
 * @version $Id$
 * @since 8.4.2
 * @since 9.0RC1
 */
public class BootstrapSelect
{
    private WebElement button;

    private WebElement hiddenSelect;

    private XWikiWebDriver driver;

    private By menuLocator = By.cssSelector("body > .bootstrap-select.open > .dropdown-menu.open");

    public BootstrapSelect(WebElement element, XWikiWebDriver driver)
    {
        this.driver = driver;
        this.button = element.findElement(By.tagName("button"));
        this.hiddenSelect = element.findElement(By.tagName("select"));
    }

    public boolean isMultiple()
    {
        String value = this.hiddenSelect.getAttribute("multiple");
        return value != null && !"false".equals(value);
    }

    public void selectByValue(String value)
    {
        selectByValues(Arrays.asList(value));
    }

    public void selectByValues(List<String> values)
    {
        boolean multipleSelect = isMultiple();

        if (!multipleSelect && values.size() > 1) {
            throw new AssertionError("More than one value specified for a select that does not allow multiple values.");
        }

        List<String> valuesToToggle = new ArrayList<>();

        // The list of values is usually small while the list of all options can be huge - use one XPath query per
        // value.
        for (String value : values) {
            assertFalse(value.contains("\""), "Value must not contain a double quote");
            WebElement element = this.hiddenSelect.findElement(By.xpath("option[@value = \"" + value + "\"]"));

            if (element.getAttribute("selected") == null) {
                valuesToToggle.add(element.getText());
            }
        }

        if (multipleSelect) {
            // Find all currently selected elements using a single XPath query to deselect them.
            for (WebElement element : this.hiddenSelect.findElements(By.xpath("option[@selected = \"selected\"]"))) {
                String value = element.getAttribute("value");

                // Toggle the selection state if the value should not be selected anymore.
                if (!values.contains(value)) {
                    valuesToToggle.add(element.getText());
                }
            }
        }

        // Open the enhanced bootstrap-select widget
        WebElement menu = openMenu();

        for (String title : valuesToToggle) {
            WebElement filterInput = getFilterInput(menu);
            if (filterInput != null) {
                filterInput.clear();
                filterInput.sendKeys(title);
            }

            // Find the element to select.
            for (WebElement element : menu.findElements(By.tagName("li"))) {
                String elementText = element.getText();

                if (title.equals(elementText)) {
                // Now, click on the element to (un) select the value
                    element.findElement(By.tagName("a")).click();

                    if (!multipleSelect) {
                        waitUntilMenuIsClosed();
                        assertEquals(elementText, button.getAttribute("title"),
                            String.format("Failed to set the value with the title [%s]. Got [%s] instead.",
                                elementText,
                                button.getAttribute("title")));
                        break;
                    } else {
                        // If the element is not displayed inside the window (selenium does not handle scrolling inside an
                        // element) the previous action has actually closed the menu, without changing the state of the
                        // element. So we need to check if the menu has been close.
                        if (!isMenuOpen()) {
                            // If that was the case, we reopen it.
                            openMenu();
                            // When we reopen the menu, the element is now contained inside the viewport (the previous click
                            // had some effect) so we can click it.
                            // TODO: make this less hacky.
                            element.findElement(By.tagName("a")).click();
                        }
                    }
                }
            }
        }

        // To finish, close the enhanced bootstrap-select widget.
        if (isMenuOpen()) {
            closeMenu();
        }
    }

    private WebElement getFilterInput(WebElement menu)
    {
        try {
            return this.driver.findElementWithoutWaiting(menu, By.tagName("input"));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private WebElement waitUntilMenuIsOpened()
    {
        this.driver.waitUntilElementIsVisible(this.menuLocator);
        return this.driver.findElementWithoutWaiting(this.menuLocator);
    }

    private void waitUntilMenuIsClosed()
    {
        this.driver.waitUntilElementDisappears(this.menuLocator);
    }

    private boolean isMenuOpen()
    {
        return this.driver.hasElementWithoutWaitingWithoutScrolling(this.menuLocator);
    }

    private WebElement openMenu()
    {
        this.button.click();
        return waitUntilMenuIsOpened();
    }

    private void closeMenu()
    {
        this.button.click();
        waitUntilMenuIsClosed();
    }

    private boolean isElementSelected(WebElement element)
    {
        return element.getAttribute("class").contains("selected");
    }
}
