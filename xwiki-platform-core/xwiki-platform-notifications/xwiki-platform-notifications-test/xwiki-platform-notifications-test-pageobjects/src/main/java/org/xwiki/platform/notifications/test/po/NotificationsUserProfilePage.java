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

    @FindBy(css = "div#notificationsPane tr[data-eventtype='create'] input")
    private WebElement pageCreatedCheckbox;

    @FindBy(css = "div#notificationsPane tr[data-eventtype='delete'] input")
    private WebElement pageDeletedCheckbox;

    @FindBy(css = "div#notificationsPane tr[data-eventtype='update'] input")
    private WebElement pageUpdatedCheckbox;

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
        return this.pageCreatedCheckbox.isSelected();
    }

    /**
     * Check if the pageDeletedCheckbox is checked.
     * 
     * @return true if the checkbox is checked
     */
    public boolean isPageDeleted()
    {
        return this.pageDeletedCheckbox.isSelected();
    }

    /**
     * Check if the pageUpdatedCheckbox is checked.
     * 
     * @return true if the checkbox is checked
     */
    public boolean isPageUpdated()
    {
        return this.pageUpdatedCheckbox.isSelected();
    }

    /**
     * Change the status of the pageCreatedCheckbox.
     * 
     * @param status New status
     */
    public void setPageCreated(boolean status)
    {
        if (status != this.isPageCreated()) {
            this.pageCreatedCheckbox.click();
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
            this.pageDeletedCheckbox.click();
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
            this.pageUpdatedCheckbox.click();
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
}
