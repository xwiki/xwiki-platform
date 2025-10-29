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
package org.xwiki.realtime.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ConfirmationModal;

/**
 * Represents the realtime edit toolbar.
 *
 * @version $Id$
 * @since 16.10.6
 * @since 17.3.0RC1
 */
public class RealtimeEditToolbar extends BaseElement
{
    private static final By ALLOW_REALTIME = By.cssSelector(".sticky-buttons .realtime-allow");

    private static final By SAVE_STATUS = By.cssSelector(".realtime-edit-toolbar realtime-status.realtime-save-status");

    private static final String VALUE = "value";

    /**
     * Waits until the user is connected to the realtime editing session.
     * 
     * @return this instance
     */
    public RealtimeEditToolbar waitUntilConnected()
    {
        getDriver().waitUntilCondition(ExpectedConditions.attributeToBe(
            By.cssSelector(".realtime-edit-toolbar realtime-status.realtime-connection-status"), VALUE, "clean"));
        return this;
    }

    /**
     * @return {@code true} if realtime editing is enabled, {@code false} otherwise (the user has left the session or
     *         has never joined)
     */
    public boolean isCollaborating()
    {
        return !getDriver().findElements(By.cssSelector(".realtime-edit-toolbar[data-user-id]")).isEmpty();
    }

    /**
     * @return {code true} if it is possible to join the realtime editing session, {@code false} otherwise
     */
    public boolean canJoinCollaboration()
    {
        List<WebElement> allowRealtime = getDriver().findElements(ALLOW_REALTIME);
        return !allowRealtime.isEmpty() && allowRealtime.get(0).isEnabled() && !allowRealtime.get(0).isSelected();
    }

    /**
     * Join the realtime editing session.
     * 
     * @return this instance
     */
    public RealtimeEditToolbar joinCollaboration()
    {
        getDriver().findElement(ALLOW_REALTIME).click();
        waitUntilConnected();
        return this;
    }

    /**
     * Leave the realtime editing session.
     * 
     * @return this instance
     */
    public RealtimeEditToolbar leaveCollaboration()
    {
        openDoneDropdown();
        ConfirmationModal confirmationModal = new ConfirmationModal(By.id("realtime-leave-modal"));
        getDriver().findElement(By.cssSelector(".realtime-edit-toolbar .realtime-action-leave")).click();
        // Confirm leaving the realtime editing session.
        confirmationModal.clickOk();
        getDriver()
            .waitUntilCondition(ExpectedConditions.and(ExpectedConditions.visibilityOfElementLocated(ALLOW_REALTIME),
                ExpectedConditions.elementSelectionStateToBe(ALLOW_REALTIME, false)));
        return this;
    }

    private RealtimeEditToolbar openDoneDropdown()
    {
        getDriver().findElement(By.cssSelector(".realtime-edit-toolbar .realtime-action-done + .dropdown-toggle"))
            .click();
        return this;
    }

    /**
     * Click on the "Done" button to leave the editing session and go to view mode.
     */
    public void clickDone()
    {
        getDriver().findElement(By.cssSelector(".realtime-edit-toolbar .realtime-action-done")).click();
    }

    /**
     * Click on the "Summarize and done" button to open the modal for summarizing the changes.
     * @return the modal to summarize.
     */
    public SummaryModal clickSummarizeAndDone()
    {
        openDoneDropdown();
        getDriver().findElement(By.cssSelector(".realtime-edit-toolbar .realtime-action-summarize")).click();
        return new SummaryModal();
    }

    /**
     * @return the user identifier in the realtime session
     */
    public String getUserId()
    {
        return getDriver().findElement(By.className("realtime-edit-toolbar")).getDomAttribute("data-user-id");
    }

    /**
     * @return the list of coeditors listed directly on the toolbar
     */
    public List<CoeditorElement> getVisibleCoeditors()
    {
        return getDriver().findElements(By.cssSelector(".realtime-edit-toolbar .realtime-users .realtime-user"))
            .stream().map(CoeditorElement::new).toList();
    }

    /**
     * Wait for the specified user to appear on the tool bar, either directly or in the users dropdown.
     * 
     * @param coeditorId the coeditor identifier
     * @return this instance
     */
    public CoeditorElement waitForCoeditor(String coeditorId)
    {
        By coeditorSelector = By.cssSelector(".realtime-edit-toolbar .realtime-user[data-id='" + coeditorId + "']");
        // The coeditor can be either displayed directly on the toolbar or hidden in the dropdown.
        getDriver().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(coeditorSelector));
        return new CoeditorElement(getDriver().findElement(coeditorSelector));
    }

    /**
     * @return {@code true} if the user is currently the only one editing the document (in the realtime session),
     *         {@code false} otherwise
     */
    public boolean isEditingAlone()
    {
        List<CoeditorElement> visibleCoeditors = getVisibleCoeditors();
        return visibleCoeditors.size() == 1 && visibleCoeditors.get(0).getId().equals(getUserId());
    }

    /**
     * @return the dropdown listing all the users editing the current document in realtime
     */
    public CoeditorsDropdown getCoeditorsDropdown()
    {
        return new CoeditorsDropdown();
    }

    /**
     * @return the save status of the edited document
     */
    public SaveStatus getSaveStatus()
    {
        return SaveStatus.fromValue(getDriver().findElement(SAVE_STATUS).getDomAttribute(VALUE));
    }

    /**
     * Wait for the save status to have the specified value.
     * 
     * @param status the save status to wait for
     * @return this instance
     */
    public RealtimeEditToolbar waitForSaveStatus(SaveStatus status)
    {
        // Unsaved changes are pushed after 60 seconds, but the check for unsaved changes is done every 20 seconds, so
        // we need to wait at most 100 seconds (adding an error margin of 20 seconds).
        int timeout = (int) (status == SaveStatus.SAVED ? 100
            : getDriver().manage().timeouts().getImplicitWaitTimeout().toSeconds());
        getDriver().waitUntilCondition(ExpectedConditions.attributeToBe(SAVE_STATUS, VALUE, status.getValue()),
            timeout);
        return this;
    }

    /**
     * Use the Save &amp; Continue shortcut key and wait for the document to be saved.
     */
    public void sendSaveShortcutKey()
    {
        sendSaveShortcutKey(true);
    }

    /**
     * Use the Save &amp; Continue shortcut key, optionally waiting for the document to be saved.
     * 
     * @param wait whether to wait for the document to be saved or not
     */
    public void sendSaveShortcutKey(boolean wait)
    {
        getDriver().switchTo().activeElement().sendKeys(Keys.chord(Keys.ALT, Keys.SHIFT, "s"));
        if (wait) {
            getDriver()
                .waitUntilCondition(ExpectedConditions.attributeToBe(SAVE_STATUS, VALUE, SaveStatus.SAVED.getValue()));
        }
    }

    /**
     * Wait for the warning that informs the user that they, or some other user, are editing the same page outside the
     * realtime collaboration session, which may lead to merge conflicts on save.
     *
     * @return this instance
     * @since 17.8.0
     * @since 17.4.5
     * @since 16.10.12
     */
    public RealtimeEditToolbar waitForConcurrentEditingWarning()
    {
        String toggleSelector = "button.realtime-warning[data-toggle=\"popover\"]";
        String popoverSelector = toggleSelector + " + .popover";

        // Wait for the popover to be fully displayed, because it uses a fade-in animation.
        getDriver().waitUntilElementIsVisible(By.cssSelector(popoverSelector + ".fade.in"));

        // Hide the popover because it can cover other UI elements.
        WebElement toggle = getDriver().findElementWithoutWaiting(By.cssSelector(toggleSelector));
        // We have to click twice because the popover was displayed without focusing the toggle.
        toggle.click();
        toggle.click();

        // Wait for the popover to be fully hidden, because it uses a fade-out animation.
        getDriver().waitUntilCondition(ExpectedConditions.numberOfElementsToBe(By.cssSelector(popoverSelector), 0));

        return this;
    }

    /**
     * @return the dropdown listing recent versions of the edited document, and the "Summarize Changes" action
     */
    public HistoryDropdown getHistoryDropdown()
    {
        return new HistoryDropdown();
    }
}
