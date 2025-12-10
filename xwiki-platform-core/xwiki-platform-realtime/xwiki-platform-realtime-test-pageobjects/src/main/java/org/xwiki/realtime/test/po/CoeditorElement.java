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
package org.xwiki.realtime.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Used to display an user that is participating to the realtime editing session.
 * 
 * @version $Id$
 * @since 16.10.6
 * @since 17.3.0RC1
 */
public class CoeditorElement extends BaseElement
{
    private WebElement container;

    /**
     * Creates a new instance based on the given coeditor element.
     * 
     * @param container the WebElement used to display the coeditor
     */
    public CoeditorElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return {@code true} if the coeditor is displayed, {@code false} otherwise (e.g. if the coeditor is listed in the
     *         dropdown and the dropdown is closed)
     */
    public boolean isDisplayed()
    {
        return this.container.isDisplayed();
    }

    /**
     * @return the coeditor identifier
     */
    public String getId()
    {
        return this.container.getDomAttribute("data-id");
    }

    /**
     * @return the coeditor's displayed name (is empty if the coeditor is not displayed)
     */
    public String getName()
    {
        return this.container.findElement(By.className("realtime-user-name")).getText();
    }

    /**
     * @return the XWiki user reference
     */
    public String getReference()
    {
        return this.container.getDomAttribute("data-reference");
    }

    /**
     * @return the user profile URL
     */
    public String getURL()
    {
        return this.container.getDomAttribute("href");
    }

    /**
     * @return the user avatar URL
     */
    public String getAvatarURL()
    {
        return getAvatar().getDomAttribute("src");
    }

    /**
     * @return the user avatar hint, usually the user full name
     */
    public String getAvatarHint()
    {
        return getAvatar().getDomAttribute("title");
    }

    /**
     * @return the user name abbreviation
     */
    public String getAbbreviation()
    {
        return this.container.findElement(By.className("realtime-user-avatar-wrapper")).getDomAttribute("data-abbr");
    }

    /**
     * Click on the coeditor avatar to see where the user is editing.
     */
    public void click()
    {
        this.container.click();
    }

    private WebElement getAvatar()
    {
        return this.container.findElement(By.className("realtime-user-avatar"));
    }
}
