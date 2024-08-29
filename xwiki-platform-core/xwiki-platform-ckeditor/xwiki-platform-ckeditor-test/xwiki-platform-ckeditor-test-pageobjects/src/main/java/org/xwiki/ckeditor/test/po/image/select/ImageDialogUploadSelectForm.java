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
package org.xwiki.ckeditor.test.po.image.select;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.FileDetector;
import org.openqa.selenium.remote.LocalFileDetector;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object for the upload selection form of the image dialog.
 * 
 * @version $Id$
 * @since 16.1.0RC1
 * @since 15.10.7
 */
public class ImageDialogUploadSelectForm extends BaseElement
{
    /**
     * Uploads the specified image so that it can be selected.
     * 
     * @param path the page to the image to upload
     * @throws URISyntaxException if the specified path is not a valid (relative) URI
     */
    public void upload(String path) throws URISyntaxException
    {
        FileDetector originalFileDetector = getDriver().getFileDetector();
        try {
            getDriver().setFileDetector(new LocalFileDetector());
            WebElement fileInput = getDriver().findElementWithoutWaitingWithoutScrolling(By.id("fileUploadField"));
            fileInput.sendKeys(getAbsolutePath(path));
            WebElement uploadButton = getDriver()
                .findElementWithoutWaitingWithoutScrolling(By.cssSelector("input[type=submit][value=Upload]"));
            uploadButton.click();
            waitForNotificationSuccessMessage("File upload succeeded.");
        } finally {
            getDriver().setFileDetector(originalFileDetector);
        }
    }

    private String getAbsolutePath(String path) throws URISyntaxException
    {
        URL resource = getClass().getResource(path);
        File file = Paths.get(resource.toURI()).toFile();
        return file.getAbsolutePath();
    }
}
