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
package org.xwiki.administration.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Represent an select field enhanced with the "bootstrap-select" plugin
 * @version $Id$
 */
public class BootstrapSelect
{
    private WebElement element;

    private WebElement button;

    private WebElement menu;

    private WebElement hiddenSelect;

    private XWikiWebDriver driver;

    public BootstrapSelect(WebElement element, XWikiWebDriver driver)
    {
        this.driver = driver;
        this.element = element;
        this.button = element.findElement(By.tagName("button"));
        this.menu = element.findElement(By.cssSelector(".dropdown-menu"));
        this.hiddenSelect = element.findElement(By.tagName("select"));
    }

    public boolean isMultiple()
    {
        String value = this.hiddenSelect.getAttribute("multiple");
        return value != null && !"false".equals(value);
    }

    public void selectByValues(List<String> values)
    {
        // Get the list of all possible values in the hidden <select> field
        // WARN: the order of the values is imported since their index will help us to identify
        // them in the enhanced bootstrap-select widget.
        List<String> options = new ArrayList<>();
        for (WebElement element : this.hiddenSelect.findElements(By.tagName("option"))) {
            options.add(element.getAttribute("value"));
        }

        // Open the enhanced bootstrap-select widget
        openMenu();

        // For each of its option
        for (WebElement element : this.menu.findElements(By.tagName("li"))) {
            // We find the language associated to this item thanks to the attribute "data-original-index"
            int index = Integer.parseInt(element.getAttribute("data-original-index"));
            String value = options.get(index);

            // Now, click on the element to select/unselect it if its status is different from what we want
            if (isElementSelected(element) != values.contains(value)) {
                element.findElement(By.tagName("a")).click();
                // If the element is not displayed inside the window (selenium does not handle scrolling inside an
                // element) the previous action has actually closed the menu, withotu changing the state of the element.
                // So we need to check if the menu has been close.
                if (!this.menu.isDisplayed()) {
                    // In that was, we reopen it
                    openMenu();
                    // When we reopen the menu, the element is now contained inside the viewport (the previous click
                    // had some effect) so we can click it.
                    // TODO: make this less hacky.
                    element.findElement(By.tagName("a")).click();
                }
            }
        }

        // To finish, close the enhanced bootstrap-select widget
        closeMenu();
    }

    private void openMenu()
    {
        this.button.click();
        driver.waitUntilCondition(new ExpectedCondition<Object>()
        {
            @Override
            public Object apply(WebDriver webDriver)
            {
                return menu.isDisplayed();
            }
        });
    }

    private void closeMenu()
    {
        this.button.click();
        driver.waitUntilCondition(new ExpectedCondition<Object>()
        {
            @Override
            public Object apply(WebDriver webDriver)
            {
                return !menu.isDisplayed();
            }
        });
    }

    private boolean isElementSelected(WebElement element)
    {
        return element.getAttribute("class").contains("selected");
    }
}
