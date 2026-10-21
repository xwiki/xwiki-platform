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

import java.util.List;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
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
     * @return the input the resource reference is typed in, which is the input created by the suggestion widget when
     *         the selected resource type has a suggester, and the resource reference input otherwise
     */
    private WebElement getDisplayedResourceReferenceInput()
    {
        List<WebElement> suggesterInputs = getResourcePicker().findElements(cssSelector(".ts-control > input"));
        return suggesterInputs.isEmpty() ? getResourceReferenceInput() : suggesterInputs.get(0);
    }

    /**
     * @return the first input displayed above the resource picker, which is the link display text
     */
    private WebElement getDisplayTextInput()
    {
        return getContainer().findElements(cssSelector(".cke_dialog_page_contents input.cke_dialog_ui_input_text"))
            .stream().filter(WebElement::isDisplayed).findFirst().orElseThrow();
    }

    /**
     * Moves the keyboard focus to the display text field, then presses the tab key once.
     *
     * @return {@code true} if the focus landed on the input the resource reference is typed in
     * @since 18.8.0RC1
     */
    public boolean isResourceReferenceInputNextInTabOrder()
    {
        WebElement displayTextInput = getDisplayTextInput();
        displayTextInput.click();
        displayTextInput.sendKeys(Keys.TAB);
        return getDriver().switchTo().activeElement().equals(getDisplayedResourceReferenceInput());
    }

    /**
     * @return {@code true} if the field label is bound to the input the resource reference is typed in
     * @since 18.8.0RC1
     */
    public boolean isLabelBoundToResourceReferenceInput()
    {
        String labelTarget =
            getResourcePicker().findElement(xpath("preceding-sibling::label")).getDomAttribute("for");
        return labelTarget != null
            && labelTarget.equals(getDisplayedResourceReferenceInput().getDomAttribute("id"));
    }

    /**
     * @return the resource currently selected in the resource picker, e.g.: {@code "doc:Space.Page"} or
     *         {@code "attach:Space.Page@image.png}
     * @since 18.2.0RC1
     */
    public String getSelectedResource()
    {
        return getResourcePickerInput().getDomProperty(ATTRIBUTE_VALUE);
    }

    /**
     * Wait until the given resource is selected in the resource picker.
     *
     * @param expectedResource the resource expected to be selected, e.g.: {@code "doc:Space.Page"}
     * @return the current page object
     * @since 18.4.6
     * @since 18.8.0RC1
     */
    public LinkDialog waitForSelectedResource(String expectedResource)
    {
        getDriver().waitUntilCondition(driver -> expectedResource.equals(getSelectedResource()));
        return this;
    }

    /**
     * @return the link label, from the display text field
     * @since 18.4.6
     * @since 18.8.0RC1
     */
    public String getDisplayText()
    {
        return getDisplayTextInput().getDomProperty(ATTRIBUTE_VALUE);
    }

    /**
     * Sets the link label.
     *
     * @param displayText the link label to type in the display text field
     * @return the current page object
     * @since 18.4.6
     * @since 18.8.0RC1
     */
    public LinkDialog setDisplayText(String displayText)
    {
        WebElement displayTextInput = getDisplayTextInput();
        displayTextInput.clear();
        displayTextInput.sendKeys(displayText);
        return this;
    }

    /**
     * Expands the link options (the anchor and the query string), unless they are expanded already. Note that the
     * link dialog is reused, so the options stay expanded the next time the dialog is opened.
     *
     * @return the current page object
     * @since 18.4.6
     * @since 18.8.0RC1
     */
    public LinkDialog expandOptions()
    {
        // The arrow points down when the options are expanded.
        if (getContainer().findElements(cssSelector(".linkOptionsToggle .arrow-down")).isEmpty()) {
            getContainer().findElement(cssSelector("button.linkOptionsToggle")).click();
        }
        return this;
    }

    /**
     * Sets the anchor of the link. The link options need to be expanded first.
     *
     * @param anchor the anchor to set, or an empty string to remove the anchor
     * @return the current page object
     * @see #expandOptions()
     * @since 18.4.6
     * @since 18.8.0RC1
     */
    public LinkDialog setAnchor(String anchor)
    {
        return setLinkOption(1, anchor);
    }

    /**
     * Sets the query string of the link. The link options need to be expanded first.
     *
     * @param queryString the query string to set, or an empty string to remove the query string
     * @return the current page object
     * @see #expandOptions()
     * @since 18.4.6
     * @since 18.8.0RC1
     */
    public LinkDialog setQueryString(String queryString)
    {
        return setLinkOption(0, queryString);
    }

    /**
     * @param index the position of the input among the displayed link options; the link dialog declares the query
     *            string before the anchor, so 0 is the query string and 1 is the anchor
     * @param value the value to set, or an empty string to clear the input
     * @return the current page object
     */
    private LinkDialog setLinkOption(int index, String value)
    {
        WebElement input =
            getContainer().findElements(cssSelector(".linkOptions input.cke_dialog_ui_input_text")).get(index);
        input.clear();
        if (!value.isEmpty()) {
            input.sendKeys(value);
        }
        return this;
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

    /**
     * Clicks the OK button, expecting the dialog to reject the submission, and accepts the alert that shows the
     * validation message.
     *
     * @return the validation message shown to the user
     * @since 18.4.6
     * @since 18.8.0RC1
     */
    public String submitExpectingValidationFailure()
    {
        clickOk();
        // The validation message is shown asynchronously because the link label is validated first, and validating it
        // requires a request to generate the default label.
        Alert alert = getDriver().waitUntilCondition(ExpectedConditions.alertIsPresent());
        String message = alert.getText();
        alert.accept();
        return message;
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
