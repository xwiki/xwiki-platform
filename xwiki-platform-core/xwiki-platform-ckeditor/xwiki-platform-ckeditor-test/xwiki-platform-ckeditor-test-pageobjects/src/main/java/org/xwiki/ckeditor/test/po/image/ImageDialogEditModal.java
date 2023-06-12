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
package org.xwiki.ckeditor.test.po.image;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.image.edit.ImageDialogAdvancedEditForm;
import org.xwiki.ckeditor.test.po.image.edit.ImageDialogStandardEditForm;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page Object for the image edition/configuration modal.
 *
 * @version $Id$
 * @since 14.7RC1
 */
public class ImageDialogEditModal extends BaseElement
{
    /**
     * Wait until the modal is loaded.
     *
     * @return the current page object
     */
    public ImageDialogEditModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(By.className("image-editor-modal"));
        getDriver().waitUntilElementDisappears(By.cssSelector(".image-editor-modal .loading"));
        return this;
    }

    /**
     * Click on the insert button to insert the configured image on the editor.
     */
    public void clickInsert()
    {
        By buttonSelector = By.cssSelector(".image-editor-modal .btn-primary");
        clickCloseButton(buttonSelector);
    }

    /**
     * Click on the cancel button to close the modal.
     * @since 14.10.13
     * @since 15.5RC1
     */
    public void clickCancel()
    {
        By buttonSelector = By.cssSelector(".image-editor-modal [data-dismiss='modal']");
        clickCloseButton(buttonSelector);
    }

    private void clickCloseButton(By buttonSelector)
    {
        // Wait for the button to be enabled before clicking.
        // Wait for the button to be hidden before continuing.
        WebElement buttonElement = getDriver().findElement(buttonSelector);
        getDriver().waitUntilElementIsEnabled(buttonElement);
        buttonElement.click();
        getDriver().waitUntilElementDisappears(buttonSelector);
    }

    /**
     * @return the standard edit tab page object
     * @since 15.2
     * @since 14.10.8
     */
    public ImageDialogStandardEditForm switchToStandardTab()
    {
        getDriver().findElement(By.cssSelector(".image-editor-modal .image-editor a[href='#standard']")).click();
        return new ImageDialogStandardEditForm();
    }

    /**
     * @return the advanded edit tab page object
     * @since 15.2
     * @since 14.10.8
     */
    public ImageDialogAdvancedEditForm switchToAdvancedTab()
    {
        getDriver().findElement(By.cssSelector(".image-editor-modal .image-editor a[href='#advanced']")).click();
        return new ImageDialogAdvancedEditForm();
    }
}
