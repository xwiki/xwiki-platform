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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
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
    public enum Right
    {
        VIEW,
        COMMENT,
        EDIT,
        DELETE,
        ADMIN,
        REGISTER,
        PROGRAM;

        @Override
        public String toString()
        {
            return StringUtils.capitalize(this.name().toLowerCase(Locale.ROOT));
        }
    }

    /** The possible states of an access right box. */
    public enum State
    {
        NONE("none"),
        ALLOW("yes"),
        DENY("no");

        String buttonClass;

        State(String buttonClass)
        {
            this.buttonClass = buttonClass;
        }

        State getNextState()
        {
            return values()[(ordinal() + 1) % values().length];
        }

        static State getButtonState(WebElement button)
        {
            for (State s : values()) {
                //  The URL may contain query string parameters (e.g. starting with 11.1RC1 the resource URLs can now
                //  contain a query parameter to avoid cache issue) and we don't care about that to identify the state.
                if ((button.getAttribute("class").contains(s.buttonClass))) {
                    return s;
                }
            }
            return NONE;
        }
    }

    private LiveTableElement rightsTable;

    @FindBy(css = "label[for='uorgu']")
    private WebElement switchToUsersLabel;

    @FindBy(css = "label[for='uorgg']")
    private WebElement switchToGroupsLabel;

    public void switchToUsers()
    {
        this.switchToUsersLabel.click();
        getRightsTable().waitUntilReady();
    }

    public void switchToGroups()
    {
        this.switchToGroupsLabel.click();
        getRightsTable().waitUntilReady();
    }

    public State getGuestRight(Right right)
    {
        return getGuestRight(right.toString());
    }

    /**
     * Allow to get the state of the given right for guest user.
     *
     * @param rightName the actual name of the right displayed in the header of the column.
     * @return the state for this given right.
     * @since 13.6RC1
     */
    public State getGuestRight(String rightName)
    {
        final By buttonLocator = By.xpath(String.format("//tr[@id='unregistered']/td[@data-title='%s']/button", rightName));
        final WebElement button = getDriver().findElement(buttonLocator);
        return State.getButtonState(button);
    }

    public State getRight(String entityName, Right right)
    {
        return getRight(entityName, right.toString());
    }

    /**
     * Allow to get the state of the given right for the given entityName.
     *
     * @param entityName name of the entity for which to get the right state.
     * @param rightName the actual name of the right displayed in the header of the column.
     * @return the state for this given right.
     * @since 13.6RC1
     */
    public State getRight(String entityName, String rightName)
    {
        final By buttonLocator =
            By.xpath(String.format(
                "//*[@id='usersandgroupstable-display']//tr[./td[@class='username']//a[contains(@href, '%s')]]"
                    + "/td[@data-title='%s']/button",
                entityName, rightName));
        final WebElement button = getDriver().findElement(buttonLocator);
        return State.getButtonState(button);
    }

    public boolean hasEntity(String entityName)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//*[@id='usersandgroupstable-display']//td[@class='username']//a[contains(@href, '" + entityName
            + "')]"));
    }

    public void clickGuestRight(Right right)
    {
        clickGuestRight(right.toString());
    }

    /**
     * Click once on a right button for guest user, waiting for the next state to appear.
     *
     * @param rightName the actual name of the right displayed in the header of the column.
     * @since 13.6RC1
     */
    public void clickGuestRight(String rightName)
    {
        try {
            getDriver().executeJavascript(
                "window.__oldConfirm = window.confirm; window.confirm = function() { return true; };");
            final By buttonLocator = By.xpath(
                String.format("*//tr[@id='unregistered']/td[@data-title='%s']/button", rightName));
            final WebElement button = getDriver().findElement(buttonLocator);
            State currentState = State.getButtonState(button);
            button.click();
            // Note: Selenium 2.0a4 returns a relative URL when calling getAttribute("src") but since we moved to
            // Selenium 2.0a7 it returns a *full* URL even though the DOM has a relative URL as in:
            // <img src="/xwiki/resources/js/xwiki/usersandgroups/img/allow.png">
            getDriver().waitUntilElementContainsAttributeValue(buttonLocator, "class",
                currentState.getNextState().buttonClass);
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
        clickRight(entityName, right.toString());
    }

    /**
     * Click once on a right button, waiting for the next state to appear.
     *
     * @param entityName the target user or group name
     * @param rightName the actual name of the right displayed in the header of the column.
     * @since 13.6RC1
     */
    public void clickRight(String entityName, String rightName)
    {
        try {
            getDriver().executeJavascript(
                "window.__oldConfirm = window.confirm; window.confirm = function() { return true; };");
            final By buttonLocator =
                By.xpath(
                    String.format("//*[@id='usersandgroupstable-display']//tr[./td[@class='username']"
                        + "//a[contains(@href, '%s')]]/td[@data-title='%s']/button", entityName, rightName));
            final WebElement button = getDriver().findElement(buttonLocator);
            State currentState = State.getButtonState(button).getNextState();
            button.click();
            // Note: Selenium 2.0a4 returns a relative URL when calling getAttribute("src") but since we moved to
            // Selenium 2.0a7 it returns a *full* URL even though the DOM has a relative URL as in:
            // <img src="/xwiki/resources/js/xwiki/usersandgroups/img/allow.png">
            getDriver().waitUntilElementContainsAttributeValue(buttonLocator, "class", currentState.buttonClass);
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
