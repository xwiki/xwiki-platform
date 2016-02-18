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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
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
     * The link to the administration section from where we can add extensions.
     */
    @FindBy(linkText = "Add Extensions")
    private WebElement addExtensionsLink;

    /**
     * The link to the administration section that lists the installed extensions.
     */
    @FindBy(linkText = "Installed Extensions")
    private WebElement installedExtensionsLink;

    /**
     * The link to the administration section that lists the core extensions.
     */
    @FindBy(linkText = "Core Extensions")
    private WebElement coreExtensionsLink;

    /**
     * Opens the extension manager administration page.
     * 
     * @return the extension manager administration page
     */
    public static ExtensionAdministrationPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin");
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
     * Clicks on the link to the 'Add Extensions' section.
     * 
     * @return the newly loaded administration page
     */
    public ExtensionAdministrationPage clickAddExtensionsSection()
    {
        addExtensionsLink.click();
        return new ExtensionAdministrationPage();
    }

    /**
     * Clicks on the link to the 'Installed Extensions' section.
     * 
     * @return the newly loaded administration page
     */
    public ExtensionAdministrationPage clickInstalledExtensionsSection()
    {
        installedExtensionsLink.click();
        return new ExtensionAdministrationPage();
    }

    /**
     * Clicks on the link to the 'Core Extensions' section.
     * 
     * @return the newly loaded administration page
     */
    public ExtensionAdministrationPage clickCoreExtensionsSection()
    {
        coreExtensionsLink.click();
        return new ExtensionAdministrationPage();
    }
}
