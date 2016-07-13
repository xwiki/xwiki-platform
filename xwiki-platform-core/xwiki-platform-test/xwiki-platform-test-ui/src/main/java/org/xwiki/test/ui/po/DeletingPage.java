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
 * Represents the page displayed while the page (and maybe its children) is deleted.
 *  
 * @version $Id$
 * @since 7.2M3 
 */
public class DeletingPage extends ViewPage
{
    private static final String SUCCESS_MESSAGE_ID = "successMessage";
    
    private static final String ERROR_MESSAGE_ID = "errorMessage";
    
    private static final String PROGRESS_BAR_CONTAINER_ID = "delete-progress-bar-container";
    
    @FindBy(id = SUCCESS_MESSAGE_ID)
    private WebElement successMessage;

    @FindBy(id = ERROR_MESSAGE_ID)
    private WebElement errorMessage;

    @FindBy(id = PROGRESS_BAR_CONTAINER_ID)
    private WebElement progressBarContainer;
    
    @FindBy(xpath = "//div[@id = 'document-title']//a")
    private WebElement titleLink;
    
    /** 
     * @return true if the deletion process is terminated
     */
    public boolean isTerminated()
    {
        return !progressBarContainer.isDisplayed();
    }

    /**
     * Wait until the delete process is terminated 
     */
    public void waitUntilIsTerminated()
    {
        getDriver().waitUntilElementDisappears(By.id(PROGRESS_BAR_CONTAINER_ID));
    }
    
    public boolean isSuccess()
    {
        return successMessage.isDisplayed();
    }

    /**
     * Wait until the delete process is terminated
     * @since 8.1M1
     */
    public void waitUntilSuccessMessage()
    {
        getDriver().waitUntilElementIsVisible(By.id(SUCCESS_MESSAGE_ID));
    }

    public String getSuccessMessage()
    {
        return successMessage.getText();
    }
    
    public DeletePageOutcomePage getDeletePageOutcomePage()
    {
        // Click on the title to get back to the "view" mode
        titleLink.click();
        // Since the page is deleted, we should have the "delete page outcome" UI...
        return new DeletePageOutcomePage();
    }
}
