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

import java.util.List;
import java.util.function.Consumer;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Page Object for the macro edition modal.
 *
 * @version $Id$
 * @since 14.9
 */
public class MacroDialogEditModal extends BaseElement
{
    /**
     * We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
     */
    private static final By MACRO_NAME = By.cssSelector("[class*=-editor-modal] .macro-name");

    /**
     * We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
     */
    private static final By SUBMIT_BUTTON = By.cssSelector("[class*=-editor-modal] .modal-footer .btn-primary");

    /**
     * The parameters a user can choose from to provide a macro feature, with the feature name as argument.
     */
    private static final String FEATURE_CHOICES = "[class*=-editor-modal] .feature-container#feature-%s";

    private static final String DATA_ID = "data-id";

    private final Consumer<Runnable> submitHandler;

    /**
     * Creates a page object whose {@link #clickSubmit()} only clicks the submit button.
     */
    public MacroDialogEditModal()
    {
        this(Runnable::run);
    }

    /**
     * Creates a page object whose {@link #clickSubmit()} lets the editor that opened the modal wait for the result of
     * the submission, which only that editor knows (e.g. CKEditor refreshes its content to render the macro).
     *
     * @param submitHandler receives the action that clicks the submit button, runs it and waits for its result
     * @since 17.10.14
     * @since 18.4.7
     * @since 18.9.0RC1
     */
    public MacroDialogEditModal(Consumer<Runnable> submitHandler)
    {
        this.submitHandler = submitHandler;
    }

    /**
     * Wait until the macro selection edition is loaded.
     *
     * @return the current page object
     */
    public MacroDialogEditModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(MACRO_NAME);
        return this;
    }

    /**
     * @return the name of the macro being edited
     * @since 18.3.0RC1
     */
    public String getMacroName()
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(MACRO_NAME).getText();
    }

    /**
     * Set the value of a macro parameter.
     *
     * @param name the macro parameter name
     * @param value the macro parameter value
     * @return this modal
     * @since 15.10.6
     * @since 16.0.0RC1
     */
    public MacroDialogEditModal setMacroParameter(String name, CharSequence... value)
    {
        WebElement parameterInput = getMacroParameterInput(name);
        parameterInput.clear();
        parameterInput.sendKeys(value);
        return this;
    }

    /**
     * Check or uncheck the checkbox parameter.
     * 
     * @param name the name of the parameter.
     * @param value the value to give.
     * @return this modal
     * @since 17.6.0RC1
     */
    public MacroDialogEditModal setMacroParameterCheckbox(String name, boolean value)
    {
        WebElement parameterInput = getMacroParameterInput(name);
        boolean selected = parameterInput.isSelected();
        if (value != selected) {
            parameterInput.click();
        }
        return this;
    }

    /**
     * Retrieves the value of a macro parameter from the macro editor modal.
     * 
     * @param name the macro parameter name
     * @return the value of the specified macro parameter
     * @since 15.10.6
     * @since 16.0.0RC1
     */
    public String getMacroParameter(String name)
    {
        return getMacroParameterInput(name).getDomProperty("value");
    }

    /**
     * @param name the name of a macro parameter
     * @return the element holding the value of the parameter: an {@code input}, or the {@code select} behind a picker
     *     (e.g. an attachment picker), see {@link #getMacroParameterSuggestInput(String)}
     */
    public WebElement getMacroParameterInput(String name)
    {
        // We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
        String fieldSelector = "[class*=-editor-modal] .macro-parameter-field ";
        return getDriver().findElementWithoutWaitingWithoutScrolling(By.cssSelector(
            String.format("%1$sinput[name='%2$s'], %1$sselect[name='%2$s']", fieldSelector, name)));
    }

    /**
     * Use this for a macro parameter whose value is picked from a list of suggestions, such as a page reference.
     *
     * @param name the macro parameter name
     * @return the suggest input used to edit the value of the specified macro parameter
     * @since 17.10.14
     * @since 18.4.7
     * @since 18.9.0RC1
     */
    public SuggestInputElement getMacroParameterSuggestInput(String name)
    {
        return new SuggestInputElement(getMacroParameterInput(name));
    }

    /**
     * @param name the name of a macro parameter
     * @return the element displaying the parameter, with its name, its description and its value
     * @since 18.10.0RC1
     */
    public WebElement getMacroParameterElement(String name)
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(By.cssSelector(
            String.format("[class*=-editor-modal] .macro-parameter[data-id='%s']", name)));
    }

    /**
     * @param name the name of a macro parameter
     * @return {@code true} if the parameter is displayed, {@code false} if it's hidden, e.g. a deprecated parameter
     *     without a value
     * @since 18.10.0RC1
     */
    public boolean isMacroParameterDisplayed(String name)
    {
        return getMacroParameterElement(name).isDisplayed();
    }

    /**
     * @param name the name of a macro parameter
     * @return {@code true} if the parameter is marked as required, {@code false} otherwise
     * @since 18.10.0RC1
     */
    public boolean isMacroParameterRequired(String name)
    {
        return getMacroParameterElement(name).getDomAttribute("class").contains("mandatory");
    }

    /**
     * @param feature the name of a macro feature, which several parameters can provide
     * @return the element displaying the parameters the user can choose from to provide the feature
     * @since 18.10.0RC1
     */
    public WebElement getMacroParameterChoicesElement(String feature)
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(
            By.cssSelector(String.format(FEATURE_CHOICES, feature)));
    }

    /**
     * @param feature the name of a macro feature, which several parameters can provide
     * @return the names of the displayed parameters the user can choose from to provide the feature, empty when the
     *     feature is not displayed as a choice
     * @since 18.10.0RC1
     */
    public List<String> getMacroParameterChoices(String feature)
    {
        return getDriver()
            .findElementsWithoutWaiting(
                By.cssSelector(String.format(FEATURE_CHOICES + " .feature-parameter .macro-parameter", feature)))
            .stream().filter(WebElement::isDisplayed).map(parameter -> parameter.getDomAttribute(DATA_ID)).toList();
    }

    /**
     * @param feature the name of a macro feature, which several parameters can provide
     * @return the name of the parameter currently chosen to provide the feature, or {@code null} if none is chosen
     * @since 18.10.0RC1
     */
    public String getSelectedMacroParameterChoice(String feature)
    {
        return getDriver()
            .findElementsWithoutWaiting(By.cssSelector(String.format(FEATURE_CHOICES + " .feature-parameter", feature)))
            .stream().filter(choice -> choice.findElement(By.className("feature-radio")).isSelected()).findFirst()
            .map(choice -> choice.findElement(By.className("macro-parameter")).getDomAttribute(DATA_ID))
            .orElse(null);
    }

    /**
     * Remove the focus from the field that the modal focuses when it opens, after waiting for that field to get it.
     * This is useful before taking a screenshot of the modal, which shouldn't depend on the focus. The modal must have
     * at least one field.
     *
     * @return this modal
     * @since 18.10.0RC1
     */
    public MacroDialogEditModal clearFocus()
    {
        // The modal focuses its first field once the macro parameters are displayed, which can happen after the modal
        // is shown, e.g. when that field is a picker.
        getDriver().waitUntilCondition(driver -> {
            WebElement focusedElement = driver.switchTo().activeElement();
            return focusedElement.isDisplayed()
                && !focusedElement.findElements(By.xpath("ancestor::*[contains(@class, '-editor-modal')]")).isEmpty();
        });
        getDriver().executeScript("document.activeElement.blur()");
        // The field doesn't look focused anymore only once the transition of its border is over.
        getDriver().waitUntilCondition(driver -> (Boolean) getDriver().executeScript(
            "return document.querySelector('[class*=-editor-modal]').getAnimations({subtree: true}).length === 0"));
        return this;
    }

    /**
     * Set the value of the macro content.
     *
     * @param content the macro content
     * @return this modal
     * @since 15.10.12
     * @since 16.4.1
     * @since 16.6.0RC1
     */
    public MacroDialogEditModal setMacroContent(CharSequence... content)
    {
        WebElement contentInput = getMacroContentInput();
        contentInput.clear();
        contentInput.sendKeys(content);
        return this;
    }

    /**
     * Retrieves the value of the macro content from the macro editor modal.
     * 
     * @return the value of the macro content
     * @since 15.10.12
     * @since 16.4.1
     * @since 16.6.0RC1
     */
    public String getMacroContent()
    {
        return getMacroContentInput().getDomProperty("value");
    }

    /**
     * @return the text area used to edit the macro content
     * @since 15.10.12
     * @since 16.4.1
     * @since 16.6.0RC1
     */
    public WebElement getMacroContentInput()
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(
            // We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
            By.cssSelector("[class*=-editor-modal] .macro-parameter-field textarea[name='$content']"));
    }

    /**
     * Click on the macro submission button, then wait for the result of the submission when the editor that opened
     * the modal knows how to (see {@link #MacroDialogEditModal(Consumer)}).
     */
    public void clickSubmit()
    {
        clickSubmit(true);
    }

    /**
     * Click on the macro submission button.
     *
     * @param expectSuccess {@code true} to wait for the result of the submission when the editor that opened the modal
     *            knows how to (see {@link #MacroDialogEditModal(Consumer)}), {@code false} when the submission is
     *            expected to be refused (e.g. a required parameter has no value): the modal then stays open and there
     *            is no submission result to wait for
     * @since 18.4.8
     * @since 18.9.0RC1
     */
    public void clickSubmit(boolean expectSuccess)
    {
        Runnable click = () -> getDriver().findElement(SUBMIT_BUTTON).click();
        if (expectSuccess) {
            this.submitHandler.accept(click);
        } else {
            click.run();
        }
    }

    /**
     * Click on the Cancel button to close the macro editor modal.
     * 
     * @since 15.10.6
     * @since 16.0.0RC1
     */
    public void clickCancel()
    {
        getDriver().findElement(
            // We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
            By.cssSelector("[class*=-editor-modal] .modal-footer button[data-dismiss='modal']")).click();
    }

    /**
     * Click on the "Change Macro" button to open the macro selection modal.
     * 
     * @return the macro selection modal
     * @since 18.3.0RC1
     */
    public MacroDialogSelectModal clickChangeMacro()
    {
        getDriver().findElement(By.xpath("//*[contains(@class, '-editor-modal')]//button[. = 'Change Macro']")).click();
        return new MacroDialogSelectModal(this.submitHandler).waitUntilReady();
    }
}
