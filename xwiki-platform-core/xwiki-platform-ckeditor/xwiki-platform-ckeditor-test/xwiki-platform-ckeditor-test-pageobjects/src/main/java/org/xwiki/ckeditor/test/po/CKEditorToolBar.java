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
package org.xwiki.ckeditor.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.image.ImageDialogEditModal;
import org.xwiki.ckeditor.test.po.image.ImageDialogSelectModal;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the CKEditor tool bar.
 * 
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
 */
public class CKEditorToolBar extends BaseElement
{
    private final WebElement container;

    /**
     * Create a new tool bar instance for the given editor.
     * 
     * @param editor the editor that owns the tool bar
     */
    public CKEditorToolBar(CKEditor editor)
    {
        this.container = getDriver().findElementWithoutWaiting(editor.getContainer(), By.className("cke_top"));
    }

    /**
     * Click on the CKEditor image button from the tool bar to insert an image (i.e. when there's no image selected in
     * the editing area).
     *
     * @return a page object for the image selection modal
     */
    public ImageDialogSelectModal insertImage()
    {
        clickImageButton();
        return new ImageDialogSelectModal();
    }

    /**
     * Click on the CKEditor image button from the tool bar when an image widget is on focus (i.e., the image modal will
     * be opened in edit mode).
     *
     * @return a page object for the image edit modal
     */
    public ImageDialogEditModal editImage()
    {
        clickImageButton();
        return new ImageDialogEditModal();
    }

    private void clickImageButton()
    {
        clickButton("image");
    }

    /**
     * Click on the link button from the tool bar to insert or edit a link.
     *
     * @return the page object to interact with the link modal
     */
    public LinkDialog insertOrEditLink()
    {
        clickButton("link");
        return new LinkDialog();
    }

    private void clickButton(String feature)
    {
        getDriver().findElementWithoutWaiting(this.container, By.className("cke_button__" + feature)).click();
    }

    /**
     * Switch between the WYSIWYG and Source mode, using the dedicated tool bar button.
     */
    public void toggleSourceMode()
    {
        clickButton("source");
        // Wait for the conversion between HTML and wiki syntax (source) to be done.
        getDriver().waitUntilElementDisappears(this.container, By.cssSelector(".cke_button__source_icon.loading"));
    }
}
