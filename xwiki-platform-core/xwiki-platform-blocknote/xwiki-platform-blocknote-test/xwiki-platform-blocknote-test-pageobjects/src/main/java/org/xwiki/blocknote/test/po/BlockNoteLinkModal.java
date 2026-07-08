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
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the link modal used to create a link from the current selection or to edit an existing link.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
public class BlockNoteLinkModal extends BaseElement
{
    /**
     * Create a new instance, waiting for the link modal to be visible.
     */
    public BlockNoteLinkModal()
    {
        getDriver().waitUntilCondition(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='linkDisplayText']")));
    }

    /**
     * Replaces the content of the link display text field with the given title.
     *
     * @param title the new link title
     */
    public void setDisplayText(String title)
    {
        WebElement displayTextInput = getDriver().findElement(By.cssSelector("[data-test='linkDisplayText']"));
        // Focus the field explicitly by clicking it: the focus performed implicitly when sending keys doesn't always
        // deliver the keys to the field. Send the selection, the field content and the submission key separately:
        // the field is a Vue controlled input which can miss keys that are sent in a single burst.
        displayTextInput.click();
        displayTextInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        displayTextInput.sendKeys(title);
    }

    /**
     * Selects the type of target the link points to (e.g. {@code Page}, {@code URL}, {@code Attachment} or
     * {@code E-mail}).
     *
     * @param targetType the label of the target type to select
     */
    public void selectTargetType(String targetType)
    {
        WebElement targetTypeSelect = getDriver().findElement(By.cssSelector("[data-test='linkTargetType']"));
        new Select(targetTypeSelect).selectByVisibleText(targetType);
    }

    /**
     * Types the given URL in the URL field. The URL field is only available when the target type is set to
     * {@code URL}.
     *
     * @param url the URL to type
     */
    public void setUrl(String url)
    {
        WebElement urlInput = getDriver().findElement(By.cssSelector("[data-test='linkUrl']"));
        urlInput.click();
        urlInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        urlInput.sendKeys(url);
    }

    /**
     * Selects the {@code URL} target type, types the given URL and submits the modal.
     *
     * @param target the URL to set as the link target
     */
    public void setTargetAndSubmit(String target)
    {
        selectTargetType("URL");
        setUrl(target);
        submit();
    }

    /**
     * Replaces the link title with the given value and submits the modal.
     *
     * @param title the new link title
     */
    public void setTitleAndSubmit(String title)
    {
        setDisplayText(title);
        submit();
    }

    /**
     * Submits the link modal, applying the changes.
     */
    public void submit()
    {
        getDriver().findElement(By.cssSelector("[data-test='linkSubmit']")).click();
    }

    /**
     * Cancels the link modal, discarding any change.
     */
    public void cancel()
    {
        getDriver().findElement(By.cssSelector("[data-test='linkCancel']")).click();
    }
}
