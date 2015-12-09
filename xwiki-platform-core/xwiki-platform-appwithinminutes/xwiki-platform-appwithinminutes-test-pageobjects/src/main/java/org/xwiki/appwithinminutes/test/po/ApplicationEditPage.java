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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents some custom actions available on application pages in inline edit mode, since the default save&view and
 * save&continue buttons are overridden in AWM.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class ApplicationEditPage extends InlinePage
{
    /**
     * The form used to edit the application class overwrites the save button because it needs to process the submitted
     * data. Otherwise the request is forwarded by the action filter to the save action.
     */
    @FindBy(name = "xaction_save")
    private WebElement saveButton;

    /**
     * @see #saveButton
     */
    @FindBy(name = "xaction_saveandcontinue")
    private WebElement saveAndContinueButton;

    @Override
    public <T extends ViewPage> T clickSaveAndView()
    {
        saveButton.click();

        if (!getUtil().isInViewMode()) {
            // Since we might have a loading step between clicking Save&View and the view page actually loading
            // (specifically when using templates that have child documents associated), we need to wait for the save to
            // finish and for the redirect to view mode to occur.
            getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver input)
                {
                    boolean inViewMode = getUtil().isInViewMode();
                    // Hack: It may happen in edit mode that the page loads and the first click() action does
                    // absolutely nothing. Most likely a race condition with the actionbuttons javascript. To work
                    // around it, we can simply click again on the save button.
                    if (!inViewMode && getDriver().hasElementWithoutWaiting(By.name("action_save"))
                        && saveButton.isEnabled()) {
                        saveButton.click();
                    }

                    return inViewMode;
                }
            });
        }

        return createViewPage();
    }

    @Override
    public void clickSaveAndContinue()
    {
        clickSaveAndContinue(true);
    }

    /**
     * Clicks on the Save & Continue button. Use this instead of {@link #clickSaveAndContinue()} when you want to wait
     * for a different message (e.g. an error message).
     *
     * @param wait {@code true} to wait for the page to be saved, {@code false} otherwise
     */
    public void clickSaveAndContinue(boolean wait)
    {
        saveAndContinueButton.click();

        if (wait) {
            // Wait until the page is really saved.
            waitForNotificationSuccessMessage("Saved");
        }
    }

    /**
     * Use this method instead of {@link #clickSaveAndView()} and call {@link WebElement#click()} when you know that the
     * next page is not a standard XWiki {@link ViewPage}.
     *
     * @return the save and view button used to submit the form.
     */
    public WebElement getSaveAndViewButton()
    {
        return saveButton;
    }
}
