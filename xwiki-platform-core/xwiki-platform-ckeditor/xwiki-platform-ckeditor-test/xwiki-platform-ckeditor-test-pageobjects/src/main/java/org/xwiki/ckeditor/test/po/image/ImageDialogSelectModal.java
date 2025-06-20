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

import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogIconSelectForm;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogTreeSelectForm;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogUploadSelectForm;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogUrlSelectForm;
import org.xwiki.test.ui.po.BaseModal;

/**
 * Page Object for the image selection modal.
 *
 * @version $Id$
 * @since 14.7RC1
 */
public class ImageDialogSelectModal extends BaseModal
{
    /**
     * Default constructor.
     */
    public ImageDialogSelectModal()
    {
        this(By.className("image-selector-modal"));
    }

    private ImageDialogSelectModal(By selector)
    {
        // This modal is added on demand so we can't rely on the base constructor to remove the "fade in" effect. We
        // have to do the wait ourselves.
        getDriver().waitUntilElementIsVisible(selector);
        this.container = getDriver().findElementWithoutWaiting(selector);
    }

    /**
     * Click on the select button to select the image and move to the image edition/configuration modal.
     *
     * @return the Page Object instance for the next modal
     */
    public ImageDialogEditModal clickSelect()
    {
        // We find the primary button in the footer because the Selection modal has other primary buttons.
        // See XWIKI-21206.
        WebElement element = this.container.findElement(By.cssSelector(".modal-footer .btn-primary"));
        getDriver().waitUntilElementIsEnabled(element);
        element.click();
        return new ImageDialogEditModal();
    }

    /**
     * Click on the Tree tab to switch to the document tree selection form.
     *
     * @return a document tree selection form page object
     */
    public ImageDialogTreeSelectForm switchToTreeTab()
    {
        return switchTab(".image-selector a[href='#documentTree-0']", ImageDialogTreeSelectForm::new);
    }

    /**
     * Click on the Icon tab to switch to the icon selection form.
     *
     * @return an icon selection form page object
     */
    public ImageDialogIconSelectForm switchToIconTab()
    {
        return switchTab(".image-selector a[href='#iconTab-0']", ImageDialogIconSelectForm::new);
    }

    /**
     * Click on the Url tab to switch to the url selection form.
     *
     * @return an url selection form page object
     */
    public ImageDialogUrlSelectForm switchToUrlTab()
    {
        return switchTab(".image-selector a[href='#urlTab-0']", ImageDialogUrlSelectForm::new);
    }

    /**
     * Click on the Upload tab to switch to the upload selection form.
     *
     * @return an upload selection form page object
     * @since 16.1.0RC1
     * @since 15.10.7
     */
    public ImageDialogUploadSelectForm switchToUploadTab()
    {
        return switchTab(".image-selector a[href='#upload-0']", ImageDialogUploadSelectForm::new);
    }

    private <T> T switchTab(String cssSelector, Supplier<T> supplier)
    {
        By selector = By.cssSelector(cssSelector);
        this.container.findElement(selector).click();
        // Prevent the test from continuing before the tab is fully switched.
        getDriver().waitUntilElementContainsAttributeValue(selector, "aria-expanded", "true");
        return supplier.get();
    }
}
