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
package org.xwiki.test.ui.po.editor;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/** User profile, change password action. */
public class ChangePasswordPage extends EditPage
{
    private static final String DEFAULT_PASSWORD = "admin";

    @FindBy(xpath = "//input[@id='xwikipassword']")
    private WebElement password1;

    @FindBy(xpath = "//input[@id='xwikipassword2']")
    private WebElement password2;

    @FindBy(xpath = "//input[@value='Update']")
    private WebElement changePassword;

    @FindBy(xpath = "//a[@class='secondary button']")
    private WebElement cancelPasswordChange;

    public void changePassword(String password, String password2)
    {
        this.password1.clear();
        this.password1.sendKeys(password);
        this.password2.clear();
        this.password2.sendKeys(password2);
    }

    public void changePasswordToDefault()
    {
        changePassword(DEFAULT_PASSWORD, DEFAULT_PASSWORD);
    }

    public void submit()
    {
        this.changePassword.click();
    }

    public void cancel()
    {
        this.cancelPasswordChange.click();
    }
}
