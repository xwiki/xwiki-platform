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

import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ViewPage;

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

    /**
     * @return the parts of the title of this quick search result that are highlighted because they match the search
     *     terms
     * @since 17.10.14
     * @since 18.4.7
     * @since 18.9.0RC1
     */
    public List<String> getHighlights()
    {
        return this.container.findElements(By.cssSelector(".value em")).stream().map(WebElement::getText).toList();
    }

    /**
     * Opens this quick search result in the current tab, by clicking it.
     *
     * @return the page of this quick search result
     * @since 18.10.0RC1
     */
    public ViewPage open()
    {
        getDriver().addPageNotYetReloadedMarker();
        this.container.click();
        getDriver().waitUntilPageIsReloaded();
        return new ViewPage();
    }

    /**
     * Opens this quick search result in a new tab, by clicking it while holding the Ctrl key. The current tab stays
     * the active one.
     *
     * @return the handle of the new tab
     * @since 18.10.0RC1
     */
    public String openInNewTab()
    {
        Set<String> existingTabHandles = getDriver().getWindowHandles();
        getDriver().createActions().keyDown(Keys.CONTROL).click(this.container).keyUp(Keys.CONTROL).perform();
        getDriver().waitUntilCondition(driver -> driver.getWindowHandles().size() > existingTabHandles.size());
        return getDriver().getWindowHandles().stream().filter(handle -> !existingTabHandles.contains(handle))
            .findFirst().orElseThrow();
    }
}
