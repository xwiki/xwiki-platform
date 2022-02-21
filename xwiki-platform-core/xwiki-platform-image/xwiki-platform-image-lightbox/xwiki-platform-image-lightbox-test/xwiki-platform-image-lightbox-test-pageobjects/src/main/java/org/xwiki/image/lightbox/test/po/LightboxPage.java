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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Actions for interacting with page images and their attached popover.
 * 
 * @version $Id$
 * @since 14.1RC1
 */
public class LightboxPage extends ViewPage
{
    /**
     * Make sure that the javascript is also loaded after page refresh since lightbox actions depend on it.
     */
    public void reloadPage()
    {
        getDriver().navigate().refresh();
        waitUntilPageJSIsLoaded();
    }

    public Optional<ImagePopover> hoverImage(int index)
    {
        try {
            WebElement image = getImageElement(index);
            assertTrue(image.isDisplayed());

            Actions action = new Actions(getDriver().getWrappedDriver());
            action.moveToElement(image).build().perform();

            // Workaround to manually display the popup since in firefox the hover is not always triggered even if the
            // mouse is moved over the image.
            showPopup(image);

            return Optional.of(new ImagePopover());
        } catch (TimeoutException e) {
            return Optional.empty();
        }
    }

    private void showPopup(WebElement image)
    {
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        js.executeScript("jQuery(arguments[0]).popover('show');", image);
    }

    public WebElement getImageElement(int index)
    {
        By selector = By.cssSelector("#xwikicontent img");
        List<WebElement> images = getDriver().findElements(selector);
        return images.get(index);
    }

    public Lightbox openLightboxAtImage(int index)
    {
        return hoverImage(index).get().openLightbox(index);
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
