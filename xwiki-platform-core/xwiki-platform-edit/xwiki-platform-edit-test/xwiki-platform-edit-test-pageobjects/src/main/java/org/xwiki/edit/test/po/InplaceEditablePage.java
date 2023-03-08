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
package org.xwiki.edit.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the in-place editor for a wiki page.
 * 
 * @version $Id$
 * @since 12.10.6
 * @since 13.2RC1
 */
public class InplaceEditablePage extends ViewPage
{
    @FindBy(id = "commentinput")
    private WebElement versionSummaryInput;

    /**
     * Click on the Edit button and wait for the in-place editor to load.
     * 
     * @return this page object
     */
    public InplaceEditablePage editInplace()
    {
        edit();
        return waitForInplaceEditor();
    }

    /**
     * Clicks on the edit link for the specified section and waits for the in-place editor to be load.
     * 
     * @param sectionNumber indicates the section to edit
     * @return this page object
     */
    public InplaceEditablePage editSectionInplace(int sectionNumber)
    {
        editSection(sectionNumber);
        waitForInplaceEditor();
        return this;
    }

    /**
     * Click on the Translate button and wait for the in-place editor to load.
     * 
     * @return this page object
     */
    public InplaceEditablePage translateInplace()
    {
        getTranslateButton().click();
        return waitForInplaceEditor();
    }

    /**
     * Wait until the user can interact with the in-place editor.
     * 
     * @return this page object
     */
    public InplaceEditablePage waitForInplaceEditor()
    {
        // The loading class is not added immediately when the Edit button is clicked (because the code of the in-place
        // editor is fetched on demand) so we need to also wait for something that stays after the in-place editor is
        // ready.
        getDriver().waitUntilElementsAreVisible(
            new By[] {By.cssSelector(".xcontent.form"), By.cssSelector("#xwikicontent:not(.loading)")}, true);
        return this;
    }

    /**
     * Sets the page title.
     * 
     * @param title the new page title
     * @return this page object
     */
    public InplaceEditablePage setDocumentTitle(String title)
    {
        WebElement titleInput = getDriver().findElement(By.id("document-title-input"));
        titleInput.clear();
        titleInput.sendKeys(title);
        return this;
    }

    @Override
    public String getDocumentTitle()
    {
        List<WebElement> titleInput = getDriver().findElementsWithoutWaiting(By.id("document-title-input"));
        if (titleInput.isEmpty()) {
            // View mode.
            return super.getDocumentTitle();
        } else {
            // Edit (in-place) mode.
            return titleInput.get(0).getAttribute("value");
        }
    }

    /**
     * @return {@code true} if the document title input has an invalid value (e.g. empty value when document titles are
     *         mandatory), {@code false} otherwise
     */
    public boolean isDocumentTitleInvalid()
    {
        return !getDriver().findElementsWithoutWaiting(By.cssSelector("input#document-title-input:invalid")).isEmpty();
    }

    /**
     * @return the message displayed when the document title validation fails
     */
    public String getDocumentTitleValidationMessage()
    {
        return getDriver().findElement(By.id("document-title-input")).getDomProperty("validationMessage");
    }

    /**
     * @return the value displayed in the document title input when there's no value set by the user
     */
    public String getDocumentTitlePlaceholder()
    {
        return getDriver().findElement(By.id("document-title-input")).getAttribute("placeholder");
    }

    /**
     * Sets the value of the version summary field.
     * 
     * @param versionSummary the new version summary
     * @return this page object
     */
    public InplaceEditablePage setVersionSummary(String versionSummary)
    {
        this.versionSummaryInput.clear();
        this.versionSummaryInput.sendKeys(versionSummary);
        return this;
    }

    /**
     * Clicks on the Cancel button and waits for the page to be rendered in view mode.
     * 
     * @return this page object
     */
    public InplaceEditablePage cancel()
    {
        getDriver().findElement(By.cssSelector("input[name='action_cancel']")).click();
        return waitForView();
    }

    /**
     * Clicks on the Save button and waits for the save confirmation message.
     * 
     * @return this page object
     */
    public InplaceEditablePage save()
    {
        return save(true);
    }

    /**
     * Clicks on the Save button and maybe waits for the save confirmation message.
     * 
     * @param wait whether to wait or not for the save success confirmation
     * @return this page object
     */
    public InplaceEditablePage save(boolean wait)
    {
        return save(wait ? "Saved" : null);
    }

    /**
     * Clicks on the Save button and waits for the specified save confirmation message.
     * 
     * @param expectedSuccessMessage the save success confirmation to wait for
     * @return this page object
     * @since 13.2
     * @since 12.10.6
     */
    public InplaceEditablePage save(String expectedSuccessMessage)
    {
        getDriver().findElement(By.cssSelector("input[name='action_saveandcontinue']")).click();
        if (expectedSuccessMessage != null) {
            waitForNotificationSuccessMessage(expectedSuccessMessage);
        }
        return this;
    }

    /**
     * Clicks on the Save and View button and waits for the page to be rendered in view mode.
     * 
     * @return this page object
     */
    public InplaceEditablePage saveAndView()
    {
        return saveAndView(true);
    }

    /**
     * Clicks on the Save and View button and maybe waits for the page to be rendered in view mode.
     * 
     * @param wait whether to wait or not for the save success confirmation
     * @return this page object
     */
    public InplaceEditablePage saveAndView(boolean wait)
    {
        return saveAndView(wait ? "Saved" : null);
    }

    /**
     * Clicks on the Save and View button and waits for the specified save confirmation message and for the page to be
     * rendered in view mode.
     * 
     * @param expectedSuccessMessage the save success confirmation to wait for
     * @return this page object
     * @since 13.2
     * @since 12.10.6
     */
    public InplaceEditablePage saveAndView(String expectedSuccessMessage)
    {
        getDriver().findElement(By.cssSelector("input[name='action_save']")).click();
        if (expectedSuccessMessage != null) {
            waitForNotificationSuccessMessage(expectedSuccessMessage);
            return waitForView();
        }
        return this;
    }

    /**
     * Waits for the page to be rendered in view mode, usually after a Save &amp; View or Cancel operation.
     * 
     * @return this page object
     */
    public InplaceEditablePage waitForView()
    {
        getDriver().waitUntilElementIsVisible(By.cssSelector(".xcontent:not(.form)"));
        return this;
    }
}
