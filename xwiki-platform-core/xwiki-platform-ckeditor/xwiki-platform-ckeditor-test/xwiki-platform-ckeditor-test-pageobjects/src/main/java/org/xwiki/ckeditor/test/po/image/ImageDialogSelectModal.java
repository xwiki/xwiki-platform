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
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogIconSelectForm;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogTreeSelectForm;
import org.xwiki.ckeditor.test.po.image.select.ImageDialogUrlSelectForm;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page Object for the image selection modal.
 *
 * @version $Id$
 * @since 14.7RC1
 */
public class ImageDialogSelectModal extends BaseElement
{
    /**
     * Wait until the modal is loaded.
     *
     * @return the current page object
     */
    public ImageDialogSelectModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(By.className("image-selector-modal"));
        return this;
    }

    /**
     * Click on the select button to select the image and move to the image edition/configuration modal.
     *
     * @return the Page Object instance for the next modal
     */
    public ImageDialogEditModal clickSelect()
    {
        WebElement element = getDriver().findElement(By.cssSelector(".image-selector-modal .btn-primary"));
        getDriver().waitUntilElementIsEnabled(element);
        element.click();
        return new ImageDialogEditModal().waitUntilReady();
    }

    /**
     * Click on the Tree tab to switch to the document tree selection form.
     *
     * @return a document tree selection form page object
     */
    public ImageDialogTreeSelectForm switchToTreeTab()
    {
        return switchTab(".image-selector-modal .image-selector a[href='#documentTree-0']", ImageDialogTreeSelectForm::new);
    }

    /**
     * Click on the Icon tab to switch to the icon selection form.
     *
     * @return an icon selection form page object
     */
    public ImageDialogIconSelectForm switchToIconTab()
    {
        return switchTab(".image-selector-modal .image-selector a[href='#iconTab-0']", ImageDialogIconSelectForm::new);
    }

    /**
     * Click on the Url tab to switch to the url selection form.
     *
     * @return an url selection form page object
     */
    public ImageDialogUrlSelectForm switchToUrlTab()
    {
        return switchTab(".image-selector-modal .image-selector a[href='#urlTab-0']", ImageDialogUrlSelectForm::new);
    }

    private <T> T switchTab(String cssSelector, Supplier<T> supplier)
    {
        By selector = By.cssSelector(cssSelector);
        getDriver().findElement(selector).click();
        // Prevent the test from continuing before the tab is fully switched.
        XWikiWebDriver xWikiWebDriver = getDriver();
        xWikiWebDriver.waitUntilCondition(driver -> {
            try {
                WebElement element = driver.findElement(selector);
                String attribute = element.getAttribute("aria-expanded");
                return attribute == null || attribute.contains("true");
            } catch (NotFoundException e) {
                return false;
            } catch (StaleElementReferenceException e) {
                // The element was removed from DOM in the meantime
                return false;
            }
        });
        return supplier.get();
    }
}
