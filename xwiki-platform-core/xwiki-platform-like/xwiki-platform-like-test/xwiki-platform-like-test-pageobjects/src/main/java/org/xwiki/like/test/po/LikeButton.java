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
package org.xwiki.like.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page Object to manipulate a Like button.
 *
 * @version $Id$
 * @since 12.7RC1
 */
public class LikeButton extends BaseElement
{
    private static final String LIKE_BUTTON_CLASS = "like-button";
    private static final String LIKE_NUMBER_CLASS = "like-number";

    private WebElement getButton()
    {
        return this.getDriver().findElementWithoutWaiting(By.className(LIKE_BUTTON_CLASS));
    }

    /**
     * @return {@code true} if the Like button is displayed.
     */
    public boolean isDisplayed()
    {
        try {
            WebElement webElement = getButton();
            return webElement.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @return {@code true} if the button is not disabled.
     */
    public boolean canBeClicked()
    {
        WebElement button = getButton();
        return button.isEnabled();
    }

    /**
     * @return the number of likes displayed in the button.
     */
    public int getLikeNumber()
    {
        WebElement button = getButton();
        WebElement numberElement =
            getDriver().findElementWithoutWaiting(button, By.className(LIKE_NUMBER_CLASS));
        return Integer.parseInt(numberElement.getText());
    }

    /**
     * Click on the Like button and wait for a Like success message.
     */
    public void clickToLike()
    {
        getButton().click();
        waitForNotificationSuccessMessage("The page has been liked.");
    }

    /**
     * Click on the Like button and wait for the unlike success message.
     */
    public void clickToUnlike()
    {
        getButton().click();
        waitForNotificationSuccessMessage("The page has been unliked.");
    }

}
