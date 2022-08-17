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
        return this;
    }

    /**
     * Click on the insert button to insert the configured image on the editor.
     */
    public void clickInsert()
    {
        getDriver().findElement(By.cssSelector(".image-editor-modal .btn-primary")).click();
    }

    /**
     * Click on the caption checkbox field.
     */
    public void clickCaptionCheckbox()
    {
        getDriver().findElement(By.id("imageCaptionActivation")).click();
    }
}
