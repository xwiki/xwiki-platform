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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.LoginPage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the {@code XWiki.AccountValidation} page, which activates an account from the validation key sent by email
 * to a newly registered user.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class AccountValidationPage extends ViewPage
{
    private static final By SUCCESS_MESSAGE = By.cssSelector("#xwikicontent .box.infomessage");

    /**
     * @return {@code true} if the account has been activated
     */
    public boolean isAccountActivated()
    {
        return getDriver().hasElementWithoutWaiting(SUCCESS_MESSAGE);
    }

    /**
     * @return the message displayed after the account validation, whether it succeeded or not
     */
    public String getMessage()
    {
        return getDriver().findElement(By.cssSelector("#xwikicontent .box")).getText();
    }

    /**
     * Click on the login link displayed once the account has been activated.
     *
     * @return the login page
     */
    public LoginPage clickLogin()
    {
        WebElement loginLink = getDriver().findElement(SUCCESS_MESSAGE).findElement(By.tagName("a"));
        getDriver().addPageNotYetReloadedMarker();
        loginLink.click();
        getDriver().waitUntilPageIsReloaded();
        return new LoginPage();
    }
}
