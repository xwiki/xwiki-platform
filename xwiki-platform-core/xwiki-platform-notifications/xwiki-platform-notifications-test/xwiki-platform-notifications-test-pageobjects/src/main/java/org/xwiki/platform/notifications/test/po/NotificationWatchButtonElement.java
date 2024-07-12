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
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the button displaying the watch status and allowing to open the modal for the watch settings.
 *
 * @version $Id$
 * @since 16.5.0RC1
 */
public class NotificationWatchButtonElement extends BaseElement
{
    @FindBy(id = "watchButton")
    private WebElement watchButton;

    /**
     * Default constructor.
     */
    public NotificationWatchButtonElement()
    {
    }

    /**
     * Click on the button to open the modal.
     * @return a new instance of {@link NotificationsWatchModal}.
     */
    public NotificationsWatchModal openModal()
    {
        this.watchButton.click();
        return new NotificationsWatchModal();
    }

    /**
     * @return {@code true} if the status says the page is watched.
     */
    public boolean isWatched()
    {
        return watchButton.getText().equals("Followed");
    }

    /**
     * @return {@code true} if the status says the page is blocked.
     */
    public boolean isBlocked()
    {
        return watchButton.getText().equals("Blocked");
    }

    /**
     * @return {@code true} if the status says there's no watch filter set.
     */
    public boolean isNotSet()
    {
        return watchButton.getText().equals("Not set");
    }

    /**
     * @return {@code true} if the status says there's custom watch filter.
     */
    public boolean isCustom()
    {
        return watchButton.getText().equals("Custom");
    }
}
