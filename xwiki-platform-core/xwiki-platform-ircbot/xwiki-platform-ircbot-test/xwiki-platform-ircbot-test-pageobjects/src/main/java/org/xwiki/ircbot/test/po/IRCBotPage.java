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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the IRC.IRCBot page.
 *
 * @version $Id$
 * @since 4.0M2
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

    /**
     * @param botListenerName the name of the listener bot (eg "Log")
     * @return true if the bot is displayed as started or false otherwise
     */
    public boolean isBotListenerStarted(String botListenerName)
    {
        boolean result = false;

        List<WebElement> tds = getDriver().findElements(By.xpath("//table[@id='listenertable']//tr/td"));
        for (int i = 0; i < tds.size(); i++) {
            if (tds.get(i).getText().equals(botListenerName)) {
                result = tds.get(i + 2).findElement(By.tagName("img")).getAttribute("alt").equalsIgnoreCase("accept");
                break;
            }
        }
        return result;
    }

    /**
     * @param botListenerName the name of the listener bot (eg "Log")
     * @return true if the bot is displayed in the listener table
     */
    public boolean containsListener(String botListenerName)
    {
        boolean result = false;

        List<WebElement> tds = getDriver().findElements(By.xpath("//table[@id='listenertable']//tr/td"));
        for (int i = 0; i < tds.size(); i++) {
            if (tds.get(i).getText().equals(botListenerName)) {
                result = true;
                break;
            }
        }
        return result;
    }
}
