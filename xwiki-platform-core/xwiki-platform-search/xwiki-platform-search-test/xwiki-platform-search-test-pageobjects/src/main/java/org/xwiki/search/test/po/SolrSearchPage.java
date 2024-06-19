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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

public class SolrSearchPage extends ViewPage
{
    @FindBy(id = "search-page-bar-input")
    private WebElement searchInput;

    @FindBy(xpath = "//div[@class = 'search-ui']//button[@type = 'submit']")
    private WebElement searchButton;

    @FindBy(xpath = "//div[@class = 'search-ui']//button[@aria-controls = 'space_facet-dropdown']")
    private WebElement spaceFaucetDropdownButton;

    @FindBy(xpath = "//div[@id = 'space_facet-dropdown']")
    private WebElement spaceFaucetDropdownContent;

    public static SolrSearchPage gotoPage()
    {
        getUtil().gotoPage("Main", "SolrSearch", "view");
        return new SolrSearchPage();
    }

    public void search(String terms) {
        this.searchInput.clear();
        this.searchInput.sendKeys(terms);
        this.searchButton.click();
    }

    public void toggleSpaceFaucet() {
        this.spaceFaucetDropdownButton.click();
    }

    public String getSpaceFaucetContent() {
        return this.spaceFaucetDropdownContent.getText();
    }
}
