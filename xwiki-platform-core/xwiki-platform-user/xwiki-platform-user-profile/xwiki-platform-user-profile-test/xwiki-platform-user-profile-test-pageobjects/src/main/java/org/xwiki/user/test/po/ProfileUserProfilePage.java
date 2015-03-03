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

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the User Profile Profile Tab.
 * 
 * @version $Id$
 */
public class ProfileUserProfilePage extends AbstractUserProfilePage
{
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

    @FindBy(xpath = "//div[@id='avatar']//img")
    private WebElement userAvatarImage;

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

    public ChangeAvatarPage changeAvatarImage()
    {
        this.changeAvatar.click();
        getDriver().waitUntilElementIsVisible(By.id("uploadAttachment"));
        return new ChangeAvatarPage();
    }

    public String getAvatarImageName()
    {
        return StringUtils.substringBefore(
            StringUtils.substringAfterLast(this.userAvatarImage.getAttribute("src"), "/"), "?");
    }
}
