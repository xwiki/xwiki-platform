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
import org.openqa.selenium.support.FindBy;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represent the result page when asking for a forgotten password.
 *
 * @version $Id$
 * @since 11.10
 */
public class ForgotUsernameCompletePage extends ViewPage
{
    @FindBy(css = ".xwikimessage")
    private WebElement messageBox;

    /**
     * @deprecated since 12.10.5 and 13.2RC1 this message is no longer displayed.
     */
    @Deprecated
    public boolean isUsernameRetrieved(String username)
    {
        return messageBox.getText().contains(username);
    }

    /**
     * @deprecated since 12.10.5 and 13.2RC1 this message is no longer displayed.
     */
    @Deprecated
    public boolean isAccountNotFound()
    {
        return messageBox.getText().contains("No account is registered using this email address");
    }

    /**
     * @return the text content of the message box.
     * @since 12.10.5
     * @since 13.2RC1
     */
    @Unstable
    public String getMessage()
    {
        return this.messageBox.getText();
    }

    /**
     * @return {@code true} if the forgot username query was successfully sent (without any error).
     * @since 12.10.5
     * @since 13.2RC1
     */
    @Unstable
    public boolean isForgotUsernameQuerySent()
    {
        // If there is no form and we see an info box, then the request was sent.
        return !getDriver().hasElementWithoutWaiting(By.cssSelector("#forgotUsernameForm"))
            && messageBox.getText().contains("If an account is registered with this email, "
            + "you will receive the account information on");
    }
}
