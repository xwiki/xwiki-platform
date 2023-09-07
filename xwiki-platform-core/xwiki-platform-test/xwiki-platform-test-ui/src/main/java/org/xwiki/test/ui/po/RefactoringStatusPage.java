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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the status page of a refactoring job.
 *
 * @version $Id$
 * @since 15.8RC1
 */
public class RefactoringStatusPage extends BasePage
{
    private static final String MESSAGE_CSS_SELECTOR =
        ".xcontent.job-status .box.successmessage, .xcontent.job-status .box.errormessage";

    @FindBy(css = MESSAGE_CSS_SELECTOR)
    private WebElement message;

    /**
     * Waits until the operation finishes.
     *
     * @return this page
     */
    public RefactoringStatusPage waitUntilFinished()
    {
        // Waits for a success or error message to be displayed before continuing. Previously, this method was waiting
        // for the job progress bar to be "missing" before continuing, which could happen too early, before the bar was
        // even displayed once.
        getDriver().waitUntilElementIsVisible(By.cssSelector(MESSAGE_CSS_SELECTOR));
        return this;
    }

    /**
     * @return the status message displayed on the page after the job finished
     */
    public String getInfoMessage()
    {
        return this.message.getText();
    }
}
