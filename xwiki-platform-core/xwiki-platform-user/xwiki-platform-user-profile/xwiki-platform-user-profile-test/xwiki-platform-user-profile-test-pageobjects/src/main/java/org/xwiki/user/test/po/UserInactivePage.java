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
import org.xwiki.test.ui.po.BasePage;

/**
 * The page displayed instead of the requested page when the current user account is disabled or not yet active.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class UserInactivePage extends BasePage
{
    private static final String MESSAGE_SELECTOR = "#mainContentArea p.xwikimessage";

    @FindBy(css = MESSAGE_SELECTOR)
    private WebElement message;

    /**
     * Checks, without waiting, whether the current page is this page, i.e. whether the notice explaining that the
     * current user account is disabled or not yet active is displayed instead of the requested page. Call it once the
     * requested page is loaded.
     *
     * @return {@code true} if the notice is displayed instead of the requested page, {@code false} otherwise
     * @since 18.9.0RC1
     */
    public static boolean isDisplayed()
    {
        return getUtil().getDriver().hasElementWithoutWaiting(By.cssSelector(MESSAGE_SELECTOR));
    }

    /**
     * @return the notice explaining why the requested page is not displayed
     */
    public String getMessage()
    {
        return this.message.getText();
    }
}
