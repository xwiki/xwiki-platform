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
 * Class that holds the types of Rights and the types of States of a check-box
 * 
 * @version $Id$
 * @since 3.2M3
 */

public class EditRightsPane extends BaseElement
{
    /** The known access rights. */
    public static enum Right
    {
        VIEW,
        COMMENT,
        EDIT,
        DELETE,
        ADMIN,
        REGISTER,
        PROGRAM;

        int getColumnIndex()
        {
            return this.ordinal() + 2;
        }
    }

    /** The possible states of an access right box. */
    public static enum State
    {
        NONE("/xwiki/resources/js/xwiki/usersandgroups/img/none.png"),
        ALLOW("/xwiki/resources/js/xwiki/usersandgroups/img/allow.png"),
        DENY("/xwiki/resources/js/xwiki/usersandgroups/img/deny1.png");

        String imageURL;

        State(String imageURL)
        {
            this.imageURL = imageURL;
        }

        State getNextState()
        {
            return values()[(ordinal() + 1) % values().length];
        }

        static State getButtonState(WebElement button)
        {
            for (State s : values()) {
                if ((button.getAttribute("src").endsWith(s.imageURL))) {
                    return s;
                }
            }
            return NONE;
        }
    }

    private LiveTableElement rightsTable;

    @FindBy(id = "uorgu")
    private WebElement showUsersField;

    @FindBy(id = "uorgg")
    private WebElement showGroupsField;

    public void switchToUsers()
    {
        this.showUsersField.click();
        getRightsTable().waitUntilReady();
    }

    public void switchToGroups()
    {
        this.showGroupsField.click();
        getRightsTable().waitUntilReady();
    }

    public State getGuestRight(Right right)
    {
        final By iconLocator = By.xpath("//tr[@id='unregistered']/td[" + right.getColumnIndex() + "]/img");
        final WebElement icon = getDriver().findElement(iconLocator);
        return State.getButtonState(icon);
    }

    public State getRight(String entityName, Right right)
    {
        final By iconLocator =
            By.xpath("//*[@id='usersandgroupstable-display']//td[@class='username']/a[contains(@href, '" + entityName
                + "')]/../../td[" + right.getColumnIndex() + "]/img");
        final WebElement icon = getDriver().findElement(iconLocator);
        return State.getButtonState(icon);
    }

    public void clickGuestRight(Right right)
    {
        try {
            getDriver().executeJavascript(
                "window.__oldConfirm = window.confirm; window.confirm = function() { return true; };");
            final By buttonLocator = By.xpath("*//tr[@id='unregistered']/td[" + right.getColumnIndex() + "]/img");
            final WebElement button = getDriver().findElement(buttonLocator);
            State currentState = State.getButtonState(button);
            button.click();
            // Note: Selenium 2.0a4 returns a relative URL when calling getAttribute("src") but since we moved to
            // Selenium 2.0a7 it returns a *full* URL even though the DOM has a relative URL as in:
            // <img src="/xwiki/resources/js/xwiki/usersandgroups/img/allow.png">
            getDriver().waitUntilElementEndsWithAttributeValue(buttonLocator, "src",
                currentState.getNextState().imageURL);
        } finally {
            getDriver().executeJavascript("window.confirm = window.__oldConfirm;");
        }
    }

    /**
     * Click once on a right button, waiting for the next state to appear.
     * 
     * @param entityName the target user or group name
     * @param right the target right
     */
    public void clickRight(String entityName, Right right)
    {
        try {
            getDriver().executeJavascript(
                "window.__oldConfirm = window.confirm; window.confirm = function() { return true; };");
            final By buttonLocator =
                By.xpath("//*[@id='usersandgroupstable-display']//td[@class='username']/a[contains(@href, '"
                    + entityName + "')]/../../td[" + right.getColumnIndex() + "]/img");
            final WebElement button = getDriver().findElement(buttonLocator);
            State currentState = State.getButtonState(button).getNextState();
            button.click();
            // Note: Selenium 2.0a4 returns a relative URL when calling getAttribute("src") but since we moved to
            // Selenium 2.0a7 it returns a *full* URL even though the DOM has a relative URL as in:
            // <img src="/xwiki/resources/js/xwiki/usersandgroups/img/allow.png">
            getDriver().waitUntilElementEndsWithAttributeValue(buttonLocator, "src", currentState.imageURL);
        } finally {
            getDriver().executeJavascript("window.confirm = window.__oldConfirm;");
        }
    }

    /**
     * Click on a right button until it gets in the wanted state.
     * 
     * @param entityName the target user or group name
     * @param right the target right
     * @param wantedState the wanted state for the right
     */
    public void setRight(String entityName, Right right, State wantedState)
    {
        while (getRight(entityName, right) != wantedState) {
            clickRight(entityName, right);
        }
    }

    /**
     * Click on a right button until it gets in the wanted state.
     * 
     * @param right the target right
     * @param wantedState the wanted state for the right
     */
    public void setGuestRight(Right right, State wantedState)
    {
        while (getGuestRight(right) != wantedState) {
            clickGuestRight(right);
        }
    }

    public String getURL(String space, String page)
    {
        return getUtil().getURL(space, page, "edit", "editor=rights");
    }

    public LiveTableElement getRightsTable()
    {
        if (this.rightsTable == null) {
            this.rightsTable = new LiveTableElement("usersandgroupstable");
        }
        return this.rightsTable;
    }
}
