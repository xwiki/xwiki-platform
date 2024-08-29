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
package org.xwiki.ckeditor.test.po.image.edit;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * @version $Id$
 * @since 15.2
 * @since 14.10.8
 */
public class ImageDialogStandardEditForm extends BaseElement
{
    /**
     * The image caption checkbox.
     */
    @FindBy(id = "imageCaptionActivation")
    private WebElement captionCheckbox;

    /**
     * Click on the caption checkbox field.
     */
    public void clickCaptionCheckbox()
    {
        this.captionCheckbox.click();
    }

    /**
     * @return true if the caption checkbox is checked
     * @since 14.10.13
     * @since 15.5RC1
     */
    public boolean isCaptionCheckboxChecked()
    {
        return this.captionCheckbox.isSelected();
    }

    /**
     * @return the list of image styles values proposed in the image styles field
     */
    public Set<String> getListImageStyles()
    {
        return getImageStylesElement()
            .getSuggestions()
            .stream()
            .map(SuggestInputElement.SuggestionElement::getValue)
            .collect(Collectors.toSet());
    }

    /**
     * @return the currently selected value of the image styles field
     */
    public String getCurrentImageStyle()
    {
        return getCurrentImageStyleInternal(SuggestInputElement.SuggestionElement::getValue);
    }

    /**
     * Returns the label of the currently selected value of the image styles field.
     *
     * @return the label of the currently selected value
     * @since 14.10.16
     * @since 15.5.2
     * @since 15.8RC1
     */
    public String getCurrentImageStyleLabel()
    {
        return getCurrentImageStyleInternal(SuggestInputElement.SuggestionElement::getLabel);
    }

    /**
     * @param value the user visible value of the field to select
     * @return the current page object
     */
    public ImageDialogStandardEditForm setImageStyle(String value)
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

    private String getCurrentImageStyleInternal(Function<SuggestInputElement.SuggestionElement, String> getValue)
    {
        return getImageStylesElement()
            .getSelectedSuggestions()
            .stream()
            .findFirst()
            .map(getValue)
            .orElseThrow(() -> new RuntimeException("Unexpected empty suggestions list."));
    }
}
