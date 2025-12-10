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
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Represents the actions possible on the search administration section.
 *
 * @version $Id$
 * @since 17.8.0RC1
 */
public class SearchAdministrationPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "Search";

    @FindBy(id = "XWiki.SearchConfigClass_0_engine")
    private WebElement searchEngineField;

    @FindBy(id = "XWiki.SearchConfigClass_0_exclusions")
    private WebElement searchExclusionsField;

    /**
     * Open the search administration section.
     *
     * @return the search administration section
     */
    public static SearchAdministrationPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new SearchAdministrationPage();
    }

    /**
     * Default constructor.
     */
    public SearchAdministrationPage()
    {
        super(SECTION_ID, true);
    }

    /**
     * @return the field used to select the search engine
     */
    public Select getSearchEngineField()
    {
        return new Select(this.searchEngineField);
    }

    /**
     * @return the field used to configure search exclusions
     */
    public SuggestInputElement getSearchExclusionsField()
    {
        return new SuggestInputElement(this.searchExclusionsField);
    }
}
