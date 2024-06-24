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
    public class SyntaxConversionConfirmationModal extends BaseModal
    {
        public SyntaxConversionConfirmationModal()
        {
            super(By.id("syntaxConversionConfirmation"));
        }

        public String getMessage()
        {
            return this.container.findElement(By.className("modal-body")).getText();
        }

        public void confirmSyntaxConversion()
        {
            this.container.findElement(By.cssSelector("button.convertSyntax")).click();
            DocumentSyntaxPicker.this.waitUntilReady();
        }

        public void rejectSyntaxConversion()
        {
            this.container.findElement(By.cssSelector("button.dontConvertSyntax")).click();
            DocumentSyntaxPicker.this.waitUntilReady();
        }

        public void acknowledgeUnsupportedConversion()
        {
            this.container.findElement(By.cssSelector("button.acknowledge")).click();
            DocumentSyntaxPicker.this.waitUntilReady();
        }
    }

    @FindBy(id = "xwikidocsyntaxinput2")
    private WebElement selectElement;

    private Select select = new Select(this.selectElement);

    public DocumentSyntaxPicker()
    {
        waitUntilReady();
    }

    public List<String> getAvailableSyntaxes()
    {
        return this.select.getOptions().stream().map(item -> item.getAttribute("value")).toList();
    }

    public String getSelectedSyntax()
    {
        return this.select.getFirstSelectedOption().getAttribute("value");
    }

    public SyntaxConversionConfirmationModal selectSyntaxById(String syntaxId)
    {
        this.select.selectByValue(syntaxId);
        return new SyntaxConversionConfirmationModal();
    }

    private void waitUntilReady()
    {
        getDriver().waitUntilCondition(elementToBeClickable(this.selectElement));
    }

    void waitUntilEnabled()
    {
        getDriver().waitUntilCondition(not(attributeToBeNotEmpty(this.selectElement, "disabled")));
    }
}
