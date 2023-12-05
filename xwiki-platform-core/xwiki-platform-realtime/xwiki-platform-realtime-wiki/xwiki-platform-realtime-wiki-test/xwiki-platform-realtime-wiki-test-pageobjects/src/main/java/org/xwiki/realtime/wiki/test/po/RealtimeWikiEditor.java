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
package org.xwiki.realtime.wiki.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the Realtime Wiki Editor.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.10
 */
public class RealtimeWikiEditor extends BaseElement
{
    @FindBy(className = "realtime-allow")
    private WebElement allowRealtimeCheckbox;

    @FindBy(id = "xwikimaincontainer")
    private WebElement mainContainerDiv;

    @FindBy(id = "xwikieditcontent")
    private WebElement editContentDiv;

    public void sendKeys(CharSequence... keys)
    {
        getDriver().executeScript("arguments[0].focus()", editContentDiv);
        mainContainerDiv.sendKeys(keys);
    }

    /**
     * Waits until the given user is present in the list of coeditors.
     * 
     * @param user The user to wait for.
     */
    public void waitUntilEditingWith(String user)
    {
        getDriver().waitUntilElementHasTextContent(By.cssSelector("a.rt-user-link"), user);
    }

    /**
     * @return {@code true} if realtime editing is enabled, {@code false} otherwise
     */
    public boolean isRealtimeEditing()
    {
        return this.allowRealtimeCheckbox.isSelected();
    }

    /**
     * Leave the realtime editing session.
     */
    public void leaveRealtimeEditing()
    {
        if (isRealtimeEditing()) {
            this.allowRealtimeCheckbox.click();
        }
    }

    /**
     * Join the realtime editing session.
     */
    public void joinRealtimeEditing()
    {
        if (!isRealtimeEditing()) {
            this.allowRealtimeCheckbox.click();
        }
    }

}
