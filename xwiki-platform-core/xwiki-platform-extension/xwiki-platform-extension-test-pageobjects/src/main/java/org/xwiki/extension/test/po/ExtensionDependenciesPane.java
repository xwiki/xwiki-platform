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
package org.xwiki.extension.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The section that displays the extension dependencies.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionDependenciesPane extends BaseElement
{
    /**
     * The section container.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the section container
     */
    public ExtensionDependenciesPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the list of direct dependencies
     */
    public List<DependencyPane> getDirectDependencies()
    {
        // Wait until all remote dependencies are resolved asynchronously.
        getDriver().waitUntilCondition(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                return ((XWikiWebDriver) driver).findElementsWithoutWaiting(container,
                    By.className("extension-item-loading")).size() > 0 ? null : container;
            }
        });
        return getDependenciesAfter("This extension depends on:");
    }

    /**
     * @return the list of backward dependencies
     */
    public List<DependencyPane> getBackwardDependencies()
    {
        return getDependenciesAfter("This extension is required by:");
    }

    /**
     * @param label the text that precedes the list of dependencies
     * @return the list of dependencies after the specified label
     */
    List<DependencyPane> getDependenciesAfter(String label)
    {
        By xpath =
            By.xpath(".//*[contains(@class, 'dependency-item') and ancestor::ul[preceding-sibling::p[starts-with(., '"
                + label + "')]]]");
        List<DependencyPane> dependencies = new ArrayList<DependencyPane>();
        for (WebElement element : getDriver().findElementsWithoutWaiting(container, xpath)) {
            dependencies.add(new DependencyPane(element));
        }
        return dependencies;
    }
}
