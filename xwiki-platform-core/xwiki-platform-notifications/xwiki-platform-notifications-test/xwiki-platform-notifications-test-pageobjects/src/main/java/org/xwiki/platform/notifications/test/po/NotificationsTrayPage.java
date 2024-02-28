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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.notifications.rest.NotificationsResource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.ViewPage;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Represents the user Notifications tab.
 *
 * @version $Id$
 * @since 9.4RC1
 */
public class NotificationsTrayPage extends ViewPage
{
    private static final String CLASS = "class";

    @FindBy(css = "li#tmNotifications div.notifications-area")
    private WebElement notificationsArea;

    @FindBy(css = "li#tmNotifications button[title='Notifications']")
    private WebElement watchListButton;

    @FindBy(css = "li#tmNotifications")
    private WebElement notificationsButton;

    @FindBy(css = "span.notifications-count")
    private WebElement countBadge;

    @FindBy(className = "notifications-toggles")
    private WebElement toggles;

    @FindBy(className = "notifications-rss-link")
    private WebElement rssLink;

    private BootstrapSwitch pageOnlyWatchedSwitch;

    private BootstrapSwitch pageAndChildrenWatchedSwitch;

    private BootstrapSwitch wikiWatchedSwitch;

    /**
     * Constructor.
     */
    public NotificationsTrayPage()
    {
    }

    /**
     * Wait until the given number of unread notification is received. This method uses a request to the REST
     * notification endpoint to compute how many unread notifications the given user has on the given wiki, using user
     * preferences.
     *
     * @param userId the serialized user reference for which to get notifications
     * @param wiki the wiki on which to get notifications
     * @param expectedUnread the number of expected unread notifications to wait for
     * @since 12.6
     */
    public static void waitOnNotificationCount(String userId, String wiki, int expectedUnread)
    {
        URI baseNotificationResourcesURI = getUtil().rest().createUri(NotificationsResource.class, Map.of());
        // Note: This number of attempts should be enough, with 10 seconds of delay between the first and the last
        // attempt.
        int maxAttempts = 10;
        boolean success = false;
        long lastCount = -1;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Optional<Long> optionalCount = attemptCount(userId, wiki, baseNotificationResourcesURI, getUtil());
            if (optionalCount.isPresent()) {
                Long count = optionalCount.get();
                lastCount = count;
                if (count == expectedUnread) {
                    success = true;
                    break;
                }
            }
            try {
                // A delay before the next attempt, to leave server-side processing the notifications asynchronously.
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Throw an exception if despite all attempts, the expected notifications count is never reached.
        if (!success) {
            throw new TimeoutException(
                String.format("Failed after [%s] attempts. Expected: [%s] - Latest value received: [%s].", maxAttempts,
                    expectedUnread, lastCount));
        }
        // Ensure to refresh the page after calling this wait, so the notification tray is updated.
        getUtil().getDriver().navigate().refresh();
    }

    private static Optional<Long> attemptCount(String userId, String wiki, URI uri, TestUtils testUtils)
    {
        URI attemptURI = UriBuilder.fromUri(uri)
            .segment("count")
            .queryParam("media", "json")
            .queryParam("userId", userId)
            .queryParam("useUserPreferences", Boolean.TRUE.toString())
            .queryParam("currentWiki", wiki)
            .queryParam("async", Boolean.FALSE.toString())
            .queryParam("_", System.currentTimeMillis())
            .build();
        try {
            GetMethod getMethod = testUtils.rest().executeGet(attemptURI);
            if (Set.of(200, 202).contains(getMethod.getStatusCode())) {
                String responseBody = IOUtils.toString(getMethod.getResponseBodyAsStream(), UTF_8);
                Map<?, ?> map = new ObjectMapper().readValue(responseBody, Map.class);
                return getOptionalLong(String.valueOf(map.get("unread")), responseBody);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<Long> getOptionalLong(String str, String responseBody)
    {
        try {
            return Optional.of(Long.parseLong(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * @return either or not the notification menu is open
     */
    public boolean isMenuOpen()
    {
        return Arrays.asList(notificationsButton.getAttribute(CLASS).split(" ")).contains("open");
    }

    /**
     * @return {@code true} only if the notification menu icon (small bell) is displayed.
     * @since 12.6
     * @since 12.5.1
     */
    public boolean isNotificationMenuVisible()
    {
        try {
            return notificationsButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Test if the text "No notification available!" is displayed in the notification tray.
     *
     * @return true if the text is not displayed
     */
    public boolean areNotificationsAvailable()
    {
        this.showNotificationTray();

        return !this.notificationsArea.getText().equals("No notifications available!");
    }

    /**
     * Get the number of available notifications.
     *
     * @return Number of unread notifications, 0 if no notification available and MAX_INT if 20+ notifications
     */
    public int getNotificationsCount()
    {
        // This part is async
        if (!this.areNotificationsAvailable()) {
            return 0;
        }
        if (this.countBadge.getText().equals("20+")) {
            return Integer.MAX_VALUE;
        } else {
            return Integer.parseInt(this.countBadge.getText());
        }
    }

    /**
     * Click the button «Clear All» in the notification tray.
     */
    public void clearAllNotifications()
    {
        if (!this.areNotificationsAvailable()) {
            return;
        } else {
            this.showNotificationTray();
            WebElement clearAllLink = getDriver().findElement(By.className("notification-event-clean"));
            if (!clearAllLink.isDisplayed()) {
                throw new RuntimeException("'Clear All' link is not displayed!");
            }
            // Calling click() doesn't have the expected result: the mouse is moved over the link (we can see that the
            // link is hovered because it gets underlined and the link URL is displayed in the browser status bar) but
            // the link is not clicked (the event listeners are not fired and neither the browser's default behavior,
            // because the window location doesn't change). If we pause the test and click the link ourselves everything
            // is fine. Moreover, if I change the link style to display:block and height:100px the click also works. So
            // it seems this is a Selenium / WebDriver bug. The workaround is to use the Enter / Return key.
            clearAllLink.sendKeys(Keys.RETURN);
            // Wait for the confirm box to be visible
            getDriver().waitUntilElementIsVisible(By.className("xdialog-content"));
            // Enter is like clicking on "yes"
            WebElement yesButton = getDriver().findElement(
                By.cssSelector(".xdialog-content input.button[value='Yes']"));
            yesButton.click();
            this.waitForNotificationSuccessMessage("Notifications have been cleared");
        }
    }

    /**
     * Ensure that the notifications tray is visible.
     */
    public void showNotificationTray()
    {
        getDriver().scrollTo(this.watchListButton);
        if (!isMenuOpen()) {
            this.watchListButton.click();
            getDriver().waitUntilCondition(webDriver -> isMenuOpen());
            waitUntilNotificationsAreLoaded();
        }
        if (!isMenuOpen()) {
            throw new RuntimeException("Failed to open the notification menu!");
        }
    }

    private void waitUntilNotificationsAreLoaded()
    {
        getDriver().waitUntilCondition(webDriver -> {
            if (!notificationsArea.getAttribute(CLASS).contains("loading")) {
                try {
                    getDriver().findElementWithoutWaiting(notificationsArea, By.className("xwiki-async"));
                } catch (NoSuchElementException e) {
                    return true;
                }
            }
            return false;
        });
        pageOnlyWatchedSwitch = new BootstrapSwitch(
            toggles.findElement(By.className("bootstrap-switch-id-notificationPageOnly")),
            getDriver()
        );
        pageAndChildrenWatchedSwitch = new BootstrapSwitch(
            toggles.findElement(By.className("bootstrap-switch-id-notificationPageAndChildren")),
            getDriver()
        );
        wikiWatchedSwitch = new BootstrapSwitch(
            toggles.findElement(By.className("bootstrap-switch-id-notificationWiki")),
            getDriver()
        );
    }

    private List<WebElement> getNotifications()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector("li#tmNotifications div.notification-event"));
    }

    /**
     * Get the number of unread notifications.
     *
     * @return number of unread notifications
     */
    public int getUnreadNotificationsCount()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(
            "li#tmNotifications div.notification-event-unread")).size();
    }

    /**
     * Get the number of read notifications.
     *
     * @return number of read notifications
     */
    public int getReadNotificationsCount()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(
            "li#tmNotifications div.notification-event:not(.notification-event-unread)")).size();
    }

    /**
     * Get the number of notifications displayed.
     *
     * @return number of notifications
     */
    public int getNotificationsListCount()
    {
        return this.getNotifications().size();
    }

    /**
     * Get the type of notification (bold text before notification content).
     *
     * @param notificationNumber index of the notification in the list
     * @return notification type
     */
    public String getNotificationType(int notificationNumber)
    {
        if (notificationNumber < 0 || notificationNumber >= this.getNotificationsCount()) {
            throw new IndexOutOfBoundsException();
        }

        return this.getNotifications().get(notificationNumber).getAttribute("data-eventtype");
    }

    /**
     * Get the content of a notification.
     *
     * @param notificationNumber index of the notification in the list
     * @return notification content
     */
    public String getNotificationContent(int notificationNumber)
    {
        if (notificationNumber < 0 || notificationNumber >= this.getNotificationsCount()) {
            throw new IndexOutOfBoundsException();
        }

        return this.getNotifications().get(notificationNumber).findElement(
            By.cssSelector(".notification-content")).getText();
    }

    /**
     * Get the page concerned by a notification (if any).
     *
     * @param notificationNumber index of the notification in the list
     * @return notification page
     */
    public String getNotificationPage(int notificationNumber)
    {
        if (notificationNumber < 0 || notificationNumber >= this.getNotificationsCount()) {
            throw new IndexOutOfBoundsException();
        }

        return this.getNotifications().get(notificationNumber).findElement(
            By.cssSelector(".notification-page")).getText();
    }

    /**
     * Get the description of a notification.
     *
     * @param notificationNumber index of the notification in the list
     * @return notification description
     */
    public String getNotificationDescription(int notificationNumber)
    {
        if (notificationNumber < 0 || notificationNumber >= this.getNotificationsCount()) {
            throw new IndexOutOfBoundsException();
        }

        return this.getNotifications().get(notificationNumber).findElement(
            By.cssSelector(".notification-description")).getText();
    }

    /**
     * Get the raw content of a notification.
     *
     * @param notificationNumber index of the notification in the list
     * @return the notification raw content
     */
    public String getNotificationRawContent(int notificationNumber)
    {
        if (notificationNumber < 0 || notificationNumber >= this.getNotificationsCount()) {
            throw new IndexOutOfBoundsException();
        }

        return this.getNotifications().get(notificationNumber).getText();
    }

    /**
     * Mark a notification as read.
     *
     * @param notificationNumber index of the notification in the list
     */
    public void markAsRead(int notificationNumber)
    {
        if (notificationNumber < 0 || notificationNumber >= this.getNotificationsCount()) {
            throw new IndexOutOfBoundsException();
        }

        WebElement e = getNotifications().get(notificationNumber)
            .findElement(By.cssSelector("button.notification-event-read-button"));

        if (e != null) {
            e.click();
        }
    }

    /**
     * @return either or not the page is watched
     * @since 10.8RC1
     * @since 9.11.8
     */
    public boolean isPageOnlyWatched()
    {
        showNotificationTray();
        return pageOnlyWatchedSwitch.getState() == BootstrapSwitch.State.ON;
    }

    /**
     * @return either or not the "space" is watched
     * @since 10.8RC1
     * @since 9.11.8
     */
    public boolean arePageAndChildrenWatched()
    {
        showNotificationTray();
        return pageAndChildrenWatchedSwitch.getState() == BootstrapSwitch.State.ON;
    }

    /**
     * @return either or not the wiki is watched
     * @since 10.8RC1
     * @since 9.11.8
     */
    public boolean isWikiWatched()
    {
        showNotificationTray();
        return wikiWatchedSwitch.getState() == BootstrapSwitch.State.ON;
    }

    private void waitUntilWatchedStateAreSaved() throws InterruptedException
    {
        waitForNotificationSuccessMessage("Saved!");
        getDriver().waitUntilCondition(driver ->
            pageOnlyWatchedSwitch.isEnabled()
                && pageAndChildrenWatchedSwitch.isEnabled()
                && wikiWatchedSwitch.isEnabled()
        );
    }

    /**
     * Watch or unwatch the current page.
     *
     * @param watched the desired state
     * @throws Exception if the expected state cannot be set
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void setPageOnlyWatchedState(boolean watched) throws Exception
    {
        showNotificationTray();
        pageOnlyWatchedSwitch.setState(watched ? BootstrapSwitch.State.ON : BootstrapSwitch.State.OFF);
        waitUntilWatchedStateAreSaved();
    }

    /**
     * Watch or unwatch the current "space".
     *
     * @param watched the desired state
     * @throws Exception if the expected state cannot be set
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void setPageAndChildrenWatchedState(boolean watched) throws Exception
    {
        showNotificationTray();
        pageAndChildrenWatchedSwitch.setState(watched ? BootstrapSwitch.State.ON : BootstrapSwitch.State.OFF);
        waitUntilWatchedStateAreSaved();
    }

    /**
     * Watch or unwatch the current wiki.
     *
     * @param watched the desired state
     * @throws Exception if the expected state cannot be set
     * @since 10.8RC1
     * @since 9.11.8
     */
    public void setWikiWatchedState(boolean watched) throws Exception
    {
        showNotificationTray();
        wikiWatchedSwitch.setState(watched ? BootstrapSwitch.State.ON : BootstrapSwitch.State.OFF);
        waitUntilWatchedStateAreSaved();
    }

    /**
     * @param username name of the current user
     * @param password password of the current user
     * @return the rss feed
     * @since 11.5RC1
     * @since 11.4
     * @since 11.3.1
     */
    public NotificationsRSS getNotificationRSS(String username, String password)
    {
        String url = this.rssLink.getAttribute("href");
        return new NotificationsRSS(url, username, password);
    }

    /**
     * @return The tray notification button.
     * @since 12.8RC1
     */
    public WebElement getNotificationsButton()
    {
        return this.notificationsButton;
    }

    /**
     * @return a new instance of {@link GroupedNotificationElementPage}.
     * @since 15.5
     */
    public GroupedNotificationElementPage getGroupedNotificationsPage()
    {
        return new GroupedNotificationElementPage(this.getNotificationsButton());
    }
}
