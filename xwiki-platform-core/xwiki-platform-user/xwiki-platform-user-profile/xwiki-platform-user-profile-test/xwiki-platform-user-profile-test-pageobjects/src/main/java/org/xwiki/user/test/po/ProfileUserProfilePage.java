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

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the User Profile Profile Tab.
 *
 * @version $Id$
 */
public class ProfileUserProfilePage extends AbstractUserProfilePage
{
    private static final By AVATAR_IMAGE = By.xpath("//div[@id='avatar']//img");

    @FindBy(xpath = "//div[@class='userInfo']/div[@class='editProfileCategory']/a")
    private WebElement editProfile;

    @FindBy(className = "given-name")
    private WebElement userFirstName;

    @FindBy(className = "family-name")
    private WebElement userLastName;

    @FindBy(className = "org")
    private WebElement userCompany;

    @FindBy(className = "note")
    private WebElement userAbout;

    @FindBy(className = "email")
    private WebElement userEmail;

    @FindBy(className = "tel")
    private WebElement userPhone;

    @FindBy(className = "adr")
    private WebElement userAddress;

    @FindBy(xpath = "//dd[preceding-sibling::dt[1]/label[. = 'Blog']]//a")
    private WebElement userBlog;

    @FindBy(xpath = "//dd[preceding-sibling::dt[1]/label[. = 'Blog Feed']]//a")
    private WebElement userBlogFeed;

    @FindBy(xpath = "//div[@id='avatar']//a")
    private WebElement changeAvatar;

    @FindBy(css = ".activity-follow .notificationWatchUserFollowing")
    private WebElement followingContainer;

    @FindBy(css = ".activity-follow .notificationWatchUserNotFollowing")
    private WebElement notFollowingContainer;

    @FindBy(id = "disable")
    private WebElement disableButton;

    @FindBy(id = "enable")
    private WebElement enableButton;

    public static ProfileUserProfilePage gotoPage(String username)
    {
        getUtil().gotoPage("XWiki", username);
        ProfileUserProfilePage page = new ProfileUserProfilePage(username);
        return page;
    }

    public ProfileUserProfilePage(String username)
    {
        super(username);
    }

    public ProfileEditPage editProfile()
    {
        this.editProfile.click();
        return new ProfileEditPage();
    }

    public String getURL()
    {
        return getUtil().getURL("XWiki", getUsername());
    }

    public String getUserFirstName()
    {
        return this.userFirstName.getText();
    }

    public String getUserLastName()
    {
        return this.userLastName.getText();
    }

    public String getUserCompany()
    {
        return this.userCompany.getText();
    }

    public String getUserAbout()
    {

        return this.userAbout.getText();
    }

    public String getUserEmail()
    {
        return this.userEmail.getText();
    }

    public String getUserPhone()
    {
        return this.userPhone.getText();
    }

    public String getUserAddress()
    {
        return this.userAddress.getText();
    }

    public String getUserBlog()
    {
        return this.userBlog.getText();
    }

    public String getUserBlogFeed()
    {
        return this.userBlogFeed.getText();
    }

    /**
     * Gets the displayed value of a custom profile field (i.e. one added to extend the user profile), located by the
     * pretty name shown as its label.
     *
     * @param prettyName the pretty name (label) of the custom field as displayed in the profile
     * @return the displayed value of the custom field
     */
    public String getUserCustomProperty(String prettyName)
    {
        return getDriver().findElementWithoutWaiting(
            By.xpath("//dd[preceding-sibling::dt[1]/label[. = '" + prettyName + "']]")).getText();
    }

    public ChangeAvatarPage changeAvatarImage()
    {
        this.changeAvatar.click();
        getDriver().waitUntilElementIsVisible(By.id("uploadAttachment"));
        return new ChangeAvatarPage();
    }

    public String getAvatarImageName()
    {
        // Read the avatar image source without scrolling and without a cached element: the profile page is reloaded
        // when the avatar is changed, so scrolling to a cached element can hit a stale element reference.
        return StringUtils.substringBefore(StringUtils.substringAfterLast(
            getDriver().findElementWithoutWaitingWithoutScrolling(AVATAR_IMAGE).getAttribute("src"), "/"), "?");
    }

    /**
     * Waits until the avatar image displayed on the profile has the passed image name as its source. The profile page
     * is reloaded after the avatar is changed, so waiting avoids reading the previous avatar or a stale image element
     * while the page is still reloading.
     *
     * @param imageName the expected avatar image file name (for example {@code avatar.png})
     * @since 18.6.0RC1
     */
    public void waitUntilAvatarImageName(String imageName)
    {
        getDriver().waitUntilCondition(driver -> {
            try {
                return imageName.equals(getAvatarImageName());
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                // The avatar image is not present yet or was removed from the DOM while the page was reloading.
                return false;
            }
        });
    }

    public boolean isFollowed()
    {
        return followingContainer.isDisplayed();
    }

    public ProfileUserProfilePage toggleFollowButton()
    {
        WebElement container = (isFollowed()) ? followingContainer : notFollowingContainer;
        getDriver().findElementWithoutWaiting(container, By.tagName("button")).click();
        container.findElement(By.className("dropdown-menu")).findElement(By.tagName("a")).click();
        waitForNotificationSuccessMessage("Done");
        return new ProfileUserProfilePage(this.getUsername());
    }

    public boolean isEnableButtonAvailable()
    {
        try {
            return this.enableButton.isDisplayed() && this.enableButton.isEnabled();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isDisableButtonAvailable()
    {
        try {
            return this.disableButton.isDisplayed() && this.disableButton.isEnabled();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void clickEnable()
    {
        this.enableButton.click();
        waitForNotificationSuccessMessage("Done");
    }

    public void clickDisable()
    {
        this.disableButton.click();
        waitForNotificationSuccessMessage("Done");
    }
}
