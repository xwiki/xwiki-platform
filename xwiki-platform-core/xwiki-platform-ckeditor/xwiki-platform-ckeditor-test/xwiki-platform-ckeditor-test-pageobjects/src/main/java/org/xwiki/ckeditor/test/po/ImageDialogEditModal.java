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
package org.xwiki.ckeditor.test.po;

import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.SuggestInputElement.SuggestionElement;

/**
 * Page Object for the image edition/configuration modal.
 *
 * @version $Id$
 * @since 14.7RC1
 */
public class ImageDialogEditModal extends BaseElement
{
    /**
     * Wait until the modal is loaded.
     *
     * @return the current page object
     */
    public ImageDialogEditModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(By.className("image-editor-modal"));
        return this;
    }

    /**
     * Click on the insert button to insert the configured image on the editor.
     */
    public void clickInsert()
    {
        // Wait for the button to be enabled before clicking.
        // Wait for the button to be hidden before continuing.
        By buttonSelector = By.cssSelector(".image-editor-modal .btn-primary");
        WebElement buttonElement = getDriver().findElement(buttonSelector);
        getDriver().waitUntilElementIsEnabled(buttonElement);
        buttonElement.click();
        getDriver().waitUntilElementDisappears(buttonSelector);
    }

    /**
     * Click on the caption checkbox field.
     */
    public void clickCaptionCheckbox()
    {
        getDriver().findElement(By.id("imageCaptionActivation")).click();
    }

    /**
     * @return the list of image styles values proposed in the image styles field
     * @since 14.8RC1
     */
    public Set<String> getListImageStyles()
    {
        return getImageStylesElement()
            .getSuggestions()
            .stream()
            .map(SuggestionElement::getValue)
            .collect(Collectors.toSet());
    }

    /**
     * @return the currently selected value of the image styles field
     * @since 14.8RC1
     */
    public String getCurrentImageStyle()
    {
        return getImageStylesElement()
            .getSelectedSuggestions()
            .stream()
            .findFirst()
            .map(SuggestionElement::getValue)
            .orElseThrow(() -> new RuntimeException("Unexpected empty suggestions list."));
    }

    /**
     * @param value the user visible value of the field to select
     * @return the current page object
     * @since 14.8RC1
     */
    public ImageDialogEditModal setImageStyle(String value)
    {
        getImageStylesElement().selectByVisibleText(value);
        return this;
    }

    private SuggestInputElement getImageStylesElement()
    {
        WebElement element = getDriver().findElement(By.id("imageStyles"));
        SuggestInputElement suggestInputElement = new SuggestInputElement(element);
        suggestInputElement.click().waitForSuggestions();
        return suggestInputElement;
    }
}
