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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the User Profile Preferences Tab.
 * 
 * @version $Id$
 */
public class PreferencesUserProfilePage extends AbstractUserProfilePage
{
    @FindBy(xpath = "//div[@id='preferencesPane']//div[@class='editProfileCategory']/a")
    private WebElement editPreferences;

    @FindBy(xpath = "//div[@id='preferencesPane']/div[1]/div/dl[2]/dd[1]")
    private WebElement timezone;

    @FindBy(xpath = "//div[@id='preferencesPane']/div[1]/div/dl[3]/dd[2]")
    private WebElement userType;

    @FindBy(xpath = "//div[@id='preferencesPane']/div[1]/div/dl[3]/dd[1]")
    private WebElement defaultEditorToUse;

    @FindBy(xpath = "//div[@id='preferencesPane']/div[1]/div/dl[5]/dd[1]")
    private WebElement viewEditShortcut;

    @FindBy(xpath = "//a[@id='changePassword']")
    private WebElement changePassword;

    public PreferencesUserProfilePage(String username)
    {
        super(username);
        getDriver().waitUntilElementIsVisible(By.id("preferencesPane"));
    }

    public String getDefaultEditor()
    {
        return this.defaultEditorToUse.getText();
    }

    public String getUserType()
    {
        return this.userType.getText();
    }

    public String getTimezone()
    {
        return this.timezone.getText();
    }

    public String getViewEditShortcut()
    {
        return this.viewEditShortcut.getText();
    }

    public PreferencesEditPage editPreferences()
    {
        getDriver().addPageNotYetReloadedMarker();
        this.editPreferences.click();
        getDriver().waitUntilPageIsReloaded();
        PreferencesEditPage preferencesEditPage = new PreferencesEditPage();
        return preferencesEditPage;
    }

    public ChangePasswordPage changePassword()
    {
        this.changePassword.click();
        return new ChangePasswordPage();
    }
}
