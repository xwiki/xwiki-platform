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
package org.xwiki.ircbot.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the IRC.IRCBot page.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class IRCBotPage extends ViewPage
{
    @FindBy(xpath = "//input[@type='submit'][@name='botaction']")
    private WebElement actionButton;

    /**
     * Opens the IRC Bot page.
     */
    public static IRCBotPage gotoPage()
    {
        getUtil().gotoPage("IRC", "IRCBot");
        return new IRCBotPage();
    }

    /**
     * Clicks the Bot action button (either starts or stops the Bot).
     */
    public void clickActionButton()
    {
        this.actionButton.click();
    }

    /**
     * @return true if the bot is started or false otherwise
     */
    public boolean isBotStarted()
    {
        return this.actionButton.getAttribute("value").equalsIgnoreCase("Stop the IRC Bot");
    }
}
