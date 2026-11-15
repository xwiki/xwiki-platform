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
    private static final By TOOLBAR = By.className("realtime-edit-toolbar");

    private static final By ALLOW_REALTIME = By.cssSelector(".sticky-buttons .realtime-allow");

    private static final By SAVE_STATUS = By.cssSelector(".realtime-edit-toolbar .realtime-save-status");

    private static final By SUMMARIZE_AND_DONE_ACTION =
        By.cssSelector(".realtime-edit-toolbar-left .realtime-action-summarize");

    private static final By DONE_ACTION = By.cssSelector(".realtime-edit-toolbar .realtime-action-done");

    private static final String CONNECTION_STATUS_SELECTOR = ".realtime-edit-toolbar .realtime-connection-status";

    /**
     * The attribute that holds the connection and save status.
     */
    private static final String STATUS_ATTRIBUTE = "data-status";

    /**
     * The value of the connection status when connected.
     */
    private static final String CONNECTED = "clean";

    private static final By VISIBLE_COEDITORS =
        By.cssSelector(".realtime-edit-toolbar .realtime-users .realtime-user");

    /**
     * Waits until the user is connected to the realtime editing session.
     * 
     * @return this instance
     */
    public RealtimeEditToolbar waitUntilConnected()
    {
        getDriver().waitUntilCondition(
            ExpectedConditions.attributeToBe(By.cssSelector(CONNECTION_STATUS_SELECTOR), STATUS_ATTRIBUTE, CONNECTED));
        return this;
    }

    /**
     * Waits until the toolbar is displayed, without waiting for the user to be connected to the realtime editing
     * session.
     *
     * @return this instance
     * @since 18.9.0
     */
    public RealtimeEditToolbar waitUntilDisplayed()
    {
        getDriver().waitUntilElementIsVisible(TOOLBAR);
        return this;
    }

    /**
     * @return {@code true} if the user is connected to the realtime editing session, {@code false} if the user is
     *         disconnected or the connection is being established
     * @since 18.9.0
     */
    public boolean isConnected()
    {
        return CONNECTED.equals(
            getDriver().findElement(By.cssSelector(CONNECTION_STATUS_SELECTOR)).getDomAttribute(STATUS_ATTRIBUTE));
    }

    /**
     * Simulates a temporary loss of the network connection, which closes the WebSocket connection used by the realtime
     * editing session, and waits for the editor to reconnect. The browser stays online, so the connection is restored
     * right away.
     * <p>
     * Applies only to the editors using ChainPad / Netflux for realtime synchronization (CKEditor, wiki editor).
     *
     * @return this instance
     * @since 18.9.0RC1
     */
    public RealtimeEditToolbar simulateConnectionLoss()
    {
        return simulateConnectionLoss(true);
    }

    /**
     * Simulates a temporary loss of the network connection, which closes the WebSocket connection used by the realtime
     * editing session. The browser stays online, so the editor tries to reconnect right away.
     * <p>
     * Applies only to the editors using ChainPad / Netflux for realtime synchronization (CKEditor, wiki editor).
     *
     * @param waitForReconnect whether to wait for the editor to reconnect; pass {@code false} when the reconnection is
     *            expected to fail
     * @return this instance
     * @since 18.9.0RC1
     */
    public RealtimeEditToolbar simulateConnectionLoss(boolean waitForReconnect)
    {
        waitUntilConnected();
        // The realtime WebSocket client closes the connection when the browser goes offline (unless XWiki is accessed
        // from localhost) and the editor shows that it's disconnected right away. The toolbar is updated
        // asynchronously, but before any reconnection, so we read the connection status after the pending updates.
        String connectionStatus = (String) getDriver().executeAsyncScript(
            "const [selector, statusAttribute, callback] = arguments;"
                + "window.dispatchEvent(new Event('offline'));"
                + "Promise.resolve().then(() => {"
                + "  callback(document.querySelector(selector).getAttribute(statusAttribute));"
                + "});",
            CONNECTION_STATUS_SELECTOR, STATUS_ATTRIBUTE);
        if (CONNECTED.equals(connectionStatus)) {
            throw new IllegalStateException("The realtime editor didn't notice the connection loss. "
                + "Note that the connection loss can't be simulated when XWiki is accessed from localhost.");
        }
        return waitForReconnect ? waitUntilConnected() : this;
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
     * Applies only to the editors using ChainPad / Netflux for realtime synchronization (CKEditor, wiki editor).
     *
     * @return {code true} if it is possible to join the realtime editing session, {@code false} otherwise
     */
    public boolean canJoinCollaboration()
    {
        List<WebElement> allowRealtime = getDriver().findElements(ALLOW_REALTIME);
        return !allowRealtime.isEmpty() && allowRealtime.get(0).isEnabled() && !allowRealtime.get(0).isSelected();
    }

    /**
     * Join the realtime editing session.
     * <p>
     * Applies only to the editors using ChainPad / Netflux for realtime synchronization (CKEditor, wiki editor).
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
     * <p>
     * Applies only to the editors using ChainPad / Netflux for realtime synchronization (CKEditor, wiki editor).
     *
     * @return this instance
     */
    public RealtimeEditToolbar leaveCollaboration()
    {
        openDoneDropdown();
        By leaveModal = By.id("realtime-leave-modal");
        // Remove the fade effect.
        new ConfirmationModal(leaveModal);
        getDriver().findElement(By.cssSelector(".realtime-edit-toolbar .realtime-action-leave")).click();
        // Confirm leaving the realtime editing session. The modal is removed along with the realtime toolbar, once
        // closed, so we can't rely on ConfirmationModal#clickOk() which waits for the modal element to be hidden.
        getDriver().findElement(By.cssSelector("#realtime-leave-modal .modal-footer .btn-primary")).click();
        getDriver().waitUntilElementDisappears(leaveModal);
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
        getDriver().findElement(DONE_ACTION).click();
    }

    /**
     * Click on the "Summarize and done" button to open the modal for summarizing the changes.
     * 
     * @return the modal to summarize.
     */
    public SummaryModal clickSummarizeAndDone()
    {
        openDoneDropdown();
        getDriver().findElement(SUMMARIZE_AND_DONE_ACTION).click();
        return new SummaryModal();
    }

    /**
     * @return {@code true} if the "Summarize &amp; Done" action is available, {@code false} otherwise
     * @since 18.1.0RC1
     * @since 17.10.4
     * @since 17.4.9
     * @since 16.10.17
     */
    public boolean hasSummarizeAndDoneAction()
    {
        return !getDriver().findElementsWithoutWaiting(SUMMARIZE_AND_DONE_ACTION).isEmpty();
    }

    /**
     * @return the user identifier in the realtime session
     */
    public String getUserId()
    {
        return getDriver().findElement(TOOLBAR).getDomAttribute("data-user-id");
    }

    /**
     * @return the number of coeditors listed directly on the toolbar
     * @since 18.7.0RC1
     * @since 18.4.4
     * @since 17.10.12
     * @since 16.10.19
     */
    public int getVisibleCoeditorCount()
    {
        return getDriver().findElements(VISIBLE_COEDITORS).size();
    }

    /**
     * @return the identifiers of the coeditors listed directly on the toolbar, in the order they are displayed
     * @since 18.7.0RC1
     * @since 18.4.4
     * @since 17.10.12
     * @since 16.10.19
     */
    public List<String> getVisibleCoeditorIds()
    {
        return CoeditorElement.readCoeditors(getDriver(), VISIBLE_COEDITORS, CoeditorElement::getId);
    }

    /**
     * @return the avatar hints of the coeditors listed on the toolbar, in the order they are displayed
     * @since 18.6.0RC1
     * @since 18.4.3
     * @since 17.10.11
     * @since 16.10.19
     */
    public List<String> getVisibleCoeditorAvatarHints()
    {
        return CoeditorElement.readCoeditors(getDriver(), VISIBLE_COEDITORS, CoeditorElement::getAvatarHint);
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
        getDriver().waitUntilCondition(driver -> getDriver().findElementWithoutScrolling(coeditorSelector));
        return new CoeditorElement(() -> getDriver().findElementWithoutScrolling(coeditorSelector));
    }

    /**
     * @return {@code true} if the user is currently the only one editing the document (in the realtime session),
     *         {@code false} otherwise
     */
    public boolean isEditingAlone()
    {
        List<String> visibleCoeditorIds = getVisibleCoeditorIds();
        return visibleCoeditorIds.size() == 1 && visibleCoeditorIds.get(0).equals(getUserId());
    }

    /**
     * Waits until the current user is the only one editing the document in realtime, e.g. after the other coeditors
     * left the realtime editing session.
     *
     * @return this instance
     * @since 18.9.0RC1
     */
    public RealtimeEditToolbar waitUntilEditingAlone()
    {
        getDriver().waitUntilCondition(driver -> isEditingAlone());
        return this;
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
        return SaveStatus.fromValue(getDriver().findElement(SAVE_STATUS).getDomAttribute(STATUS_ATTRIBUTE));
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
        getDriver().waitUntilCondition(
            ExpectedConditions.attributeToBe(SAVE_STATUS, STATUS_ATTRIBUTE, status.getValue()), timeout);
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
            getDriver().waitUntilCondition(ExpectedConditions.and(
                // Wait for the content to be saved.
                ExpectedConditions.attributeToBe(SAVE_STATUS, STATUS_ATTRIBUTE, SaveStatus.SAVED.getValue()),
                // Wait for the edit form to be re-enabled.
                ExpectedConditions.elementToBeClickable(DONE_ACTION)));
        }
    }

    /**
     * Wait for the warning that informs the user that they, or some other user, are editing the same page outside the
     * realtime collaboration session, which may lead to merge conflicts on save.
     * <p>
     * Applies only to the editors using ChainPad / Netflux for realtime synchronization (CKEditor, wiki editor).
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
