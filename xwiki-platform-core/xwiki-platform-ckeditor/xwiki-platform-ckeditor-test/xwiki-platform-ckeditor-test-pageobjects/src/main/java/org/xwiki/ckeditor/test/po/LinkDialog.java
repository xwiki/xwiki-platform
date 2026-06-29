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

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.SuggestInputElement;

import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.xpath;

/**
 * Page object for the CKEditor link dialog used to insert or edit links.
 *
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
 */
public class LinkDialog extends CKEditorDialog
{
    /**
     * Set the given value on the resource search field.
     *
     * @param value the value to use to search for a resource (i.e., page or attachment)
     * @return the current page object
     */
    public LinkDialog setResourceReference(String value)
    {
        WebElement resourceReferenceInput = getResourceReferenceInput();
        if (SuggestInputElement.isAvailable(getDriver(), resourceReferenceInput)) {
            new SuggestInputElement(resourceReferenceInput).clear().sendKeys(value).waitForSuggestions();
        } else {
            resourceReferenceInput.clear();
            resourceReferenceInput.sendKeys(value);
        }
        return this;
    }

    /**
     * @return the text input that holds the resource reference
     */
    private WebElement getResourceReferenceInput()
    {
        WebElement resourcePicker = getResourcePicker();
        return resourcePicker.findElement(cssSelector("input.resourceReference"));
    }

    /**
     * @return the suggest input element to use to search for and select resources
     * @since 18.2.0RC1
     */
    public SuggestInputElement getResourceSuggestInput()
    {
        return new SuggestInputElement(getResourceReferenceInput());
    }

    /**
     * @return the resource currently selected in the resource picker, e.g.: {@code "doc:Space.Page"} or
     *         {@code "attach:Space.Page@image.png}
     * @since 18.2.0RC1
     */
    public String getSelectedResource()
    {
        return getResourcePickerInput().getDomAttribute(ATTRIBUTE_VALUE);
    }

    private WebElement getResourcePickerInput()
    {
        return getResourcePicker().findElement(xpath("preceding-sibling::input"));
    }

    public LinkDialog createLinkOfNewPage(boolean exactReference)
    {
        String label = (exactReference) ? "Create with exact reference..." : "Create new page...";
        getResourceSuggestInput().selectByVisibleText(label);
        return this;
    }

    public LinkPickerModal openLinkPickerModal()
    {
        getResourcePicker().findElement(By.cssSelector("button.resourceType")).click();
        return new LinkPickerModal(By.cssSelector(".entity-resource-picker-modal.modal"));
    }

    /**
     * Select a resource type for the resource picker (e.g., {@code "doc"}, or {@code "attachment"}).
     *
     * @param resourceType the resource type to select
     * @return the current page object
     */
    public LinkDialog setResourceType(String resourceType)
    {
        WebElement resourcePicker = getResourcePicker();
        resourcePicker.findElement(By.cssSelector(".dropdown-toggle")).click();
        resourcePicker
            .findElement(By.cssSelector(".input-group-btn.open .resourceTypes a[data-id=\"" + resourceType + "\"]"))
            .click();
        return this;
    }

    private WebElement getResourcePicker()
    {
        return getContainer().findElement(cssSelector(".resourcePicker"));
    }

    @Override
    public void submit()
    {
        //maybeBlurSuggestInput();

        super.submit();
    }

    private void maybeBlurSuggestInput()
    {
        WebElement resourceReferenceInput = getResourceReferenceInput();
        if (SuggestInputElement.isAvailable(getDriver(), resourceReferenceInput)) {
            new SuggestInputElement(resourceReferenceInput).sendKeys(Keys.TAB);
        }
    }
}
