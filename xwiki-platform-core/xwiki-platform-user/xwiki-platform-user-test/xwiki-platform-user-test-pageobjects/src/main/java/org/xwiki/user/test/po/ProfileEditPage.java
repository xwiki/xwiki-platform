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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.editor.EditPage;

/** User profile, the profile information pane, edit mode. */
public class ProfileEditPage extends EditPage
{
    @FindBy(id = "XWiki.XWikiUsers_0_first_name")
    private WebElement userFirstName;

    @FindBy(id = "XWiki.XWikiUsers_0_last_name")
    private WebElement userLastName;

    @FindBy(id = "XWiki.XWikiUsers_0_company")
    private WebElement userCompany;

    @FindBy(id = "XWiki.XWikiUsers_0_email")
    private WebElement userEmail;

    @FindBy(id = "XWiki.XWikiUsers_0_phone")
    private WebElement userPhone;

    @FindBy(id = "XWiki.XWikiUsers_0_blog")
    private WebElement userBlog;

    @FindBy(id = "XWiki.XWikiUsers_0_blogfeed")
    private WebElement userBlogFeed;

    @FindBy(id = "XWiki.XWikiUsers_0_comment")
    private WebElement userAbout;

    @FindBy(id = "XWiki.XWikiUsers_0_address")
    private WebElement userAddress;

    public String getUserFirstName()
    {
        return this.userFirstName.getText();
    }

    public void setUserFirstName(String userFirstName)
    {
        this.userFirstName.clear();
        this.userFirstName.sendKeys(userFirstName);
    }

    public String getUserLastName()
    {
        return this.userLastName.getText();
    }

    public void setUserLastName(String userLastName)
    {
        this.userLastName.clear();
        this.userLastName.sendKeys(userLastName);
    }

    public String getUserCompany()
    {
        return this.userCompany.getText();
    }

    public void setUserCompany(String userCompany)
    {
        this.userCompany.clear();
        this.userCompany.sendKeys(userCompany);
    }

    public String getUserAbout()
    {
        return this.userAbout.getText();
    }

    public void setUserAbout(String userAbout)
    {
        this.userAbout.clear();
        this.userAbout.sendKeys(userAbout);
    }

    public String getUserEmail()
    {
        return this.userEmail.getText();
    }

    public void setUserEmail(String userEmail)
    {
        this.userEmail.clear();
        this.userEmail.sendKeys(userEmail);
    }

    public String getUserPhone()
    {
        return this.userPhone.getText();
    }

    public void setUserPhone(String userPhone)
    {
        this.userPhone.clear();
        this.userPhone.sendKeys(userPhone);
    }

    public String getUserAddress()
    {
        return this.userAddress.getText();
    }

    public void setUserAddress(String userAddress)
    {
        this.userAddress.clear();
        this.userAddress.sendKeys(userAddress);
    }

    public String getUserBlog()
    {
        return this.userBlog.getText();
    }

    public void setUserBlog(String userBlog)
    {
        this.userBlog.clear();
        this.userBlog.sendKeys(userBlog);
    }

    public String getUserBlogFeed()
    {
        return this.userBlogFeed.getText();
    }

    public void setUserBlogFeed(String userBlogFeed)
    {
        this.userBlogFeed.clear();
        this.userBlogFeed.sendKeys(userBlogFeed);
    }
}
