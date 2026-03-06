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
package org.xwiki.realtime.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Modal displayed when clicking on "Summarize and Done" button in realtime edition.
 *
 * @version $Id$
 */
public class SummaryModal extends BaseModal
{
    /**
     * Default constructor.
     */
    public SummaryModal()
    {
        super(By.id("realtime-changeSummaryModal"));
        getDriver().waitUntilCondition(driver -> this.isDisplayed());
    }

    /**
     * Fill the summary textarea with the given content.
     * 
     * @param summary the text to put in the summary textarea
     */
    public void setSummary(String summary)
    {
        WebElement textarea = getSummaryTextArea();
        textarea.clear();
        textarea.sendKeys(summary);
    }

    /**
     * @return the content of the summary textarea
     * @since 18.1.0RC1
     * @since 17.10.4
     * @since 17.4.9
     * @since 16.10.17
     */
    public String getSummary()
    {
        return getSummaryTextArea().getAttribute("value");
    }

    /**
     * @return the summary textarea
     * @since 18.1.0RC1
     * @since 17.10.4
     * @since 17.4.9
     * @since 16.10.17
     */
    public WebElement getSummaryTextArea()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.id("realtime-changeSummaryModal-summary"));
    }

    /**
     * @return whether the value of the summary textarea is valid
     * @since 18.1.0RC1
     * @since 17.10.4
     * @since 17.4.9
     * @since 16.10.17
     */
    public boolean isSummaryValid()
    {
        return getDriver().isValid(getSummaryTextArea());
    }

    /**
     * Wait until the summary textarea is focused.
     * 
     * @since 18.1.0RC1
     * @since 17.10.4
     * @since 17.4.9
     * @since 16.10.17
     * @return this instance
     */
    public SummaryModal waitUntilSummaryIsFocused()
    {
        WebElement textarea = getSummaryTextArea();
        getDriver().waitUntilCondition(driver -> textarea.equals(driver.switchTo().activeElement()));
        return this;
    }

    /**
     * Check or uncheck the minor change checkbox.
     */
    public void toggleMinorEdit()
    {
        getDriver()
            .findElementWithoutWaiting(this.container, By.cssSelector("input[type='checkbox'][name='minorChange']"))
            .click();
    }

    /**
     * Click on the save button and eventually wait for the saved success message.
     * 
     * @param waitSuccess if {@code true} wait for the "Saved" success message
     */
    public void clickSave(boolean waitSuccess)
    {
        boolean continueEditing = isContinueEditing();
        boolean editingInplace = isEditingInplace();
        if (waitSuccess && !continueEditing && !editingInplace) {
            getDriver().addPageNotYetReloadedMarker();
        }
        getDriver().findElementWithoutWaiting(this.container, By.cssSelector(".btn-primary")).click();
        if (waitSuccess) {
            if (continueEditing) {
                waitForNotificationSuccessMessage("Saved");
            } else if (editingInplace) {
                new RealtimeInplaceEditablePage().waitForView();
            } else {
                getDriver().waitUntilPageIsReloaded();
                new ViewPage();
            }
        }
    }

    /**
     * @return whether the user continues editing after submitting the summary
     */
    public boolean isContinueEditing()
    {
        return "true".equals(this.container.getDomAttribute("data-continue"));
    }

    /**
     * @return whether this modal was opened from the inplace editing mode or standalone editing mode
     */
    public boolean isEditingInplace()
    {
        return !getDriver().findElementsWithoutWaiting(By.cssSelector(".xcontent.form")).isEmpty();
    }

    /**
     * Click on the "Changes" tab and wait for the changes to be fetched.
     *
     * @since 18.1.0RC1
     * @since 17.10.4
     * @since 17.4.9
     * @since 16.10.17
     */
    public void viewChanges()
    {
        clickTab("changes");
        // Wait for the changes to be fetched.
        WebElement spinner = getDriver().findElementWithoutWaiting(this.container,
            By.cssSelector("#realtime-changeSummaryModal-changesTab > realtime-spinner"));
        getDriver().waitUntilCondition(ExpectedConditions.invisibilityOf(spinner));
    }

    /**
     * Click on the "Summary" tab to input the summary of the changes.
     *
     * @since 18.1.0RC1
     * @since 17.10.4
     * @since 17.4.9
     * @since 16.10.17
     */
    public void viewSummary()
    {
        clickTab("summary");
    }

    private void clickTab(String tabId)
    {
        getDriver().findElementWithoutWaiting(this.container,
            By.cssSelector("a[aria-controls=\"realtime-changeSummaryModal-" + tabId + "Tab\"]")).click();
    }
}
