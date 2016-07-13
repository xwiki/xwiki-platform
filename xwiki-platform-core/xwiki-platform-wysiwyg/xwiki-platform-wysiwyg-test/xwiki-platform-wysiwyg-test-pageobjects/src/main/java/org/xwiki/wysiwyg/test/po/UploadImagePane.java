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
package org.xwiki.wysiwyg.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Models the image upload wizard step that is accessible when inserting or editing an image with the WYSIWYG editor.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class UploadImagePane extends WizardStepElement
{
    /**
     * The file input used to specify the path to the image to upload.
     */
    @FindBy(name = "filepath")
    private WebElement fileInput;

    /**
     * The button that moves the wizard to the image configuration step.
     */
    @FindBy(xpath = "//button[. = 'Image Settings']")
    private WebElement imageSettingsButton;

    /**
     * The insert image button (which skips the image configuration step).
     */
    @FindBy(xpath = "//button[. = 'Insert Image']")
    private WebElement insertImageButton;

    @Override
    public UploadImagePane waitToLoad()
    {
        super.waitToLoad();
        getDriver().waitUntilElementIsVisible(By.className("xUploadPanel"));
        return this;
    }

    /**
     * Fills the URL with the specified image path.
     * 
     * @param imagePath the path to the image to upload in URL form
     */
    public void setImageToUpload(String imagePath)
    {
        fileInput.sendKeys(imagePath);
    }

    /**
     * Clicks on the "Image Settings" button, waits for the image to be uploaded and then for the image configuration
     * pane to be loaded.
     * 
     * @return the pane used to configure the uploaded image before inserting it into the content
     */
    public ImageConfigPane configureImage()
    {
        imageSettingsButton.click();
        return new ImageConfigPane().waitToLoad();
    }

    /**
     * Clicks on the "Insert Image" button, waits for the image to be uploaded and then for the image wizard to close.
     */
    public void insertImage()
    {
        insertImageButton.click();
        // TODO: Wait for the wizard to close.
    }
}
