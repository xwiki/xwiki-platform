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
 * Models the wizard step that allows us to select an image from those attached to the selected page. This wizard step
 * is aggregated by the image selection wizard step which is accessible when inserting or editing an image with the
 * WYSIWYG content editor.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class CurrentPageImageSelectPane extends WizardStepElement
{
    /**
     * The select button.
     */
    @FindBy(xpath = "//button[. = 'Select']")
    private WebElement selectButton;

    /**
     * The option to upload a new image to the current document.
     */
    @FindBy(className = "xNewImagePreview")
    private WebElement uploadNewImageOption;

    @Override
    public CurrentPageImageSelectPane waitToLoad()
    {
        super.waitToLoad();
        getDriver().waitUntilElementIsVisible(By.className("xImagesSelector"));
        return this;
    }

    /**
     * Moves forward to the upload image wizard step.
     * 
     * @return the pane used to upload a new image to the selected document
     */
    public UploadImagePane uploadImage()
    {
        uploadNewImageOption.click();
        selectButton.click();
        return new UploadImagePane().waitToLoad();
    }
}
