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
package org.xwiki.test.ui.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.openqa.selenium.support.ui.ExpectedConditions.attributeToBeNotEmpty;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;

/**
 * Represents the syntax picker used to change the syntax of a document.
 * 
 * @version $Id$
 * @since 12.6.3
 * @since 12.9RC1
 */
public class DocumentSyntaxPicker extends BaseElement
{
    private static final String VALUE_ATTRIBUTE = "value";

    /**
     * Represents the modal displayed when changing the syntax for requesting conversion.
     *
     * @version $Id$
     */
    public class SyntaxConversionConfirmationModal extends BaseModal
    {
        /**
         * Default constructor.
         */
        public SyntaxConversionConfirmationModal()
        {
            super(By.id("syntaxConversionConfirmation"));
        }

        /**
         * @return the message displayed in the modal.
         */
        public String getMessage()
        {
            return this.container.findElement(By.className("modal-body")).getText();
        }

        /**
         *  Confirm the conversion and wait for the picker to be ready.
         */
        public void confirmSyntaxConversion()
        {
            this.container.findElement(By.cssSelector("button.convertSyntax")).click();
            DocumentSyntaxPicker.this.waitUntilReady();
        }

        /**
         * Reject the conversion and wait for the picker to be ready.
         */
        public void rejectSyntaxConversion()
        {
            this.container.findElement(By.cssSelector("button.dontConvertSyntax")).click();
            DocumentSyntaxPicker.this.waitUntilReady();
        }

        /**
         * Acknowledge for the unsupported conversion and wait for the picker to be ready.
         */
        public void acknowledgeUnsupportedConversion()
        {
            this.container.findElement(By.cssSelector("button.acknowledge")).click();
            DocumentSyntaxPicker.this.waitUntilReady();
        }
    }

    @FindBy(id = "xwikidocsyntaxinput2")
    private WebElement selectElement;

    private Select select = new Select(this.selectElement);

    /**
     * Default constructor.
     */
    public DocumentSyntaxPicker()
    {
        waitUntilReady();
    }

    /**
     * @return the list of syntaxes.
     */
    public List<String> getAvailableSyntaxes()
    {
        return this.select.getOptions().stream()
            .map(item -> item.getAttribute(VALUE_ATTRIBUTE))
            .collect(Collectors.toList());
    }

    /**
     * @return the currently selected syntax.
     */
    public String getSelectedSyntax()
    {
        return this.select.getFirstSelectedOption().getAttribute(VALUE_ATTRIBUTE);
    }

    /**
     * Select the syntax from its id. This action will trigger the opening of a modal for informing about the conversion
     * of the content.
     *
     * @param syntaxId the id of the syntax to chose
     * @return a new instance of {@link SyntaxConversionConfirmationModal}.
     */
    public SyntaxConversionConfirmationModal selectSyntaxById(String syntaxId)
    {
        this.select.selectByValue(syntaxId);
        return new SyntaxConversionConfirmationModal();
    }

    /**
     * Wait until the select is clickable.
     */
    private void waitUntilReady()
    {
        getDriver().waitUntilCondition(elementToBeClickable(this.selectElement));
    }

    /**
     * Wait until the select is enabled.
     */
    void waitUntilEnabled()
    {
        getDriver().waitUntilCondition(not(attributeToBeNotEmpty(this.selectElement, "disabled")));
    }
}
