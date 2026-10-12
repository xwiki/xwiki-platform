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
package org.xwiki.wiki.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the {@code WikiManager.JoinWiki} page, where a user joins a wiki whose membership type is open.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class JoinWikiPage extends ExtendedViewPage
{
    @FindBy(css = "#xwikicontent form button")
    private WebElement yesButton;

    /**
     * @return the question asking the user to confirm that they want to join the wiki
     */
    public String getConfirmationMessage()
    {
        return getDriver().findElement(By.cssSelector("#xwikicontent .box")).getText();
    }

    /**
     * Confirm that the current user wants to join the wiki.
     *
     * @return the page displaying the result of the join
     */
    public JoinWikiPage confirm()
    {
        getDriver().addPageNotYetReloadedMarker();
        this.yesButton.click();
        getDriver().waitUntilPageIsReloaded();
        return new JoinWikiPage();
    }

    /**
     * @return the message displayed when the user has successfully joined the wiki
     */
    public String getSuccessMessage()
    {
        return getDriver().findElement(By.cssSelector("#xwikicontent .box.successmessage")).getText();
    }
}
