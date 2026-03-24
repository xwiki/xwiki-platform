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
package org.xwiki.blocknote.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the BlockNote toolbar.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
public class BlockNoteToolBar extends BaseElement
{
    private WebElement container;

    /**
     * Create a new instance that can be used to interact with the specified BlockNote toolbar.
     *
     * @param container the element containing the toolbar
     */
    public BlockNoteToolBar(WebElement container)
    {
        this.container = container;
    }

    /**
     * @param action the action associated with the button to retrieve
     * @return the button associated with the specified action
     */
    public WebElement getButton(String action)
    {
        return this.container.findElement(By.cssSelector("button[data-test='" + action + "']"));
    }

    /**
     * Clicks the button associated with the specified action.
     *
     * @param action the action associated with the button to click
     */
    public void clickButton(String action)
    {
        getButton(action).click();
    }

    /**
     * Checks if the button associated with the specified action is disabled.
     *
     * @param action the action associated with the button to check
     * @return {@code true} if the button is disabled, {@code false} otherwise
     */
    public boolean isButtonDisabled(String action)
    {
        return getButton(action).getAttribute("disabled") != null;
    }

    /**
     * Checks if the button associated with the specified action is toggled.
     *
     * @param action the action associated with the button to check
     * @return {@code true} if the button is toggled, {@code false} otherwise
     */
    public boolean isButtonToggled(String action)
    {
        return "true".equals(getButton(action).getAttribute("aria-pressed"));
    }
}
