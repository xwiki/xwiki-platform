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
package org.xwiki.image.test.po;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ViewPage;

public class ImagePage extends ViewPage
{
    public void hoverImage(int index)
    {
        WebElement image = getImage(index);
        assertTrue(image.isDisplayed());

        Actions action = new Actions(getDriver().getWrappedDriver());
        action.moveToElement(image).build().perform();

        // Workaround to manually display the popup since in firefox the hover is not always triggered even if the mouse
        // is moved over the image.
        showPopup(image);
    }

    private void showPopup(WebElement image)
    {
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        js.executeScript("jQuery(arguments[0]).popover('show');", image);
    }

    public WebElement getImage(int index)
    {
        By xpath = By.xpath(".//*[@id='xwikicontent']//img");
        getDriver().waitUntilElementsAreVisible(new By[] {xpath}, true);
        List<WebElement> images = getDriver().findElements(xpath);
        return images.get(index);
    }

    public void openLightboxAtImage(int index)
    {
        hoverImage(index);

        By lightboxPath = By.cssSelector(".popover .openLightbox");
        getDriver().waitUntilElementIsVisible(lightboxPath);
        getDriver().findElementWithoutWaiting(lightboxPath).click();

        By className = By.className("blueimp-gallery-display");
        getDriver().waitUntilElementIsVisible(className);
        assertTrue(getDriver().findElement(className).isDisplayed());
    }

    public boolean isToolbarOpen()
    {
        try {
            By popoverSelector = By.cssSelector(".popover");
            return getDriver().findElementWithoutWaiting(popoverSelector).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isLightboxOpen()
    {
        try {
            By className = By.className("blueimp-gallery-display");
            return getDriver().findElement(className).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public WebElement getToolbarDownload()
    {
        By downloadSelector = By.cssSelector(".popover .imageDownload");
        return getDriver().findElementWithoutWaiting(downloadSelector);
    }

    public void downloadFile()
    {
        By path = By.cssSelector(".popover .imageDownload");
        getDriver().findElement(path).click();
    }

    private File getFileToUpload(String testResourcePath, String filename)
    {
        return new File(testResourcePath, "ImageIT/" + filename);
    }

    public String attachFile(String testResourcePath, String image)
    {
        AttachmentsPane attachmentsPane = this.openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(getFileToUpload(testResourcePath, image).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(image);
        assertTrue(attachmentsPane.attachmentExistsByFileName(image));

        return attachmentsPane.getDateOfLastUpload(image);
    }
}
