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

import java.util.NoSuchElementException;
import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.openqa.selenium.By.cssSelector;

/**
 * Page object for the CKEditor link dialog used to insert or edit links.
 *
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
 */
public class LinkDialog extends CKEditorDialog
{
    private static final By DROPDOWN_ITEM_SELECTOR = cssSelector(".dropdown-menu .dropdown-item");

    /**
     * Set the given value on the resource value search field.
     *
     * @param value the value to use to search for a resource (i.e., page or attachment)
     * @return the current page object
     */
    public LinkDialog setResourceValue(String value)
    {
        return setResourceValue(value, true);
    }

    /**
     * Set the given value on the resource value search field.
     *
     * @param value the value to use to search for a resource (i.e., page or attachment)
     * @param wait when {@code true}, wait for the dropdown to be shown before continuing
     * @return the current page object
     */
    public LinkDialog setResourceValue(String value, boolean wait)
    {
        WebElement resourcePicker = getResourcePicker();
        WebElement element = resourcePicker.findElement(cssSelector(" input.resourceReference"));
        element.clear();
        element.sendKeys(value);
        if (wait) {
            getDriver().waitUntilElementsAreVisible(resourcePicker, new By[] {DROPDOWN_ITEM_SELECTOR}, true);
        }
        return this;
    }

    /**
     * @return the resource type currently selected in the resource picker (e.g., {@code "doc"})
     */
    public String getSelectedResourceType()
    {
        return getResourceDisplay().getAttribute("data-resourcetype");
    }

    /**
     * @return the resource reference currently selected in the resource picker (e.g., {@code "Sandbox.WebHome"})
     */
    public String getSelectedResourceReference()
    {
        return getResourceDisplay().getAttribute("data-resourcereference");
    }

    private WebElement getResourceDisplay()
    {
        return getResourcePicker().findElement(cssSelector(".resourceDisplay"));
    }

    /**
     * Click on one of the choices proposed in the dropdown after using {@link #setResourceValue(String)}, based on its
     * hint and label.
     *
     * @param hint the hint of the resource (e.g, {@code "ParentSpace / ChildSpace"} for a page)
     * @param label the label of the resource (e.g., {@code "PageName"} for a page, or the attachment name for an
     *            attachment)
     * @return the current page object
     * @throws NoSuchElementException in case of issue when looking for the item by its hint and label
     */
    public LinkDialog selectPageItem(String hint, String label)
    {
        getResourcePicker().findElements(DROPDOWN_ITEM_SELECTOR).stream()
            .filter(element -> Objects.equals(element.findElement(cssSelector(".resource-hint")).getText(), hint)
                && Objects.equals(element.findElement(cssSelector(".resource-label")).getText(), label))
            .findFirst().orElseThrow(() -> new NoSuchElementException(String.format("%s / %s not found", hint, label)))
            .click();
        return this;
    }

    public LinkDialog createLinkOfNewPage(boolean exactReference)
    {
        String resourceLabelName = (exactReference) ? "Create with exact reference..." : "Create in current space...";
        getResourcePicker().findElements(DROPDOWN_ITEM_SELECTOR).stream()
            .filter(element ->
                Objects.equals(element.findElement(cssSelector(".resource-label")).getText(), resourceLabelName))
            .findFirst().orElseThrow(() -> new NoSuchElementException(String.format("%s not found", resourceLabelName)))
            .click();
        return this;
    }

    public LinkPickerModal openDocumentPicker()
    {
        getContainer().findElement(By.cssSelector("button.resourceType")).click();
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
}
