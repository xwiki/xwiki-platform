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
package org.xwiki.appwithinminutes.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents the actions available when editing the application template provider. This is also the third step of the
 * App Within Minutes wizard, in which the application entries are configured.
 *
 * @version $Id$
 * @since 8.4
 */
public class ApplicationTemplateProviderEditPage extends ApplicationEditPage
{
    @FindBy(id = "XWiki.TemplateProviderClass_0_icon")
    private WebElement iconInput;

    @FindBy(id = "XWiki.TemplateProviderClass_0_description")
    private WebElement descriptionInput;

    @FindBy(id = "XWiki.TemplateProviderClass_0_creationRestrictionsAreSuggestions")
    private WebElement creationRestrictionsAreSuggestionsCheckBox;

    @FindBy(id = "XWiki.TemplateProviderClass_0_terminal")
    private WebElement terminalSelect;

    /**
     * Default constructor, wait for the next step button to be ready.
     */
    public ApplicationTemplateProviderEditPage()
    {
        super(true, false);
    }

    /**
     * Clicks on the Next Step button.
     *
     * @return the page that represents the next step of the App Within Minutes wizard
     */
    public ApplicationHomeEditPage clickNextStep()
    {
        clickSaveAndView(true);
        return new ApplicationHomeEditPage();
    }

    @Override
    public WebElement getSaveAndViewButton()
    {
        return this.nextStepButton;
    }

    /**
     * Clicks on the Previous Step button.
     *
     * @return the page that represents the previous step of the App Within Minutes wizard
     */
    public ApplicationClassEditPage clickPreviousStep()
    {
        previousStepButton.click();
        return new ApplicationClassEditPage();
    }

    public ApplicationTemplateProviderEditPage setIcon(String icon)
    {
        this.iconInput.clear();
        this.iconInput.sendKeys(icon);
        // Make sure the icon picker doesn't remain open.
        this.iconInput.sendKeys(Keys.ESCAPE);
        return this;
    }

    public String getIcon()
    {
        return this.iconInput.getText();
    }

    public ApplicationTemplateProviderEditPage setDescription(String description)
    {
        this.descriptionInput.clear();
        this.descriptionInput.sendKeys(description);
        return this;
    }

    public String getDescription()
    {
        return this.descriptionInput.getText();
    }

    public ApplicationTemplateProviderEditPage clickEnforceEntryLocation()
    {
        this.creationRestrictionsAreSuggestionsCheckBox.click();
        return this;
    }

    public boolean isEntryLocationEnforced()
    {
        return !this.creationRestrictionsAreSuggestionsCheckBox.isSelected();
    }

    public ApplicationTemplateProviderEditPage setTerminal(String visibleText)
    {
        new Select(this.terminalSelect).deselectByVisibleText(visibleText);
        return this;
    }

    public String getTerminal()
    {
        return new Select(this.terminalSelect).getFirstSelectedOption().getText();
    }

    public boolean hasTerminalOption()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector("select#XWiki\\.TemplateProviderClass_0_terminal"))
            .size() > 0;
    }
}
