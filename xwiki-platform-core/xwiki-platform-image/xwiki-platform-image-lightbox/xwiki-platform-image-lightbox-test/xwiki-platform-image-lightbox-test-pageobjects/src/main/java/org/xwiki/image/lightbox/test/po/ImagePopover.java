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

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The toolbar opened on image hover.
 *
 * @version $Id$
 * @since 14.1RC1
 */
public class ImagePopover extends BaseElement
{
    /**
     * The image this popover is associated with, used to re-trigger the hover while waiting for the popover to
     * show up. May be {@code null} when the popover is not tied to a specific image.
     */
    private final WebElement image;

    /**
     * @param image the image this popover is associated with, used to re-trigger the hover in
     *     {@link #waitUntilReady()}
     */
    public ImagePopover(WebElement image)
    {
        this.image = image;
    }

    /**
     * Click on the open lightbox action of the image popover.
     *
     * @return a lightbox page object
     */
    public Lightbox openLightbox()
    {
        // We need to wait for the element to appear as the popover is displayed only after a delay.
        getDriver().findElement(By.cssSelector(".popover .openLightbox")).click();

        return new Lightbox();
    }

    public String getImageId()
    {
        return getCopyImageIdButton().getAttribute("data-imageId");
    }

    public WebElement getCopyImageIdButton()
    {
        return getDriver().findElement(By.cssSelector(".popover .copyImageId"));
    }

    public WebElement getDownloadButton()
    {
        By downloadSelector = By.cssSelector(".popover .imageDownload");
        return getDriver().findElement(downloadSelector);
    }

    public WebElement getImagePermalinkButton()
    {
        return getDriver().findElement(By.cssSelector(".popover .permalink"));
    }

    /**
     * Wait for a popover to be visible and continue.
     *
     * @return the current page object
     * @throws TimeoutException if no popover is found
     */
    public ImagePopover waitUntilReady()
    {
        By popover = By.cssSelector("#imagePopoverContainer .popover");
        XWikiWebDriver driver = getDriver();
        TimeoutException lastException = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            if (this.image != null) {
                // Re-trigger a real mouse move over the image so the lightbox's "show popover after the mouse
                // stops moving" timer (re)starts. The lightbox JavaScript attaches its mousemove handler
                // asynchronously (RequireJS), so the initial hover's move can be lost; the mouse being then
                // stationary, no further move would ever fire and the popover would never show. We move once per
                // attempt and then wait: moving on every poll cycle would instead keep clearing the show timer.
                new Actions(driver.getWrappedDriver())
                    .moveToElement(this.image, (attempt % 2 == 0) ? 1 : -1, 0).perform();
            }
            try {
                driver.waitUntilElementIsVisible(popover, 2);
                return this;
            } catch (TimeoutException e) {
                lastException = e;
            }
        }
        throw lastException;
    }
}
