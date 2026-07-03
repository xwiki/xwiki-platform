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
package org.xwiki.platform.notifications.test.po.preferences.filters;

import java.util.HashSet;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.test.ui.po.BaseModal;

/**
 * Represents the Add filter modal.
 *
 * @version $Id$
 * @since 13.3RC1
 */
public class CustomNotificationFilterModal extends BaseModal
{
    private static final String VALUE = "value";
    private static final String INCLUSIVE = "inclusive";
    private static final String EXCLUSIVE = "exclusive";

    /**
     * Available notification formats.
     */
    public enum NotificationFormat
    {
        /**
         * For notification sent through the notification area.
         */
        ALERT,

        /**
         * For notification sent by mail.
         */
        EMAIL
    };

    /**
     * Default constructor.
     */
    public CustomNotificationFilterModal()
    {
        super(By.id("modal-add-custom-filter-preference"));
    }

    /**
     * @return the tree allowing to select the locations.
     */
    public DocumentTreeElement getLocations()
    {
        return new DocumentTreeElement(getDriver().findElementWithoutWaiting(this.container,
            By.className("location-tree")));
    }

    /**
     * Get the action select: note that we don't rely on {@link org.xwiki.test.ui.po.Select} since it's hitting
     * Escape key when performing select which closes the modal.
     *
     * @return the actions select element.
     */
    private Select getActions()
    {
        return new Select(getDriver().findElementWithoutWaiting(this.container,
            By.id("notificationFilterTypeSelector")));
    }

    /**
     * @return the selected action for the new filter.
     */
    public CustomNotificationFilterPreference.FilterAction getSelectedAction()
    {
        Select actions = this.getActions();
        if (actions.getFirstSelectedOption().getAttribute(VALUE).equals(INCLUSIVE)) {
            return CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT;
        } else if (actions.getFirstSelectedOption().getAttribute(VALUE).equals(EXCLUSIVE)) {
            return CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT;
        } else {
            throw new IllegalArgumentException(
                String.format("Cannot guess the action associated with value [%s]",
                    actions.getFirstSelectedOption().getAttribute(VALUE)));
        }
    }

    /**
     * Select the given action for the new filter.
     * @param action the action to select.
     */
    public void selectAction(CustomNotificationFilterPreference.FilterAction action)
    {
        if (action == CustomNotificationFilterPreference.FilterAction.NOTIFY_EVENT) {
            this.getActions().selectByValue(INCLUSIVE);
        } else if (action == CustomNotificationFilterPreference.FilterAction.IGNORE_EVENT) {
            this.getActions().selectByValue(EXCLUSIVE);
        } else {
            throw new IllegalArgumentException(String.format("Unsupported action [%s]", action));
        }
    }

    /**
     * Get the select element for the event types: note that we don't rely on {@link org.xwiki.test.ui.po.Select}
     * since it's hitting Escape key when performing select which closes the modal.
     *
     * @return the select element for the event types of the filter.
     */
    public Select getEventsSelector()
    {
        return new Select(getDriver().findElementWithoutWaiting(this.container,
            By.id("notificationFilterEventTypeSelector")));
    }

    private WebElement getEmailFormat()
    {
        return getDriver().findElementWithoutWaiting(this.container,
            By.id("notificationFilterNotificationFormatSelector_email"));
    }

    private WebElement getAlertFormat()
    {
        return getDriver().findElementWithoutWaiting(this.container,
            By.id("notificationFilterNotificationFormatSelector_alert"));
    }

    /**
     * @return the selected formats for the filter.
     */
    public Set<NotificationFormat> getSelectedFormats()
    {
        Set<NotificationFormat> result = new HashSet<>();
        if (this.getEmailFormat().isSelected()) {
            result.add(NotificationFormat.EMAIL);
        }
        if (this.getAlertFormat().isSelected()) {
            result.add(NotificationFormat.ALERT);
        }
        return result;
    }

    /**
     * Allow to select the formats for the notification.
     * @param formats the formats to select.
     */
    public void selectFormats(Set<NotificationFormat> formats)
    {
        WebElement emailFormat = this.getEmailFormat();
        WebElement alertFormat = this.getAlertFormat();
        if (alertFormat.isSelected()) {
            alertFormat.click();
        }
        if (emailFormat.isSelected()) {
            emailFormat.click();
        }
        for (NotificationFormat format : formats) {
            if (format == NotificationFormat.ALERT) {
                alertFormat.click();
            } else if (format == NotificationFormat.EMAIL) {
                emailFormat.click();
            } else {
                throw new IllegalArgumentException(String.format("Unsupported format [%s]", format));
            }
        }
    }

    /**
     * Close the modal by cancelling it and wait until the modal is not displayed anymore.
     */
    public void clickCancel()
    {
        getDriver().findElementWithoutWaiting(this.container, By.className("btn-default")).click();
        this.waitForClosed();
    }

    private WebElement getSubmitButton()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.className("btn-primary"));
    }

    /**
     * @return {@code true} if the submit button is enabled.
     */
    public boolean isSubmitEnabled()
    {
        return this.getSubmitButton().isEnabled();
    }

    /**
     * Click on the Submit button to add the new filter and wait until the modal is closed, and the Live Data of custom
     * filter preferences has been refreshed. Be careful to call {@link #isSubmitEnabled()} before to ensure the Submit
     * button can be clicked.
     *
     * @see #clickSubmit(int) if the form leads to the creation of more than one notification filter
     */
    public void clickSubmit()
    {
        clickSubmit(1);
    }

    /**
     * Click on the Submit button to add new filters and wait until the modal is closed, and the Live Data of custom
     * filter preferences to be refreshed. Be careful to call {@link #isSubmitEnabled()} before to ensure the Submit
     * button can be clicked.
     *
     * @param offset the expected number of additional notification filters created
     */
    public void clickSubmit(int offset)
    {
        LiveDataElement liveDataElement =
            new LiveDataElement("notificationCustomFilterPreferencesLiveData");
        TableLayoutElement tableLayout = liveDataElement.getTableLayout();
        int rowCount = tableLayout.countRows();
        getSubmitButton().click();
        waitForClosed();

        tableLayout.waitUntilRowCountEqualsTo(rowCount + offset);
    }
}
