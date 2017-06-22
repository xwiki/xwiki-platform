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
package org.xwiki.platform.notifications.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the user profile's Notifications tab.
 * 
 * @version $Id$
 * @since 9.4RC1
 */
public class NotificationsUserProfilePage extends ViewPage
{
    private static final String SAVED_NOTIFICATION_TEXT = "Saved!";

    @FindBy(id = "notificationsPane")
    private WebElement notificationsPane;

    @FindBy(css = "td.notificationTypeCell[data-eventtype='create'][data-format='alert'] .bootstrap-switch")
    private WebElement pageCreatedSwitch;

    @FindBy(css = "td.notificationTypeCell[data-eventtype='delete'][data-format='alert'] .bootstrap-switch")
    private WebElement pageDeletedSwitch;

    @FindBy(css = "td.notificationTypeCell[data-eventtype='update'][data-format='alert'] .bootstrap-switch")
    private WebElement pageUpdatedSwitch;

    @FindBy(css = "td.notificationTypeCell[data-eventtype='addComment'][data-format='alert'] .bootstrap-switch")
    private WebElement pageCommentedSwitch;

    /**
     * Construct a NotificationsUserProfilePage (and for the browser page to be fully loaded).
     */
    public NotificationsUserProfilePage()
    {
        getDriver().waitUntilElementIsVisible(notificationsPane, By.className("bootstrap-switch"));
    }

    /**
     * @param username the user profile document name
     * @return the notifications profile tab page object
     */
    public static NotificationsUserProfilePage gotoPage(String username)
    {
        getUtil().gotoPage("XWiki", username, "view", "category=notifications");
        return new NotificationsUserProfilePage();
    }

    /**
     * Check if the pageCreatedCheckbox is checked.
     * 
     * @return true if the checkbox is checked
     */
    public boolean isPageCreated()
    {
        return isSwitchOn(this.pageCreatedSwitch);
    }

    /**
     * Check if the pageDeletedCheckbox is checked.
     * 
     * @return true if the checkbox is checked
     */
    public boolean isPageDeleted()
    {
        return isSwitchOn(pageDeletedSwitch);
    }

    /**
     * Check if the pageUpdatedCheckbox is checked.
     * 
     * @return true if the checkbox is checked
     */
    public boolean isPageUpdated()
    {
        return isSwitchOn(pageUpdatedSwitch);
    }

    /**
     * Check if the pageCommentedCheckbox is checked.
     *
     * @return true if the checkbox is checked
     * @since 9.5
     * @since 9.6RC1
     */
    public boolean isPageCommented()
    {
        return isSwitchOn(pageCommentedSwitch);
    }

    /**
     * Change the status of the pageCreatedCheckbox.
     * 
     * @param status New status
     */
    public void setPageCreated(boolean status)
    {
        if (status != this.isPageCreated()) {
            clickOnSwitch(this.pageCreatedSwitch);
            this.waitForNotificationSuccessMessage(SAVED_NOTIFICATION_TEXT);
        }
    }

    /**
     * Change the status of the pageDeletedCheckbox.
     * 
     * @param status New status
     */
    public void setPageDeleted(boolean status)
    {
        if (status != this.isPageDeleted()) {
            clickOnSwitch(this.pageDeletedSwitch);
            this.waitForNotificationSuccessMessage(SAVED_NOTIFICATION_TEXT);
        }
    }

    /**
     * Change the status of the pageUpdatedCheckbox.
     * 
     * @param status New status
     */
    public void setPageUpdated(boolean status)
    {
        if (status != this.isPageUpdated()) {
            clickOnSwitch(this.pageUpdatedSwitch);
            this.waitForNotificationSuccessMessage(SAVED_NOTIFICATION_TEXT);
        }
    }

    /**
     * Change the status of the pageCommentCheckbox.
     *
     * @param status New status
     * @since 9.5
     * @since 9.6RC1
     */
    public void setPageCommented(boolean status)
    {
        if (status != this.isPageCommented()) {
            clickOnSwitch(this.pageCommentedSwitch);
            this.waitForNotificationSuccessMessage(SAVED_NOTIFICATION_TEXT);
        }
    }

    /**
     * Disable every notification parameters.
     */
    public void disableAllParameters()
    {
        this.setPageCreated(false);
        this.setPageDeleted(false);
        this.setPageUpdated(false);
    }

    private boolean isSwitchOn(WebElement webElement)
    {
        // TODO: create a generic widget object
        return webElement.getAttribute("class").contains("bootstrap-switch-on");
    }

    private void clickOnSwitch(WebElement webElement)
    {
        // TODO: create a generic widget object
        webElement.findElement(By.className("bootstrap-switch-label")).click();
    }
}
