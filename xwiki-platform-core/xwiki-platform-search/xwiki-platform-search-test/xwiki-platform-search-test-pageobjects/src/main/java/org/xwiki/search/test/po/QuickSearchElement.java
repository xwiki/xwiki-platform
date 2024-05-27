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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

public class QuickSearchElement extends BaseElement
{
    @FindBy(id = "headerglobalsearchinput")
    private WebElement searchInput;

    @FindBy(css = "#globalsearch button")
    private WebElement searchButton;

    /**
     * Enter text in the quick search box.
     * @param terms text to search
     */
    public void search(String terms)
    {
        this.searchButton.click();
        this.searchInput.clear();
        this.searchInput.sendKeys(terms);
        getDriver().waitUntilElementIsVisible(By.cssSelector(".searchSuggest li.showAllResults.loading"));
    }

    /**
     * Get a suggest item from the "Page Titles" category.
     * @param index the index of the item to get
     * @return the title of the item, or null if it could not be found
     */
    public WebElement getSuggestItemTitles(int index)
    {
        getDriver().waitUntilElementIsVisible(By.cssSelector(".searchSuggest .results0:not(.loading)"));

        WebElement suggestItemsListTitles = getDriver().findElement(By.cssSelector(".results0 .suggestList"));
        if (suggestItemsListTitles == null) {
            return null;
        }

        List<WebElement> titles = suggestItemsListTitles.findElements(By.xpath("//div[@class = 'value']"));
        if (index < titles.size()) {
            return titles.get(index);
        } else {
            return null;
        }
    }
}
