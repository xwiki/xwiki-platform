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

import org.jspecify.annotations.NonNull;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.wysiwyg.test.po.MacroDialogEditModal;
import org.xwiki.wysiwyg.test.po.MacroDialogSelectModal;
import org.xwiki.wysiwyg.test.po.image.ImageDialogEditModal;

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
        return this.container.findElement(getButtonSelector(action));
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
     * Checks if the button associated with the specified action is present in the toolbar.
     *
     * @param action the action associated with the button to check
     * @return {@code true} if the button is present, {@code false} otherwise
     * @since 18.6.0
     */
    public boolean hasButton(String action)
    {
        return !getDriver().findElementsWithoutWaiting(this.container, getButtonSelector(action)).isEmpty();
    }

    private @NonNull By getButtonSelector(String action)
    {
        return By.cssSelector("button[data-test='" + action + "']");
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

    /**
     * Clicks the button to edit the selected image. This opens the image edit modal.
     * 
     * @return the image edit modal
     * @since 18.3.0RC1
     */
    public ImageDialogEditModal editImage()
    {
        clickButton("changeimage");
        return new ImageDialogEditModal();
    }

    /**
     * Clicks the button to create a link from the current selection. This opens the link popover.
     *
     * @return the link popover
     * @since 18.6.0RC1
     */
    public BlockNoteLinkPopover createLink()
    {
        clickButton("createLink");
        return new BlockNoteLinkPopover();
    }

    /**
     * Checks if the button to create a link is present in the toolbar.
     *
     * @return {@code true} if the button is present, {@code false} otherwise
     * @since 18.6.0
     */
    public boolean hasCreateLinkButton()
    {
        return hasButton("createLink");
    }

    /**
     * Clicks the button to edit the selected link. This opens the link popover.
     *
     * @return the link popover
     * @since 18.6.0RC1
     */
    public BlockNoteLinkPopover editLink()
    {
        clickButton("editLink");
        return new BlockNoteLinkPopover();
    }

    /**
     * Clicks the button to insert a new macro call. This opens the macro selector modal.
     *
     * @return the macro selector modal
     * @since 18.6.0
     */
    public MacroDialogSelectModal insertMacro()
    {
        clickButton("insertMacro");
        return new MacroDialogSelectModal().waitUntilReady();
    }

    /**
     * Clicks the button to edit the selected macro. This opens the macro edit modal.
     *
     * @return the macro edit modal
     * @since 18.6.0
     */
    public MacroDialogEditModal editMacro()
    {
        clickButton("editMacro");
        return new MacroDialogEditModal().waitUntilReady();
    }
}
