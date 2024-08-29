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
package org.xwiki.whatsnew.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the What's New template page.
 *
 * @version $Id$
 */
public class WhatsNewPage extends ViewPage
{
    @FindBy(className = "xwiki-whatsnew-item")
    private List<WebElement> items;

    /**
     * Displays the What's New UI (template) on an arbitrary page.
     *
     * @return the page object instance
     */
    public static WhatsNewPage gotoPage()
    {
        getUtil().gotoPage("Main", "WebHome", "view", "xpage=whatsnew");
        return new WhatsNewPage();
    }

    /**
     * @return the number of news items displayed
     */
    public int getNewsItemCount()
    {
        return this.items.size();
    }

    /**
     * @param index the position of the new items for which to get the title (starts at 0)
     * @return the item's title
     */
    public String getNewsItemTitle(int index)
    {
        return this.items.get(index).findElement(By.className("xwiki-whatsnew-item-title")).getText();
    }

    /**
     * @param index the position of the new items for which to get the description (starts at 0)
     * @return the item's description
     */
    public String getNewsItemDescription(int index)
    {
        return this.items.get(index).findElement(By.className("xwiki-whatsnew-item-description")).getText();
    }

    /**
     * @param index the position of the new items for which to get the published date (starts at 0)
     * @return the item's published date
     */
    public String getNewsItemDate(int index)
    {
        return this.items.get(index).findElement(By.className("xwiki-whatsnew-item-date")).getText();
    }
}
