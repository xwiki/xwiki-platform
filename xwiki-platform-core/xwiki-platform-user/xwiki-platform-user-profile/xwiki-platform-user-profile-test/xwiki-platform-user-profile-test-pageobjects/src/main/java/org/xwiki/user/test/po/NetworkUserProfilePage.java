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
package org.xwiki.user.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the user profile's Network tab.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class NetworkUserProfilePage extends AbstractUserProfilePage
{
    /**
     * @param username the user profile document name
     * @return the network profile tab page object
     */
    public static NetworkUserProfilePage gotoPage(String username)
    {
        getUtil().gotoPage("XWiki", username, "view", "category=network");
        NetworkUserProfilePage page = new NetworkUserProfilePage(username);
        return page;
    }

    /**
     * @param username the user profile document name
     */
    public NetworkUserProfilePage(String username)
    {
        super(username);
    }

    public List<String> getFollowedUsers()
    {
        List<String> result = new ArrayList<String>();

        boolean empty =
            getDriver().findElementWithoutWaiting(By.cssSelector(".following")).getText()
                .contains("You are not following the activity of any user");
        if (!empty) {
            List<WebElement> userElements =
                getDriver().findElementsWithoutWaiting(By.cssSelector("#networkPane .following .user-id"));
            for (WebElement userElement : userElements) {
                String rawValue = userElement.getText();
                // Remove wrapping parenthesis in which the userID is displayed.
                result.add(rawValue.substring(1, rawValue.length() - 1));
            }
        }

        return result;
    }

    public void unfollowUser(String username)
    {
        WebElement unfollowButton =
            getDriver().findElementWithoutWaiting(
                By.xpath(String.format("//li[.//span[@class='user-id' and .='(%s)']]//a[contains(@class,'action')]",
                    username)));
        unfollowButton.click();
    }
}
