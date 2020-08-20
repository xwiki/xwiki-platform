package org.xwiki.like.test.po;

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

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page Object for the Modal to interact with Likes.
 *
 * @version $Id$
 * @since 12.7RC1
 */
public class LikeModal extends BaseElement
{
    private static final String MODAL_ID = "like-modal";
    private static final String UNLIKE_BUTTON_ID = "modal-unlike-button";

    /**
     * @return {@code true} if the modal is displayed
     */
    public boolean isDisplayed()
    {
        try {
            WebElement webElement = this.getDriver().findElementWithoutWaiting(By.id(MODAL_ID));
            return webElement.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if a button is displayed to perform Unlike action.
     */
    public boolean isUnlikeButtonDisplayed()
    {
        try {
            WebElement webElement = this.getDriver().findElementWithoutWaiting(By.id(UNLIKE_BUTTON_ID));
            return webElement.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Click on the Unlike button and wait for a Unlike success message.
     */
    public void clickUnlikeButton()
    {
        this.getDriver().findElementWithoutWaiting(By.id(UNLIKE_BUTTON_ID)).click();
        waitForNotificationSuccessMessage("The page has been unliked.");
    }
}
