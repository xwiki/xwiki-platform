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

    private static final String BOOTSTRAP_SWITCH_LABEL = "bootstrap-switch-label";

    @FindBy(id = "notificationsPane")
    private WebElement notificationsPane;

    @FindBy(css = "div#notificationsPane tr[data-eventtype='create'] .bootstrap-switch")
    private WebElement pageCreatedSwitch;

    @FindBy(css = "div#notificationsPane tr[data-eventtype='delete'] .bootstrap-switch")
    private WebElement pageDeletedSwitch;

    @FindBy(css = "div#notificationsPane tr[data-eventtype='update'] .bootstrap-switch")
    private WebElement pageUpdatedSwitch;

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
     * Try to find a row corresponding to the notification parameter of the given type.
     *
     * @since 9.5RC1
     * @param eventType the event that has to be found
     * @return the row corresponding to this element
     */
    public WebElement findNotificationParameterRow(String eventType)
    {
        return this.getDriver().findElement(
                By.cssSelector("div#notificationsPane tr[data-eventtype='" + eventType + "']"));
    }

    /**
     * Use the given row to enable a notification preference.
     *
     * @since 9.5RC1
     * @param notificationParameterRow the concerned notification preference row
     */
    public void enablePreference(WebElement notificationParameterRow)
    {
        if (!isSwitchOn(notificationParameterRow)) {
            clickOnSwitch(notificationParameterRow);
        }

        this.waitForNotificationSuccessMessage(SAVED_NOTIFICATION_TEXT);
    }

    /**
     * Disable every standard notification parameters.
     *
     * @since 9.5RC1
     */
    public void disableAllStandardParameters()
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
        webElement.findElement(By.className(BOOTSTRAP_SWITCH_LABEL)).click();
    }
}
