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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

public class Lightbox extends ViewPage
{
    public boolean waitUntilIsSlideDisplayed(int index)
    {
        try {
            By slideSelector = By.cssSelector("#blueimp-gallery .slide-active");
            getDriver().waitUntilCondition(attributeToBe(slideSelector, "data-index", String.valueOf(index)));
            return getDriver().findElement(slideSelector).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public WebElement getSlide()
    {
        By slideSelector = By.cssSelector("#blueimp-gallery .slide-active");
        return getDriver().findElement(slideSelector);
    }

    public String getSlideIndex()
    {
        By slideSelector = By.cssSelector("#blueimp-gallery .slide-active");
        return getDriver().findElement(slideSelector).getAttribute("data-index");
    }

    public String getCaptionContent()
    {
        By captionSelector = By.cssSelector("#blueimp-gallery .caption");
        return getDriver().findElement(captionSelector).getText();
    }

    public String getTitleContent()
    {
        By titleSelector = By.cssSelector("#blueimp-gallery .title");
        return getDriver().findElement(titleSelector).getText();
    }

    public String getPublisherContent()
    {
        By publisherSelector = By.cssSelector("#blueimp-gallery .publisher");
        return getDriver().findElement(publisherSelector).getText();
    }

    public String getDateContent()
    {
        By dateSelector = By.cssSelector("#blueimp-gallery .date");
        return getDriver().findElement(dateSelector).getText().replace("on", "");
    }

    public WebElement getDownloadElement()
    {
        By downloadSelector = By.cssSelector("#blueimp-gallery .download");
        return getDriver().findElement(downloadSelector);
    }

    public boolean isMissingImageOpen()
    {
        try {
            return getDriver().findElement(By.cssSelector("#blueimp-gallery .slide-error")).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void clickEscape()
    {
        By escapeSelector = By.cssSelector("#blueimp-gallery .escape");
        getDriver().findElement(escapeSelector).click();
    }

    public void clickNext()
    {
        By nextSelector = By.cssSelector("#blueimp-gallery .next");
        getDriver().findElement(nextSelector).click();
    }

    public void clickPrev()
    {
        By prevSelector = By.cssSelector("#blueimp-gallery .prev");
        getDriver().findElement(prevSelector).click();
    }

    public void clickFullscreen()
    {
        By fullscreenSelector = By.cssSelector("#blueimp-gallery .fullscreen");
        getDriver().findElement(fullscreenSelector).click();
    }

    public void clickThumbnail(int index)
    {
        List<WebElement> thumbnails = getDriver().findElements(By.cssSelector("#blueimp-gallery .indicator >li"));
        thumbnails.get(index).click();
    }

    public void clickSlideshow()
    {
        By slideshowSelector = By.cssSelector("#blueimp-gallery .autoPlay");
        getDriver().findElement(slideshowSelector).click();
    }

}
