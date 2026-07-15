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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the link popover used to create a link from the current selection or to edit an existing link.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
public class BlockNoteLinkPopover extends BaseElement
{
    private final WebElement container;

    /**
     * Create a new instance, waiting for the link popover to be visible.
     */
    public BlockNoteLinkPopover()
    {
        this.container = getDriver().findElement(By.cssSelector(".bn-form-popover"));
        getDriver().waitUntilCondition(ExpectedConditions.visibilityOf(this.container));
    }

    /**
     * Types the given link target (a URL or an entity reference) in the target field and submits it by pressing Enter.
     *
     * @param target the link target
     */
    public void setTargetAndSubmit(String target)
    {
        WebElement targetInput = this.container.findElement(By.cssSelector("input[data-test='searchBoxInput']"));
        // Focus the field explicitly by clicking it: the focus performed implicitly when sending keys doesn't always
        // deliver the keys to the field (the popover doesn't focus its fields when opening). Send the field content
        // and the submission key separately: the field is a React controlled input which can miss keys that are sent
        // in a single burst.
        targetInput.click();
        targetInput.sendKeys(target);
        targetInput.sendKeys(Keys.ENTER);
    }

    /**
     * Replaces the content of the link title field with the given title and submits it by pressing Enter. The title
     * field is only available when editing an existing link.
     *
     * @param title the new link title
     */
    public void setTitleAndSubmit(String title)
    {
        WebElement titleInput = this.container.findElement(By.cssSelector("input[data-test='linkTitle']"));
        // Focus the field explicitly by clicking it: the focus performed implicitly when sending keys doesn't always
        // deliver the keys to the field (the popover doesn't focus its fields when opening). Send the selection, the
        // field content and the submission key separately: the field is a React controlled input which can miss keys
        // that are sent in a single burst.
        titleInput.click();
        titleInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        titleInput.sendKeys(title);
        titleInput.sendKeys(Keys.ENTER);
    }
}
