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
package org.xwiki.image.lightbox.test.po;

import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The toolbar opened on image hover.
 * 
 * @version $Id$
 * @since 14.1RC1
 */
public class ImagePopover extends BaseElement
{
    public Optional<Lightbox> openLightbox(int index)
    {
        try {
            By lightboxButtonSelector = By.cssSelector(".popover .openLightbox");
            getDriver().waitUntilElementIsVisible(lightboxButtonSelector);
            getDriver().findElementWithoutWaiting(lightboxButtonSelector).click();
            getDriver().waitUntilElementIsVisible(By.className("blueimp-gallery-display"));

            return Optional.of(new Lightbox());
        } catch (TimeoutException e) {
            return Optional.empty();
        }
    }

    public boolean isImagePopoverDisplayed()
    {
        try {
            By popoverSelector = By.cssSelector(".popover");
            return getDriver().findElementWithoutWaiting(popoverSelector).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public WebElement getDownloadButton()
    {
        By downloadSelector = By.cssSelector(".popover .imageDownload");
        return getDriver().findElement(downloadSelector);
    }
}
