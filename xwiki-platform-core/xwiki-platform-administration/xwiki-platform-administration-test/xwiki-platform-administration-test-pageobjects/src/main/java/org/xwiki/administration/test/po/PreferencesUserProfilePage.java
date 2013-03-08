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
package org.xwiki.administration.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.editor.ChangePasswordPage;
import org.xwiki.test.ui.po.editor.PreferencesEditPage;

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

    @FindBy(xpath = "//a[@id='changePassword']")
    private WebElement changePassword;

    public PreferencesUserProfilePage(String username)
    {
        super(username);
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

    public PreferencesEditPage editPreferences()
    {
        this.editPreferences.click();

        PreferencesEditPage editPage = new PreferencesEditPage();
        // The user profile and the user preferences are currently loaded together. This means that when we edit the
        // preferences the entire user profile is edited but only the preferences are visible. The consequence is that
        // the WYSIWYG editor is loaded (but not displayed) for the user profile fields even if they are hidden so we
        // need to wait for it to be safe.
        if (!"Text".equals(editPage.getDefaultEditor())) {
            new ProfileUserProfilePage(getUsername()).waitForProfileEditionToLoad();
        }

        return editPage;
    }

    public ChangePasswordPage changePassword()
    {
        this.changePassword.click();
        return new ChangePasswordPage();
    }
}
