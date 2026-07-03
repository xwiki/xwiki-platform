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

import org.openqa.selenium.By;
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

    @FindBy(id = "solrAdminAction")
    private WebElement indexingActionField;

    @FindBy(id = "solrAdminWiki")
    private WebElement indexingWikiField;

    @FindBy(className = "solrQueueSize")
    private WebElement queueSizeField;

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

    /**
     * @return the dropdown list used to select the indexing action to perform (add to the index, delete from the
     *         index or reindex)
     * @since 17.10.10
     * @since 18.4.3
     * @since 18.5.0RC1
     */
    public Select getIndexingActionField()
    {
        return new Select(this.indexingActionField);
    }

    /**
     * @return the dropdown list used to select the wiki on which to perform the indexing action (or the entire farm
     *         when the empty value is selected)
     * @since 17.10.10
     * @since 18.4.3
     * @since 18.5.0RC1
     */
    public Select getIndexingWikiField()
    {
        return new Select(this.indexingWikiField);
    }

    /**
     * Submit the indexing action form, i.e. click the "Apply" button. This triggers a full-page reload.
     *
     * @since 17.10.10
     * @since 18.4.3
     * @since 18.5.0RC1
     */
    public void submitIndexingAction()
    {
        getDriver().findElement(
            By.xpath("//select[@id = 'solrAdminAction']/ancestor::form//input[@type = 'submit']")).click();
    }

    /**
     * @return the current size of the Solr indexing queue, as displayed in the administration section
     * @since 17.10.10
     * @since 18.4.3
     * @since 18.5.0RC1
     */
    public int getQueueSize()
    {
        return Integer.parseInt(this.queueSizeField.getText().trim());
    }

    /**
     * @return the text of the success message displayed after submitting an indexing action
     * @since 17.10.10
     * @since 18.4.3
     * @since 18.5.0RC1
     */
    public String getActionResultMessage()
    {
        return getDriver().findElement(By.cssSelector("div.box.successmessage")).getText();
    }
}
