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
import org.xwiki.ckeditor.test.po.image.edit.ImageDialogAdvancedEditForm;
import org.xwiki.ckeditor.test.po.image.edit.ImageDialogStandardEditForm;
import org.xwiki.test.ui.po.BaseModal;

/**
 * Page Object for the image edition/configuration modal.
 *
 * @version $Id$
 * @since 14.7RC1
 */
public class ImageDialogEditModal extends BaseModal
{
    /**
     * Default constructor.
     */
    public ImageDialogEditModal()
    {
        this(By.className("image-editor-modal"));
    }

    private ImageDialogEditModal(By selector)
    {
        super(selector);

        // This modal is added on demand so we can't rely on the base constructor to remove the "fade in" effect. We
        // have to do the wait ourselves.
        getDriver().waitUntilElementIsVisible(selector);
        this.container = getDriver().findElementWithoutWaiting(selector);
        getDriver().waitUntilElementDisappears(this.container, By.className("loading"));
    }

    /**
     * Click on the insert button to insert the configured image on the editor.
     */
    public void clickInsert()
    {
        this.container.findElement(By.className("btn-primary")).click();
        waitForClosed();
    }

    /**
     * @return the standard edit tab page object
     * @since 15.2
     * @since 14.10.8
     */
    public ImageDialogStandardEditForm switchToStandardTab()
    {
        this.container.findElement(By.cssSelector(".image-editor a[href='#standard']")).click();
        return new ImageDialogStandardEditForm();
    }

    /**
     * @return the advanded edit tab page object
     * @since 15.2
     * @since 14.10.8
     */
    public ImageDialogAdvancedEditForm switchToAdvancedTab()
    {
        this.container.findElement(By.cssSelector(".image-editor a[href='#advanced']")).click();
        return new ImageDialogAdvancedEditForm();
    }
}
