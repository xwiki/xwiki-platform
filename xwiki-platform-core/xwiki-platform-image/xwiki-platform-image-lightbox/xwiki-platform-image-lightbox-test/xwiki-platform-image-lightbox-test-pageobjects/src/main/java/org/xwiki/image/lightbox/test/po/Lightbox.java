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

import static org.openqa.selenium.support.ui.ExpectedConditions.attributeToBe;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The image lightbox gallery.
 * 
 * @version $Id$
 * @since 14.1RC1
 */
public class Lightbox extends BaseElement
{
    public Lightbox()
    {
        waitUntilReady();
    }

    public boolean isDisplayed()
    {
        try {
            return getDriver().findElement(By.className("blueimp-gallery-display")).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean waitForSlide(int index)
    {
        try {
            int thumbnailIndex = index + 1;
            By thumbnailSelector = By.xpath(
                ".//div[@id=\"blueimp-gallery\"]/ol[contains(@class, \"indicator\")]/li[" + thumbnailIndex + " ]");
            getDriver().waitUntilElementIsVisible(thumbnailSelector);
            getDriver().waitUntilElementHasAttributeValue(thumbnailSelector, "class", "active");

            By slideSelector = By.cssSelector("#blueimp-gallery .slide-active");
            getDriver().waitUntilCondition(attributeToBe(slideSelector, "data-index", String.valueOf(index)));

            return getDriver().findElement(slideSelector).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public WebElement getSlideElement()
    {
        By slideSelector = By.cssSelector("#blueimp-gallery .slide-active");
        return getDriver().findElement(slideSelector);
    }

    public String getSlideIndex()
    {
        By slideSelector = By.cssSelector("#blueimp-gallery .slide-active");
        return getDriver().findElement(slideSelector).getAttribute("data-index");
    }

    public String getCaption()
    {
        By captionSelector = By.cssSelector("#blueimp-gallery .caption");
        // Wait until the element is visible before continuing. Otherwise, the non-initialized element with an
        // empty value can be used, with an empty text, leading to flaky tests.
        getDriver().waitUntilElementIsVisible(captionSelector);
        return getDriver().findElement(captionSelector).getText();
    }

    public String getTitle()
    {
        By titleSelector = By.cssSelector("#blueimp-gallery .title");
        return getDriver().findElement(titleSelector).getText();
    }

    public String getPublisher()
    {
        By publisherSelector = By.cssSelector("#blueimp-gallery .publisher");
        return getDriver().findElement(publisherSelector).getText();
    }

    public WebElement getCopyImageIdButton()
    {
        return getDriver().findElement(By.cssSelector("#blueimp-gallery .copyImageId"));
    }

    public String getImageId()
    {
        return getCopyImageIdButton().getAttribute("data-imageId");
    }

    public String getDate()
    {
        By dateSelector = By.cssSelector("#blueimp-gallery .date");
        return getDriver().findElement(dateSelector).getText().replace("on ", "");
    }

    public WebElement getDownloadButton()
    {
        By downloadSelector = By.cssSelector("#blueimp-gallery .download");
        return getDriver().findElement(downloadSelector);
    }

    public boolean isImageMissing()
    {
        try {
            return getDriver().findElement(By.cssSelector("#blueimp-gallery .slide-error")).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void close()
    {
        By escapeSelector = By.cssSelector("#blueimp-gallery .escape");
        getDriver().findElement(escapeSelector).click();
        getDriver().waitUntilElementDisappears(By.className("blueimp-gallery-display"));
    }

    public void next(int nextSlideIndex)
    {
        By nextSelector = By.cssSelector("#blueimp-gallery .next");
        getDriver().findElement(nextSelector).click();

        waitForSlide(nextSlideIndex);
    }

    public void previous(int nextSlideIndex)
    {
        By prevSelector = By.cssSelector("#blueimp-gallery .prev");
        getDriver().findElement(prevSelector).click();

        waitForSlide(nextSlideIndex);
    }

    public boolean toggleFullscreen()
    {
        boolean waitForFullscreenOff = isFullscreenOn();

        By fullscreenSelector = By.cssSelector("#blueimp-gallery .fullscreen");
        getDriver().findElement(fullscreenSelector).click();

        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return waitForFullscreenOff ? !isFullscreenOn() : isFullscreenOn();
            }

        });

        return isFullscreenOn();
    }

    private boolean isFullscreenOn()
    {
        JavascriptExecutor js = ((JavascriptExecutor) getDriver());
        WebElement fullscreen =
            (WebElement) js.executeScript("var element = document.fullscreenElement; return element");

        return fullscreen != null && fullscreen.isDisplayed();
    }

    public void clickThumbnail(int index)
    {
        List<WebElement> thumbnails = getDriver().findElements(By.cssSelector("#blueimp-gallery .indicator >li"));
        thumbnails.get(index).click();

        waitForSlide(index);
    }

    public void toggleSlideshow()
    {
        By slideshowSelector = By.cssSelector("#blueimp-gallery .autoPlay");
        getDriver().findElement(slideshowSelector).click();
    }

    private Lightbox waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(By.className("blueimp-gallery-display"));

        return this;
    }
}
