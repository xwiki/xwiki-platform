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
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page Object for the macro edition modal.
 *
 * @version $Id$
 * @since 14.9
 */
public class MacroDialogEditModal extends BaseElement
{
    /**
     * Wait until the macro selection edition is loaded.
     *
     * @return the current page object
     */
    public MacroDialogEditModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(
            // We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
            By.cssSelector("[class*=-editor-modal] .macro-name"));
        return this;
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
     * Retrieves the value of a macro parameter from the macro editor modal.
     * 
     * @param name the macro parameter name
     * @return the value of the specified macro parameter
     * @since 15.10.6
     * @since 16.0.0RC1
     */
    public String getMacroParameter(String name)
    {
        return getMacroParameterInput(name).getAttribute("value");
    }

    public WebElement getMacroParameterInput(String name)
    {
        return getDriver().findElementWithoutWaitingWithoutScrolling(
            // We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
            By.cssSelector("[class*=-editor-modal] .macro-parameter-field input[name='" + name + "']"));
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
        return getMacroContentInput().getAttribute("value");
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
     * Click on the macro submission button.
     */
    public void clickSubmit()
    {
        getDriver().findElement(
            // We match *-editor-modal so the page object can be used both in Dashboard and CKEditor tests.
            By.cssSelector("[class*=-editor-modal] .modal-footer .btn-primary")).click();
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
}
