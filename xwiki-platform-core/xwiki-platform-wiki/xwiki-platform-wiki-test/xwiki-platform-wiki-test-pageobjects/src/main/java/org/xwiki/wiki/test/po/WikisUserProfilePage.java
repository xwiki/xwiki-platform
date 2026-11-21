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
package org.xwiki.wiki.test.po;

import java.util.function.Predicate;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.notifications.test.po.NotificationsContainerElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The "Wikis" section of a user profile, listing the wikis the user is a member of.
 *
 * @version $Id$
 * @since 18.10.0
 */
public class WikisUserProfilePage extends ViewPage
{
    private static final String MAIN_WIKI = "xwiki";

    private static final By ACTIVITY_COLUMN = By.id("wikiActivityColumn");

    private static final By NOTIFICATIONS_MACRO = By.className("notifications-macro");

    /**
     * Maximum number of page reloads performed while waiting for the expected activity. Events are recorded
     * asynchronously, so the activity might not list them right after a page has been saved.
     */
    private static final int MAX_RELOAD_ATTEMPTS = 10;

    private final String username;

    /**
     * @param username the name of the user whose profile is displayed
     */
    public WikisUserProfilePage(String username)
    {
        this.username = username;
    }

    /**
     * Open the "Wikis" section of the profile of the given global user.
     *
     * @param username the name of the user whose profile is displayed
     * @return the page object
     */
    public static WikisUserProfilePage gotoPage(String username)
    {
        // The section is only available on the main wiki, which holds the profiles of the global users.
        getUtil().gotoPage(new DocumentReference(MAIN_WIKI, "XWiki", username), "view", "category=wikis");
        return new WikisUserProfilePage(username);
    }

    /**
     * Click the "show activity" link of a wiki and wait for its activity to be displayed, without reloading the page.
     *
     * @param wikiId the identifier of the wiki
     * @return the activity of the wiki
     */
    public NotificationsContainerElement showActivity(String wikiId)
    {
        getDriver().findElement(By.cssSelector("#wikis > li[id='" + wikiId + "'] .wikiActivity a")).click();
        WebElement activityColumn = getDriver().findElement(ACTIVITY_COLUMN);
        getDriver().waitUntilCondition(
            driver -> getDriver().hasElementWithoutWaiting(activityColumn, NOTIFICATIONS_MACRO));
        return new NotificationsContainerElement(activityColumn.findElement(NOTIFICATIONS_MACRO));
    }

    /**
     * Click the "show activity" link of a wiki until its activity matches the given condition, reloading the page
     * between attempts since the events are recorded asynchronously.
     *
     * @param wikiId the identifier of the wiki
     * @param condition the condition that the displayed activity must match
     * @return the activity of the wiki, displayed by the last click
     */
    public NotificationsContainerElement showActivity(String wikiId,
        Predicate<NotificationsContainerElement> condition)
    {
        NotificationsContainerElement activity = showActivity(wikiId);
        int attempts = 0;
        while (!condition.test(activity) && attempts < MAX_RELOAD_ATTEMPTS) {
            activity = gotoPage(this.username).showActivity(wikiId);
            attempts++;
        }
        return activity;
    }
}
