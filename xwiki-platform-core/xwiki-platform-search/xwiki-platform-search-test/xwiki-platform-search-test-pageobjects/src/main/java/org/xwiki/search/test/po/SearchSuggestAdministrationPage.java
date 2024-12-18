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
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.test.ui.po.Select;

/**
 * Represents the actions possible on the Sarch Suggest administration section.
 *
 * @version $Id$
 */
public class SearchSuggestAdministrationPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "searchSuggest";

    @FindBy(id = "XWiki.SearchSuggestConfig_0_activated")
    private WebElement searchSuggestActivationSelect;

    /**
     * Open the Search Suggest administration section.
     *
     * @return the Search Suggest administration section
     */
    public static SearchSuggestAdministrationPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new SearchSuggestAdministrationPage();
    }

    /**
     * Default constructor.
     */
    public SearchSuggestAdministrationPage()
    {
        super(SECTION_ID, true);
    }

    public void setActivated(boolean activated)
    {
        Select select = new Select(this.searchSuggestActivationSelect);
        select.selectByValue(activated ? "1" : "0");
    }
}
