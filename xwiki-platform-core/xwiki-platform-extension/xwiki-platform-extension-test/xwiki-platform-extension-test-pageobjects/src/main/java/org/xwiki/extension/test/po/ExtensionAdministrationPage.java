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

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The extension administration page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionAdministrationPage extends ViewPage
{
    /**
     * Opens the extension manager administration page.
     * 
     * @return the extension manager administration page
     */
    public static ExtensionAdministrationPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "section=XWiki.Extensions");
        return new ExtensionAdministrationPage();
    }

    /**
     * Opens the extension manager administration page that lists the installed extensions.
     * 
     * @return the extension manager administration page
     */
    public static ExtensionAdministrationPage gotoInstalledExtensions()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "section=XWiki.Extensions&repo=installed");
        return new ExtensionAdministrationPage();
    }

    /**
     * Opens the extension manager administration page that lists the core extensions.
     * 
     * @return the extension manager administration page
     */
    public static ExtensionAdministrationPage gotoCoreExtensions()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "section=XWiki.Extensions&repo=core");
        return new ExtensionAdministrationPage();
    }

    /**
     * @return the extension search bar
     */
    public SimpleSearchPane getSearchBar()
    {
        return new SimpleSearchPane();
    }

    /**
     * @return the currently displayed extension search results
     */
    public SearchResultsPane getSearchResults()
    {
        return new SearchResultsPane();
    }

    /**
     * Enabled/disable recommended extensions filtering.
     * 
     * @return the extension manager administration page
     * @since 12.10
     */
    public ExtensionAdministrationPage setRecommended(boolean enabled)
    {
        SimpleSearchPane simpleSearchPane = getSearchBar();
        simpleSearchPane.setRecommended(enabled);
        simpleSearchPane.clickButton();

        return new ExtensionAdministrationPage();
    }

    /**
     * Enabled/disable extensions index.
     * 
     * @return the extension manager administration page
     * @since 12.10
     */
    public ExtensionAdministrationPage setIndexed(boolean enabled)
    {
        SimpleSearchPane simpleSearchPane = getSearchBar();
        simpleSearchPane.setIndexed(enabled);
        simpleSearchPane.clickButton();

        return new ExtensionAdministrationPage();
    }

    /**
     * Starts the extensions indexation, by clicking on the "Index" button.
     *
     * @since 15.5
     */
    public void startIndex()
    {
        getDriver().findElement(By.cssSelector("input[name=\"index_start\"]")).click();
    }
}
