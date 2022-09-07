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

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Actions for interacting with page images and their attached popover.
 * 
 * @version $Id$
 * @since 14.1RC1
 */
public class LightboxPage extends ViewPage
{
    public Optional<ImagePopover> hoverImage(int index)
    {
        try {
            WebElement image = getImageElement(index);
            assertTrue(image.isDisplayed());

            Actions action = new Actions(getDriver().getWrappedDriver());
            action.moveToElement(image).build().perform();

            return Optional.of(new ImagePopover());
        } catch (TimeoutException e) {
            return Optional.empty();
        }
    }

    public WebElement getImageElement(int index)
    {
        By selector = By.cssSelector("#xwikicontent img");
        List<WebElement> images = getDriver().findElements(selector);
        return images.get(index);
    }

    /**
     * Get the nth image of the document, move the mouse over the image, waits for the popover toi be displayed and
     * click on the open lightbox action.
     *
     * @param index the index of the image to open in the lightbox
     * @return a lightbox page object
     */
    public Lightbox openLightboxAtImage(int index)
    {
        return hoverImage(index).get().openLightbox();
    }

    private File getFileToUpload(String testResourcePath, String filename)
    {
        return new File(testResourcePath, "ImageIT/" + filename);
    }

    public String attachFile(String testResourcePath, String image)
    {
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(getFileToUpload(testResourcePath, image).getAbsolutePath());
        attachmentsPane.waitForUploadToFinish(image);
        assertTrue(attachmentsPane.attachmentExistsByFileName(image));

        return attachmentsPane.getDateOfLastUpload(image);
    }
}
