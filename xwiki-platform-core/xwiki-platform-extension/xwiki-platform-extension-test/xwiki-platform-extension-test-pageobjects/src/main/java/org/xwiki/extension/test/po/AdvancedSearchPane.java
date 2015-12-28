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
import org.xwiki.extension.ExtensionId;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the advanced extension search form.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class AdvancedSearchPane extends BaseElement
{
    /**
     * The text input used to specify the extension ID.
     */
    @FindBy(id = "advancedExtensionSearch-id")
    private WebElement idInput;

    /**
     * The text input used to specify the extension version.
     */
    @FindBy(id = "advancedExtensionSearch-version")
    private WebElement versionInput;

    /**
     * The submit button.
     */
    @FindBy(xpath = "//*[@id = 'extension-search-advanced']//input[@type = 'submit']")
    private WebElement searchButton;

    /**
     * The cancel button.
     */
    @FindBy(xpath = "//*[@id = 'extension-search-advanced']//a[@href = '#extension-search-simple']")
    private WebElement cancelButton;

    /**
     * @return the text input used to specify the extension ID
     */
    public WebElement getIdInput()
    {
        return idInput;
    }

    /**
     * @return the text input used to specify the extension version
     */
    public WebElement getVersionInput()
    {
        return versionInput;
    }

    /**
     * @return the submit button
     */
    public WebElement getSearchButton()
    {
        return searchButton;
    }

    /**
     * @return the cancel button
     */
    public WebElement getCancelButton()
    {
        return cancelButton;
    }

    /**
     * Searches for the specified extension.
     * 
     * @param extensionId the extension identifier
     * @param extensionVersion the extension version
     * @return the search results pane
     */
    public SearchResultsPane search(CharSequence extensionId, CharSequence extensionVersion)
    {
        idInput.clear();
        idInput.sendKeys(extensionId);
        versionInput.clear();
        versionInput.sendKeys(extensionVersion);
        searchButton.click();
        return new SearchResultsPane();
    }

    /**
     * Searches for the specified extension.
     * 
     * @param extensionId the extension identifier
     * @return the search results pane
     */
    public SearchResultsPane search(ExtensionId extensionId)
    {
        return search(extensionId.getId(), extensionId.getVersion().getValue());
    }
}
